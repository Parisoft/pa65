package com.parisoft.pa65.output;

import com.parisoft.pa65.pojo.Call;
import com.parisoft.pa65.pojo.Function;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class StackTrace {

    private static final String PADDING = "       ";
    private static final String PADDING_WITH_COLUMN = "   |   ";
    private static final String PADDING_WITH_ENTRY = "   +-- ";
    private static final String PADDING_WITH_RECURSION = "   @-- ";

    private final Map<String, List<String>> functions;
    private final List<String> vectors;
    private final Deque<String> stack;

    public StackTrace(Collection<Function> functions, List<String> vectors) {
        this.functions = new HashMap<>(functions.size());
        this.vectors = vectors;

        for (Function function : functions) {
            this.functions.put(function.getName(),
                               function.getStmts().stream()
                                       .filter(o -> o instanceof Call)
                                       .map(o -> (Call) o)
                                       .filter(Call::isValid)
                                       .map(Call::getFunction)
                                       .collect(toList()));
        }

        stack = new ArrayDeque<>(functions.size());
    }

    public String print() {
        StringBuilder tree = new StringBuilder();

        for (String vector : vectors) {
            printTree(vector, new ArrayList<>(), tree);
            tree.append(lineSeparator());
        }

        return tree.toString();
    }

    private void printTree(String function, List<Boolean> moreFunctionsInHierarchy, StringBuilder tree) {
        StringBuilder line = new StringBuilder();

        if (moreFunctionsInHierarchy.size() > 0) {
            for (boolean hasColumn : moreFunctionsInHierarchy.subList(0, moreFunctionsInHierarchy.size() - 1)) {
                line.append(hasColumn ? PADDING_WITH_COLUMN : PADDING);
            }
        }

        if (moreFunctionsInHierarchy.size() > 0) {
            String entry = line.toString() + (stack.contains(function) ? PADDING_WITH_RECURSION : PADDING_WITH_ENTRY);
            line.append(PADDING_WITH_COLUMN)
                    .append(lineSeparator())
                    .append(entry);
        }

        tree.append(line)
                .append(function)
                .append(lineSeparator());

        if (stack.contains(function)) { // avoid recursion
            return;
        }

        final List<String> list = functions.getOrDefault(function, emptyList());

        for (int i = 0; i < list.size(); i++) {
            moreFunctionsInHierarchy.add(i < list.size() - 1);
            stack.push(function);
            printTree(list.get(i), moreFunctionsInHierarchy, tree);
            stack.poll();
            moreFunctionsInHierarchy.remove(moreFunctionsInHierarchy.size() - 1);
        }
    }
}
