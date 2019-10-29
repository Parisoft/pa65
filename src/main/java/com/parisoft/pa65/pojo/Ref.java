package com.parisoft.pa65.pojo;

public class Ref {

    private String srcVariable;
    private String tgtVariable;

    public Ref(String srcVariable, String tgtVariable) {
        this.srcVariable = srcVariable;
        this.tgtVariable = tgtVariable;
    }

    public String getSrcVariable() {
        return srcVariable;
    }

    public String getTgtVariable() {
        return tgtVariable;
    }

    @Override
    public String toString() {
        return "Ref{" +
                "srcVariable='" + srcVariable + '\'' +
                ", tgtVariable='" + tgtVariable + '\'' +
                '}';
    }
}
