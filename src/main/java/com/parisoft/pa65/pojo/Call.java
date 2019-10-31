package com.parisoft.pa65.pojo;

public class Call {

    private String function;
    private boolean jump;
    private boolean invalid;

    public Call(String function, boolean jump) {
        this.function = function;
        this.jump = jump;
        this.invalid = false;
    }

    public String getFunction() {
        return function;
    }

    public boolean isJump() {
        return jump;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public boolean isValid() {
        return !invalid;
    }

    @Override
    public String toString() {
        return "Call{" +
                "function='" + function + '\'' +
                ", jump=" + jump +
                ", invalid=" + invalid +
                '}';
    }
}
