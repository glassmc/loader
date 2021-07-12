package com.github.glassmc.loader.util;

public class Identifier {

    private final String className;

    private final String fieldName;

    private final String methodName;
    private final String methodDesc;

    public static Identifier parse(String identifier) {
        String className;
        String fieldName = null;
        String methodName = null;
        String methodDesc = null;

        if(identifier.contains("#")) {
            String[] classElementSplit = identifier.split("#");
            className = classElementSplit[0];
            if(classElementSplit[1].contains("(")) {
                String[] methodNameDescSplit = classElementSplit[1].split("\\(");
                methodNameDescSplit[1] = "(" + methodNameDescSplit[1];

                methodName = methodNameDescSplit[0];
                methodDesc = methodNameDescSplit[1];
            } else {
                fieldName = classElementSplit[1];
            }
        } else {
            className = identifier;
        }

        return new Identifier(className, fieldName, methodName, methodDesc);
    }

    public Identifier(String className, String fieldName, String methodName, String methodDesc) {
        this.className = className;
        this.fieldName = fieldName;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
    }

    public String getClassName() {
        return className;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

}
