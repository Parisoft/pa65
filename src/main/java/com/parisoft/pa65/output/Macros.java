package com.parisoft.pa65.output;

import java.util.stream.IntStream;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

public class Macros {

    private final String vector;

    public Macros(String vector) {
        this.vector = vector;
    }

    @Override
    @SuppressWarnings("StringBufferReplaceableByString")
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\t.feature leading_dot_in_identifiers").append(lineSeparator())
                .append(lineSeparator());

        builder.append("\t.macro .func name").append(lineSeparator())
                .append("\t.if .xmatch(name, ").append(vector).append(")").append(lineSeparator())
                .append("\t__PA65_ALLOC_HEAP__").append(lineSeparator())
                .append("\t.endif").append(lineSeparator())
                .append("\t.define .palloc(seg,var,size) var = name::var").append(lineSeparator())
                .append("name:").append(lineSeparator())
                .append("\t.scope").append(lineSeparator())
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
}
