package com.parisoft.pa65.pojo;

public class Call {

    private String function;
    private boolean jump;

    public Call(String function, boolean jump) {
        this.function = function;
        this.jump = jump;
    }

    public String getFunction() {
        return function;
    }

    public boolean isJump() {
        return jump;
    }

    @Override
    public String toString() {
        return "Call{" +
                "function='" + function + '\'' +
                ", jump=" + jump +
                '}';
    }
}
