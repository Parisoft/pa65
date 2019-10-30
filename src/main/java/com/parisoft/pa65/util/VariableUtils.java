package com.parisoft.pa65.util;

public class VariableUtils {

    public static String functionOf(String variable) {
        if (variable == null) {
            return null;
        }

        return variable.substring(0, variable.indexOf("::"));
    }

    public static String localNameOf(String variable) {
        if (variable == null) {
            return null;
        }

        return variable.substring(variable.indexOf("::") + 2);
    }

    public static boolean belongsToFunction(String variable, String function){
        return variable != null && variable.startsWith(function.concat("::"));
    }
}
