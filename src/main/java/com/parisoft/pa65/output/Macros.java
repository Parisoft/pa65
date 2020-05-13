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
        builder.append("\t; Jump to a function by RTS trick").append(lineSeparator())
                .append("\t; @param table\tA function table defined as .ftable").append(lineSeparator())
                .append("\t; @param X\tRegister X loaded with the function index").append(lineSeparator())
                .append("\t.macro jtx table").append(lineSeparator())
                .append("\tlda .ident(.concat(.string(table), \"_hi\")), x").append(lineSeparator())
                .append("\tpha").append(lineSeparator())
                .append("\tlda .ident(.concat(.string(table), \"_lo\")), x").append(lineSeparator())
                .append("\tpha").append(lineSeparator())
                .append("\trts").append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());
    }

    private void buildJty(StringBuilder builder) {
        builder.append("\t; Jump to a function by RTS trick").append(lineSeparator())
                .append("\t; @param table\tA function table defined as .ftable").append(lineSeparator())
                .append("\t; @param Y\tRegister Y loaded with the function index").append(lineSeparator())
                .append("\t.macro jty table").append(lineSeparator())
                .append("\tlda .ident(.concat(.string(table), \"_hi\")), y").append(lineSeparator())
                .append("\tpha").append(lineSeparator())
                .append("\tlda .ident(.concat(.string(table), \"_lo\")), y").append(lineSeparator())
                .append("\tpha").append(lineSeparator())
                .append("\trts").append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());
    }

    private void buildFuncTable(StringBuilder builder) {
        builder.append("\t; Table of functions to be called with jtx or jty").append(lineSeparator())
                .append("\t; @param name\tThe name of the table").append(lineSeparator())
                .append("\t; @param funcs\tArray of functions").append(lineSeparator())
                .append("\t.macro .ftable name, funcs").append(lineSeparator())
                .append("name:").append(lineSeparator())
                .append(".ident(.concat(.string(name), \"_lo\")):").append(lineSeparator())
                .append("\t.lobytes funcs").append(lineSeparator())
                .append(".ident(.concat(.string(name), \"_hi\")):").append(lineSeparator())
                .append("\t.hibytes funcs").append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());
    }

    private void buildPref(StringBuilder builder) {
        builder.append("\t; Create a reference of a variable defined on another function").append(lineSeparator())
                .append("\t; @param v1\tThe name of the referer variable").append(lineSeparator())
                .append("\t; @param v2\tThe name of the referee variable").append(lineSeparator())
                .append("\t.define .pref(v1,v2) v1 = v2").append(lineSeparator())
                .append(lineSeparator());
    }

    private void buildPfree(StringBuilder builder) {
        builder.append("\t; Free a memory space allocated by some variables").append(lineSeparator())
                .append("\t.macro .pfree ").append(IntStream.rangeClosed(1, 32).mapToObj(v -> "v" + v).collect(joining(", "))).append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());
    }

    private void buildFunc(StringBuilder builder) {
        builder.append("\t; Declare a function").append(lineSeparator())
                .append("\t; @param name\tThe name of the function").append(lineSeparator())
                .append("\t.macro .func name").append(lineSeparator())
                .append("\t.if .xmatch(name, ").append(vector).append(")").append(lineSeparator())
                .append("\t").append(Segments.PA_65_ALLOC_HEAP).append(lineSeparator())
                .append("\t.endif").append(lineSeparator())
                .append("\t; Allocate some bytes of a variable into a segment.").append(lineSeparator())
                .append("\t; @param seg\tThe name of a segment to allocate into. The directives .zeropage and .bss can also be used").append(lineSeparator())
                .append("\t; @param var\tThe name of the variable").append(lineSeparator())
                .append("\t; @param size\tThe number of bytes to allocate").append(lineSeparator())
                .append("\t.define .palloc(seg,var,size) var = name::var").append(lineSeparator())
                .append("name:").append(lineSeparator())
                .append("\t.scope").append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());

        builder.append("\t; Ends a function declaration").append(lineSeparator())
                .append("\t.macro .endfunc").append(lineSeparator())
                .append("\t.endscope").append(lineSeparator())
                .append("\t.undefine .palloc").append(lineSeparator())
                .append("\t.endmac").append(lineSeparator())
                .append(lineSeparator());
    }
}
