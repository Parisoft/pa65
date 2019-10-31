package com.parisoft.pa65.output;

import com.parisoft.pa65.heap.Heap;
import com.parisoft.pa65.pojo.Block;
import com.parisoft.pa65.pojo.Ref;

import java.util.List;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;

public class Scopes {

    private final Heap heap;

    public Scopes(Heap heap) {
        this.heap = heap;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        heap.getBlocksByFunction()
                .forEach((func, blocks) -> {
                    List<Ref> refs = heap.getRefsByFunction().getOrDefault(func, emptyList());
                    int maxLen = Math.max(blocks.stream().map(Block::getShortVariable).mapToInt(String::length).max().orElse(1),
                                          refs.stream().map(Ref::getShortSrcVariable).mapToInt(String::length).max().orElse(1));

                    builder.append("\t.scope ").append(func).append(lineSeparator());
                    blocks.sort(comparing(Block::getOffset));
                    blocks.forEach(block -> builder.append("\t").append(rpad(block.getShortVariable(), maxLen)).append(" = ").append(Heap.nameOf(block.getSegment())).append("+").append(rpad(block.getOffset(), 2))
                            .append("\t; Segment=").append(block.getSegment()).append(" Size=").append(Integer.toHexString(block.getSize()))
                            .append(lineSeparator()));
                    refs.forEach(ref -> builder.append("\t").append(rpad(ref.getShortSrcVariable(), maxLen)).append(" = ").append(ref.getTgtVariable()).append(lineSeparator()));
                    builder.append("\t.endscope").append(lineSeparator())
                            .append(lineSeparator());
                });

        return builder.toString();
    }

    private static String rpad(Object o, int len) {
        StringBuilder builder = new StringBuilder(o.toString());

        for (int i = builder.length(); i < len; i++) {
            builder.append(" ");
        }

        return builder.toString();
    }
}
