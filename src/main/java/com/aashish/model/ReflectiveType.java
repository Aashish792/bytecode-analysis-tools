package com.aashish.model;

/**
 * Types of reflective patterns detected during bytecode analysis.
 */
public enum ReflectiveType {
    
    CLASS_FOR_NAME("Class.forName()", "Dynamic class loading"),
    GET_METHOD("getMethod()", "Method lookup by name"),
    GET_DECLARED_METHOD("getDeclaredMethod()", "Private method lookup"),
    METHOD_INVOKE("Method.invoke()", "Reflective invocation"),
    CONSTRUCTOR_NEW_INSTANCE("Constructor.newInstance()", "Reflective instantiation"),
    CLASS_NEW_INSTANCE("Class.newInstance()", "Deprecated instantiation"),
    FIELD_GET("Field.get()", "Reflective field read"),
    FIELD_SET("Field.set()", "Reflective field write"),
    GET_CLASS("getClass()", "Runtime type check"),
    CLASS_GET_NAME("Class.getName()", "Class name lookup");
    
    private final String pattern;
    private final String description;
    
    ReflectiveType(String pattern, String description) {
        this.pattern = pattern;
        this.description = description;
    }
    
    public String getPattern() { return pattern; }
    public String getDescription() { return description; }
}
