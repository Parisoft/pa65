package com.parisoft.pa65.output;

import com.parisoft.pa65.heap.Heap;

import static java.lang.System.lineSeparator;

public class Segments {

    static final String PA_65_ALLOC_HEAP = "__PA65_ALLOC_HEAP__";

    private final Heap heap;

    public Segments(Heap heap) {
        this.heap = heap;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        heap.getBlocksBySegment().keySet().forEach(segment -> builder.append(segment.equals("\"ZEROPAGE\"") || segment.equals(".zeropage") ? "\t.globalzp " : "\t.global ").append(Heap.nameOf(segment)).append(lineSeparator()));
        builder.append(lineSeparator());

        builder.append("\t.macro " + PA_65_ALLOC_HEAP).append(lineSeparator())
                .append("\t.pushseg").append(lineSeparator())
                .append(lineSeparator());
        heap.getBlocksBySegment().forEach((segment, heap) -> {
            int heapSize = heap.stream().mapToInt(block -> block.getOffset() + block.getSize()).max().orElse(0);

            if (heapSize > 0) {
                if (segment.startsWith(".")) {
                    builder.append("\t").append(segment).append(lineSeparator());
                } else {
                    builder.append("\t").append(".segment ").append(segment).append(lineSeparator());
                }

                builder.append(Heap.nameOf(segment)).append(":\t.res ").append("$").append(Integer.toHexString(heapSize)).append(lineSeparator())
                        .append(lineSeparator());
            }
        });
        builder.append("\t.popseg").append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());

        return builder.toString();
    }
}
