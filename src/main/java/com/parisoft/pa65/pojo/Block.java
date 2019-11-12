package com.parisoft.pa65.pojo;

import static com.parisoft.pa65.util.VariableUtils.functionOf;
import static com.parisoft.pa65.util.VariableUtils.shortNameOf;

public class Block {

    private int offset;
    private int size;
    private boolean finished;
    private Alloc alloc;

    public Block(Alloc alloc) {
        this.alloc = alloc;
        size = alloc.getSize();
        offset = 0;
        finished = false;
    }

    public Block() {
        super();
    }

    public String getVariable() {
        return alloc == null ? null : alloc.getVariable();
    }

    public String getSegment() {
        return alloc == null ? null : alloc.getSegment();
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

    public Alloc getAlloc() {
        return alloc;
    }

    public boolean isFree() {
        return alloc == null;
    }

    public boolean isNotFree() {
        return !isFree();
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isNotFinished() {
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
        if (this.size < 0) { // overflow
            this.size = Integer.MAX_VALUE;
        }
    }

    public int getOffsetPlusSize() {
        try {
            return Math.addExact(offset,size);
        } catch (ArithmeticException e) {
            return Integer.MAX_VALUE;
        }
    }

    public boolean overlaps(Block that) {
        return (this.offset >= that.offset && this.offset < that.offset + that.size) || (that.offset >= this.offset && that.offset < this.offset + this.size);
    }

    public String getFunction() {
        return alloc == null ? null : functionOf(alloc.getVariable());
    }

    public String getShortVariable() {
        return shortNameOf(alloc.getVariable());
    }

    @Override
    public String toString() {
        return "Block{" +
                "variable='" + getVariable() + '\'' +
                ", segment='" + getSegment() + '\'' +
                ", offset=" + offset +
                ", size=" + size +
                ", finished=" + finished +
                '}';
    }
}
