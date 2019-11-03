package com.parisoft.pa65;

import com.parisoft.pa65.heap.Heap;
import com.parisoft.pa65.output.Macros;
import com.parisoft.pa65.output.Scopes;
import com.parisoft.pa65.output.Segments;
import com.parisoft.pa65.output.StackTrace;
import com.parisoft.pa65.pojo.Alloc;
import com.parisoft.pa65.pojo.Call;
import com.parisoft.pa65.pojo.Free;
import com.parisoft.pa65.pojo.Function;
import com.parisoft.pa65.pojo.Ref;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.parisoft.pa65.util.VariableUtils.absNameOf;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.Collections.emptyList;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.toList;

public class PA65 {

    private static final Pattern FUNC_OPEN_PATTERN = Pattern.compile("\\.func\\s+([^;\\\\*]+).*", CASE_INSENSITIVE);
    private static final Pattern FUNC_CLOSE_PATTERN = Pattern.compile("\\.endfunc([\\s;\\\\*]+.*)?", CASE_INSENSITIVE);
    private static final Pattern ALLOC_PATTERN = Pattern.compile("(?:@?\\w*:?\\s*)?\\.palloc\\s+([^,\\s]+)\\s*,\\s*(\\w+)\\s*,\\s*([\\d$%]+).*", CASE_INSENSITIVE);
    private static final Pattern REF_PATTERN = Pattern.compile("(?:@?\\w*:?\\s*)?\\.pref\\s+(\\w+)\\s*,\\s*(\\w+::\\w+).*", CASE_INSENSITIVE);
    private static final Pattern CALL_PATTERN = Pattern.compile("(?:@?\\w*:?\\s*)?(jsr|jmp|jeq|jne|jmi|jpl|jcs|jcc|jvs|jvc|jtx|jty)\\s+(\\w+)(?:[\\s;\\\\*]+.*)?", CASE_INSENSITIVE);
    private static final Pattern TABLE_PATTERN = Pattern.compile("(?:@?\\w*:?\\s*)?\\.func_table\\s+(\\w+)\\s*,\\s*\\{?\\s*(\\s*,?\\s*\\w+)*[}\\\\]?(?:[\\s;\\\\*]+.*)?", CASE_INSENSITIVE);
    private static final Pattern FREE_PATTERN = Pattern.compile("(?:@?\\w*:?\\s*)?\\.pfree(?:\\s+(\\w+)(?:\\s*,\\s*(\\w+))*)?(?:[\\s;\\\\*]+.*)?", CASE_INSENSITIVE);
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    private final Heap heap = new Heap();
    private final Map<String, Function> functions = new LinkedHashMap<>();
    private final List<String> vectors;

    public PA65(Collection<File> input) throws IOException {
        parseFunctions(input);

        List<String> referenced = functions.values()
                .stream()
                .map(Function::getStmts)
                .flatMap(List::stream)
                .filter(o -> o instanceof Call)
                .map(o -> ((Call) o).getFunction())
                .collect(toList());
        this.vectors = functions.keySet()
                .stream()
                .filter(func -> !referenced.contains(func))
                .collect(toList());
    }

    public String output() {
        return new Segments(heap).toString()
                + new Scopes(heap)
                + new Macros(vectors.get(0));
    }

    public String getStackTrace() {
        return new StackTrace(functions.values(), vectors).print();
    }

    public void createHeap() {
        for (String vector : vectors) {
            Function function = functions.values()
                    .stream()
                    .filter(func -> func.getName().equals(vector))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Vector function not found: " + vector));

            processFunction(function);
        }
    }

    private void processFunction(Function function) {
        for (Object stmt : function.getStmts()) {
            if (stmt instanceof Alloc) {
                heap.allocByFirstFit((Alloc) stmt);
            } else if (stmt instanceof Ref) {
                heap.addReference(function, (Ref) stmt);
                allocOf((Ref) stmt).ifPresent(heap::allocByFirstFit);
            } else if (stmt instanceof Free) {
                if (((Free) stmt).getVariables().isEmpty()) {
                    heap.free();
                } else {
                    heap.free(((Free) stmt).getVariables());
                    List<String> referencedVars = heap.getRefsByFunction().getOrDefault(function.getName(), emptyList())
                            .stream()
                            .filter(ref -> ((Free) stmt).getVariables().contains(ref.getSourceVar()))
                            .map(this::allocOf)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .map(Alloc::getVariable)
                            .collect(toList());
                    heap.free(referencedVars);
                }
            } else if (stmt instanceof Call) {
                Function called = functions.get(((Call) stmt).getFunction());

                if (called == null) { // skip if not declared as .func
                    ((Call) stmt).setInvalid(true);
                    continue;
                }

                ArrayDeque<Function> stack = heap.getStack();

                if (stack.contains(called)) { // skip on recursion
                    continue;
                }

                if (((Call) stmt).isJump()) {
                    heap.save(function);
                } else {
                    stack.push(function);
                }

                processFunction(called);

                if (((Call) stmt).isJump()) {
                    heap.load(function);
                } else {
                    stack.poll();
                }
            }
        }

        heap.free(function);
    }

    private Optional<Alloc> allocOf(Ref ref) {
        AtomicReference<String> target = new AtomicReference<>(ref.getTargetVar());
        Object found;

        while ((found = functions.values().stream()
                .map(Function::getStmts)
                .flatMap(List::stream).parallel()
                .filter(o -> (o instanceof Alloc && ((Alloc) o).getVariable().equals(target.get()))
                        || (o instanceof Ref && ((Ref) o).getSourceVar().equals(target.get())))
                .findFirst()
                .orElse(null)) != null && !(found instanceof Alloc)) {
            target.set(((Ref) found).getTargetVar());
        }

        return Optional.ofNullable((Alloc) found);
    }

    private void parseFunctions(Collection<File> files) throws IOException {
        Map<String, Set<String>> tables = new LinkedHashMap<>();

        for (File file : files) {
            Function function = null;
            Matcher matcher;

            List<String> lines = Files.readAllLines(file.toPath());

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();

                if (function == null) {
                    if ((matcher = FUNC_OPEN_PATTERN.matcher(line)).matches()) {
                        function = new Function(matcher.group(1));
                        functions.put(function.getName(), function);
                    }

                    continue;
                }

                if ((matcher = ALLOC_PATTERN.matcher(line)).matches()) {
                    function.getStmts().add(new Alloc(matcher.group(1), absNameOf(function, matcher.group(2)), Integer.valueOf(matcher.group(3))));
                } else if ((matcher = REF_PATTERN.matcher(line)).matches()) {
                    function.getStmts().add(new Ref(absNameOf(function, matcher.group(1)), matcher.group(2)));
                } else if ((matcher = CALL_PATTERN.matcher(line)).matches()) {
                    function.getStmts().add(new Call(matcher.group(2), !matcher.group(1).equalsIgnoreCase("jsr")));
                } else if ((matcher = FREE_PATTERN.matcher(line)).matches()) {
                    Free free = new Free();
                    String var1 = matcher.group(1);
                    String var2 = matcher.group(2);

                    if (var1 != null) {
                        free.getVariables().add(absNameOf(function, var1));
                    }

                    if (var2 != null) {
                        free.getVariables().add(absNameOf(function, var2));
                    }

                    String[] tokens = COMMA_PATTERN.split(line);

                    for (int t = 1; t < tokens.length - 1; t++) {
                        free.getVariables().add(absNameOf(function, tokens[t].trim()));
                    }

                    function.getStmts().add(free);
                } else if ((matcher = TABLE_PATTERN.matcher(line)).matches()) {
                    String tableName = matcher.group(1);
                    Set<String> entries = tables.computeIfAbsent(tableName, s -> new HashSet<>());

                    String subline = null;
                    int j = i;

                    do {
                        if (subline == null) {
                            subline = line.substring(line.indexOf(tableName) + tableName.length());
                        } else {
                            subline = lines.get(++j).trim();
                        }

                        for (String token : COMMA_PATTERN.split(subline)) {
                            String entry = token.trim().replaceAll("\\s*-\\s*1", "").replaceAll("\\W", "");

                            if (entry.length() > 0) {
                                entries.add(entry);
                            }
                        }
                    } while (!subline.contains("}"));

                    i = j;
                } else if (FUNC_CLOSE_PATTERN.matcher(line).matches()) {
                    function = null;
                }
            }
        }

        // replace call to jump tables for calls to it's functions
        for (Function function : functions.values()) {
            for (int i = 0; i < function.getStmts().size(); i++) {
                Object stmt = function.getStmts().get(i);

                if (stmt instanceof Call && tables.containsKey(((Call) stmt).getFunction())) {
                    List<Call> calls = tables.get(((Call) stmt).getFunction()).stream()
                            .filter(functions::containsKey)
                            .map(target -> new Call(target, true))
                            .collect(toList());
                    function.getStmts().remove(stmt);
                    function.getStmts().addAll(i, calls);
                }
            }
        }
    }

    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("pa65").build()
                .defaultHelp(true)
                .description("Pseudo memory allocator for ca65 projects");
        parser.addArgument("-d", "--debug").nargs("?").setDefault(false).setConst(true).choices(true, false).help("Set debug mode.");
        parser.addArgument("-t", "--tree").nargs("?").setDefault(false).setConst(true).choices(true, false).help("Print the execution tree.");
        parser.addArgument("-o", "--output").required(false).help("Path to the generated file. Omit to print the file content to the standard output.");
        parser.addArgument("file").nargs("+").help("Input source files in ca65 format");

        Namespace namespace = null;

        try {
            namespace = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        List<File> input = namespace.getList("file").stream().map(Object::toString).map(File::new).collect(toList());
        String output = namespace.getString("output");
        boolean debug = namespace.getBoolean("debug");
        boolean tree = namespace.getBoolean("tree");

        try {
            PA65 pa65 = new PA65(input);
            pa65.createHeap();

            if (tree) {
                System.out.println(pa65.getStackTrace());
            }

            if (output != null) {
                Files.write(Paths.get(output), pa65.output().getBytes(), CREATE, TRUNCATE_EXISTING);
            } else {
                System.out.println(pa65.output());
            }
        } catch (Exception e) {
            if (debug) {
                e.printStackTrace();
            } else {
                System.err.println(e.getMessage());
            }

            System.exit(1);
        }
    }
}
