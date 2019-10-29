package com.parisoft.pa65;

import com.parisoft.pa65.heap.Heap;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.lang.System.lineSeparator;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class PA65 {

    private static final Pattern FUNC_OPEN_PATTERN = Pattern.compile("\\.func\\s+([^;\\\\*]+).*", CASE_INSENSITIVE);
    private static final Pattern FUNC_CLOSE_PATTERN = Pattern.compile("\\.endfunc([\\s;\\\\*]+.*)?", CASE_INSENSITIVE);
    private static final Pattern ALLOC_PATTERN = Pattern.compile("\\.palloc\\s+([^,\\s]+)\\s*,\\s*(\\w+)\\s*,\\s*([\\d$%]+).*", CASE_INSENSITIVE);
    private static final Pattern REF_PATTERN = Pattern.compile("\\.pref\\s+(\\w+)\\s*,\\s*(\\w+::\\w+).*", CASE_INSENSITIVE);
    private static final Pattern CALL_PATTERN = Pattern.compile("(jsr|jmp|jeq|jne|jmi|jpl|jcs|jcc|jvs|jvc)\\s+(\\w+)(?:[\\s;\\\\*]+.*)?", CASE_INSENSITIVE);
    private static final Pattern FREE_PATTERN = Pattern.compile("\\.pfree\\s+(\\w+)(?:\\s*,\\s*(\\w+))*(?:[\\s;\\\\*]+.*)?", CASE_INSENSITIVE);
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    private final Heap heap = new Heap();
    private final Map<String, ArrayList<Ref>> refByFunc = new LinkedHashMap<>();
    private final ArrayDeque<Function> stack = new ArrayDeque<>();
    private final Map<String, Function> functions;
    private final Collection<String> vectors;

    public PA65(Collection<String> vectors, Collection<File> input) throws IOException {
        this.functions = readFunctions(input);
        this.vectors = vectors;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        heap.getBlocksBySegment().keySet().forEach(segment -> builder.append(segment.equals("\"ZEROPAGE\"") || segment.equals(".zeropage") ? "\t.globalzp " : "\t.global ").append(heapNameOf(segment)).append(lineSeparator()));
        builder.append(lineSeparator());

        if (!heap.getBlocksBySegment().isEmpty()) {
            builder.append("\t.ifndef ").append(heapNameOf(heap.getBlocksBySegment().keySet().iterator().next())).append(lineSeparator())
                    .append(lineSeparator());
            heap.getBlocksBySegment().forEach((segment, heap) -> {
                int heapSize = heap.stream().mapToInt(block -> block.getOffset() + block.getSize()).max().orElse(0);

                if (heapSize > 0) {
                    if (segment.startsWith(".")) {
                        builder.append("\t").append(segment).append(lineSeparator());
                    } else {
                        builder.append("\t").append(".segment ").append(segment).append(lineSeparator());
                    }

                    builder.append(heapNameOf(segment)).append(":\t.res ").append("$").append(Integer.toHexString(heapSize)).append(lineSeparator())
                            .append(lineSeparator());
                }
            });
            builder.append("\t.endif").append(lineSeparator())
                    .append(lineSeparator());
        }

        heap.getBlocksByFunction()
                .forEach((func, blocks) -> {
                    ArrayList<Ref> refs = refByFunc.getOrDefault(func, new ArrayList<>());
                    builder.append("\t.scope ").append(func).append(lineSeparator());
                    blocks.forEach(block -> builder.append("\t").append(block.getVariable()).append(" =\t").append(heapNameOf(block.getSegment())).append("+").append(block.getOffset())
                            .append("\t; segment=").append(block.getSegment()).append(" size=").append(block.getSize())
                            .append(lineSeparator()));
                    refs.forEach(ref -> builder.append("\t").append(ref.getSrcVariable()).append(" =\t").append(ref.getTgtVariable()).append(lineSeparator()));
                    builder.append("\t.endscope").append(lineSeparator())
                            .append(lineSeparator());
                });

        builder.append("\t.feature leading_dot_in_identifiers").append(lineSeparator())
                .append(lineSeparator());

        builder.append("\t.macro .func name").append(lineSeparator())
                .append("name:").append(lineSeparator())
                .append("\t.scope").append(lineSeparator())
                .append("\t.define .palloc(seg,var,size) var = name::var").append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());
        builder.append("\t.macro .endfunc").append(lineSeparator())
                .append("\t.endscope").append(lineSeparator())
                .append("\t.undefine .palloc").append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());
        builder.append("\t.macro .pfree ").append(IntStream.rangeClosed(1, 32).mapToObj(v -> "v" + v).collect(joining(", "))).append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());
        builder.append("\t.define .pref(v1,v2) v1 = v2").append(lineSeparator());

        return builder.toString();
    }

    public void createHeap() throws IOException {
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
                refByFunc.computeIfAbsent(function.getName(), s -> new ArrayList<>()).add((Ref) stmt);
            } else if (stmt instanceof Free) {
                heap.free(((Free) stmt).getVariables());
            } else if (stmt instanceof Call) {
                Function called = functions.get(((Call) stmt).getFunction());

                if (called == null) { // skip if not declared as .func
                    continue;
                }

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

    private static Map<String, Function> readFunctions(Collection<File> files) throws IOException {
        Map<String, Function> functions = new LinkedHashMap<>();

        for (File file : files) {
            Function function = null;
            Matcher matcher;

            for (String line : Files.readAllLines(file.toPath())) {
                line = line.trim();

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
                    free.getVariables().add(absNameOf(function, matcher.group(1)));
                    free.getVariables().add(absNameOf(function, matcher.group(2)));
                    String[] tokens = COMMA_PATTERN.split(line);
                    for (int i = 1; i < tokens.length - 1; i++) {
                        free.getVariables().add(absNameOf(function, tokens[i].trim()));
                    }
                    function.getStmts().add(free);
                } else if (FUNC_CLOSE_PATTERN.matcher(line).matches()) {
                    function = null;
                }
            }
        }

        return functions;
    }

    private static String absNameOf(Function function, String var) {
        return function.getName() + "::" + var;
    }

    private static String heapNameOf(String segment) {
        return "heap" + Integer.toHexString(Math.abs(segment.hashCode()));
    }

    public static void main(String[] args) throws IOException {
        ArgumentParser parser = ArgumentParsers.newFor("pa65").build()
                .defaultHelp(true)
                .description("Pseudo memory allocator for ca65 projects");
        parser.addArgument("-r", "--reset").required(true).help("Name of the reset or main function");
        parser.addArgument("-n", "--nmi").required(false).help("Name of the nmi function");
        parser.addArgument("-i", "--irq").required(false).help("Name of the irq function");
        parser.addArgument("-o", "--output").required(false).help("Path to the generated file. Omit to print the file content to the standard output.");
        parser.addArgument("file").nargs("+").help("Input source files in ca65 format");

        Namespace namespace = null;

        try {
            namespace = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        List<String> vectors = new ArrayList<>(3);
        vectors.add(namespace.getString("reset"));
        vectors.add(namespace.getString("nmi"));
        vectors.add(namespace.getString("irq"));
        vectors.removeIf(Objects::isNull);

        List<File> input = namespace.getList("file").stream().map(Object::toString).map(File::new).collect(toList());
        String output = namespace.getString("output");

        PA65 pa65 = new PA65(vectors, input);
        pa65.createHeap();

        if (output != null) {
            Files.write(Paths.get(output), pa65.toString().getBytes(), CREATE, TRUNCATE_EXISTING);
        } else {
            System.out.println(pa65);
        }
    }
}
