package com.parisoft.pa65.pojo;

public class Call {

    private String function;

    public Call(String function) {
        this.function = function;
    }

    public String getFunction() {
        return function;
    }

    @Override
    public String toString() {
        return "Call{" +
                "function='" + function + '\'' +
                '}';
    }
}
