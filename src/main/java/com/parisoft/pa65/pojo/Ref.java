package com.parisoft.pa65.pojo;

import com.parisoft.pa65.util.VariableUtils;

import static com.parisoft.pa65.util.VariableUtils.shortNameOf;

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

    public String getShortSrcVariable(){
        return shortNameOf(srcVariable);
    }

    @Override
    public String toString() {
        return "Ref{" +
                "srcVariable='" + srcVariable + '\'' +
                ", tgtVariable='" + tgtVariable + '\'' +
                '}';
    }
}
