package com.parisoft.pa65.pojo;

public class Alloc {

    private String segment;
    private String variable;
    private int size;

    public Alloc(String segment, String variable, int size) {
        this.segment = segment;
        this.variable = variable;
        this.size = size;
    }

    public Alloc(Block block) {
        segment = block.getSegment();
        variable = block.getVariable();
        size = block.getSize();
    }

    public String getSegment() {
        return segment;
    }

    public String getVariable() {
        return variable;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "Alloc{" +
                "segment='" + segment + '\'' +
                ", variable='" + variable + '\'' +
                ", size=" + size +
                '}';
    }
}
