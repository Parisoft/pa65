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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\t.feature leading_dot_in_identifiers").append(lineSeparator())
                .append(lineSeparator());

        buildFunc(builder);

        buildPfree(builder);

        buildPref(builder);

        buildFuncTable(builder);

        buildJtx(builder);

        buildJty(builder);

        return builder.toString();
    }

    private void buildJtx(StringBuilder builder) {
        builder.append("\t.macro jtx table").append(lineSeparator())
                .append("\tlda .ident(.concat(.string(table), \"_hi\")), x").append(lineSeparator())
                .append("\tpha").append(lineSeparator())
                .append("\tlda .ident(.concat(.string(table), \"_lo\")), x").append(lineSeparator())
                .append("\tpha").append(lineSeparator())
                .append("\trts").append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());
    }

    private void buildJty(StringBuilder builder) {
        builder.append("\t.macro jty table").append(lineSeparator())
                .append("\tlda .ident(.concat(.string(table), \"_hi\")), y").append(lineSeparator())
                .append("\tpha").append(lineSeparator())
                .append("\tlda .ident(.concat(.string(table), \"_lo\")), y").append(lineSeparator())
                .append("\tpha").append(lineSeparator())
                .append("\trts").append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());
    }

    private void buildFuncTable(StringBuilder builder) {
        builder.append("\t.macro .func_table name, funcs").append(lineSeparator())
                .append("name:").append(lineSeparator())
                .append(".ident(.concat(.string(name), \"_lo\")):").append(lineSeparator())
                .append("\t.lobytes funcs").append(lineSeparator())
                .append(".ident(.concat(.string(name), \"_hi\")):").append(lineSeparator())
                .append("\t.hibytes funcs").append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());
    }

    private void buildPref(StringBuilder builder) {
        builder.append("\t.define .pref(v1,v2) v1 = v2").append(lineSeparator())
                .append(lineSeparator());
    }

    private void buildPfree(StringBuilder builder) {
        builder.append("\t.macro .pfree ").append(IntStream.rangeClosed(1, 32).mapToObj(v -> "v" + v).collect(joining(", "))).append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());
    }

    private void buildFunc(StringBuilder builder) {
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
    }
}
