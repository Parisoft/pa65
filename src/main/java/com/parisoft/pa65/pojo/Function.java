package com.parisoft.pa65.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Function {

    private String name;

    private List<Object> stmts = new ArrayList<>();

    public Function(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Object> getStmts() {
        return stmts;
    }

    @Override
    public String toString() {
        return "Function{" +
                "name='" + name + '\'' +
                ", stmts=" + stmts +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Function)) {
            return false;
        }

        return Objects.equals(this.name, ((Function) obj).name);
    }
}
