package com.parisoft.pa65.pojo;

import static com.parisoft.pa65.util.VariableUtils.functionOf;
import static com.parisoft.pa65.util.VariableUtils.localNameOf;

public class Block {

    private String variable;
    private String segment;
    private int offset;
    private int size;
    private boolean finished;

    public Block(Alloc alloc) {
        variable = alloc.getVariable();
        segment = alloc.getSegment();
        size = alloc.getSize();
        offset = 0;
        finished = false;
    }

    public Block() {
        super();
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isFree() {
        return variable == null;
    }

    public boolean isNotFree(){
        return !isFree();
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isNotFinished(){
        return !isFinished();
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void subSize(Block that) {
        this.size -= that.size;
    }

    public void addSize(Block that) {
        this.size += that.size;
    }

    public String getFunction() {
        return functionOf(variable);
    }

    public String getLocalVariable() {
        return localNameOf(variable);
    }

    @Override
    public String toString() {
        return "Block{" +
                "variable='" + variable + '\'' +
                ", segment='" + segment + '\'' +
                ", offset=" + offset +
                ", size=" + size +
                ", finished=" + finished +
                '}';
    }
}
