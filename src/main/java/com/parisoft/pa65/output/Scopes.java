package com.parisoft.pa65.output;

import com.parisoft.pa65.heap.Heap;
import com.parisoft.pa65.pojo.Block;
import com.parisoft.pa65.pojo.Ref;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.parisoft.pa65.util.VariableUtils.functionOf;
import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class Scopes {

    private final Heap heap;

    public Scopes(Heap heap) {
        this.heap = heap;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        Map<String, Scope> scopeByName = heap.getBlocksByFunction().entrySet().stream()
                .map(entry -> {
                    Scope scope = new Scope();
                    scope.name = entry.getKey();
                    scope.blocks = entry.getValue();

                    return scope;
                })
                .collect(toMap(scope -> scope.name, Function.identity()));

        heap.getRefsByFunction()
                .forEach((func, refs) -> scopeByName.compute(func, (name, scope) -> {
                    if (scope == null) {
                        scope = new Scope();
                        scope.name = name;
                    }

                    scope.refs = refs;

                    return scope;
                }));

        List<Scope> scopes = scopeByName.values()
                .stream()
                .filter(scope -> scope.refs.isEmpty())
                .collect(toList());

        scopeByName.values()
                .stream()
                .filter(scope -> scope.refs.size() > 0)
                .sorted(comparingInt((Scope scope) -> scope.refs.size()).reversed())
                .forEach(scope -> addDependencies(scope.name, scopeByName, scopes));

        scopes.forEach(scope -> {
            int maxLen = Math.max(scope.blocks.stream().map(Block::getShortVariable).mapToInt(String::length).max().orElse(1),
                                  scope.refs.stream().map(Ref::getShortSourceVar).mapToInt(String::length).max().orElse(1));
            builder.append("\t.scope ").append(scope.name).append(lineSeparator());
            scope.blocks.sort(comparing(Block::getOffset));
            scope.blocks.forEach(block -> builder.append("\t").append(rpad(block.getShortVariable(), maxLen)).append(" = ").append(Heap.nameOf(block.getSegment())).append("+").append(rpad(block.getOffset(), 2))
                    .append("\t; Segment=").append(block.getSegment()).append(" Size=").append(Integer.toHexString(block.getSize()))
                    .append(lineSeparator()));
            scope.refs.forEach(ref -> builder.append("\t").append(rpad(ref.getShortSourceVar(), maxLen)).append(" = ").append(ref.getTargetVar()).append(lineSeparator()));
            builder.append("\t.endscope").append(lineSeparator())
                    .append(lineSeparator());
        });

        return builder.toString();
    }

    private static void addDependencies(String scopeName, Map<String, Scope> scopeByName, List<Scope> scopes) {
        Scope scope = scopeByName.get(scopeName);
        scope.refs.forEach(ref -> addDependencies(functionOf(ref.getTargetVar()), scopeByName, scopes));

        if (scopes.contains(scope)) {
            return;
        }

        scopes.add(scope);
    }

    private static String rpad(Object o, int len) {
        StringBuilder builder = new StringBuilder(o.toString());

        for (int i = builder.length(); i < len; i++) {
            builder.append(" ");
        }

        return builder.toString();
    }

    class Scope implements Comparable<Scope> {

        String name;
        List<Block> blocks = emptyList();
        List<Ref> refs = emptyList();

        @Override
        public int compareTo(Scope that) {
            if (this.refs.isEmpty() && that.refs.isEmpty()) {
                return this.name.compareTo(that.name);
            }

            if (this.refs.isEmpty()) {
                return -1;
            }

            if (that.refs.isEmpty()) {
                return 1;
            }

            if (this.refs.stream().anyMatch(ref -> functionOf(ref.getTargetVar()).equals(that.name))) {
                return 1;
            }

            if (that.refs.stream().anyMatch(ref -> functionOf(ref.getTargetVar()).equals(this.name))) {
                return -1;
            }

            return 0;
        }
    }
}
