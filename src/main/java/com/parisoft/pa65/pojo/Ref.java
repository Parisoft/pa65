package com.parisoft.pa65.pojo;

import static com.parisoft.pa65.util.VariableUtils.shortNameOf;

public class Ref {

    private String sourceVar;
    private String targetVar;

    public Ref(String sourceVar, String targetVar) {
        this.sourceVar = sourceVar;
        this.targetVar = targetVar;
    }

    public String getSourceVar() {
        return sourceVar;
    }

    public String getTargetVar() {
        return targetVar;
    }

    public String getShortSourceVar(){
        return shortNameOf(sourceVar);
    }

    @Override
    public String toString() {
        return "Ref{" +
                "sourceVar='" + sourceVar + '\'' +
                ", targetVar='" + targetVar + '\'' +
                '}';
    }
}
