package com.parisoft.pa65.pojo;

import java.util.ArrayList;
import java.util.List;

public class Free {

    List<String> variables = new ArrayList<>();

    public List<String> getVariables() {
        return variables;
    }

    @Override
    public String toString() {
        return "Free{" +
                "variables=" + variables +
                '}';
    }
}
