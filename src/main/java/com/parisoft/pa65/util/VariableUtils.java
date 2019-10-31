package com.parisoft.pa65.util;

import com.parisoft.pa65.pojo.Function;

public class VariableUtils {

    public static String functionOf(String variable) {
        if (variable == null) {
            return null;
        }

        return variable.substring(0, variable.indexOf("::"));
    }

    public static String shortNameOf(String variable) {
        if (variable == null) {
            return null;
        }

        return variable.substring(variable.indexOf("::") + 2);
    }

    public static String absNameOf(Function function, String var) {
        if (var.contains("::")) {
            return var;
        }

        return function.getName() + "::" + var;
    }
}
