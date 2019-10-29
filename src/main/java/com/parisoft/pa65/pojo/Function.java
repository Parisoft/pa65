package com.parisoft.pa65.pojo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Queue;

public class Function {

    private String name;

    private Deque<Object> stmts = new ArrayDeque<>();

    public Function(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Deque<Object> getStmts() {
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
