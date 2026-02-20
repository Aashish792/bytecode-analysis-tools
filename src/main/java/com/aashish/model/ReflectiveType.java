package com.aashish.model;

/**
 * Types of reflective patterns detected during bytecode analysis.
 * 
 * Each type represents a different way that code can dynamically
 * load classes or invoke methods at runtime, bypassing static analysis.
 */
public enum ReflectiveType {
    
    CLASS_FOR_NAME(
        "Class.forName()",
        "Dynamically loads a class by name at runtime",
        "Hidden class dependency - the loaded class may have its own dependencies"
    ),
    
    GET_METHOD(
        "getMethod() / getDeclaredMethod()",
        "Looks up a method by name and parameter types",
        "Method being looked up may be in another JAR"
    ),
    
    METHOD_INVOKE(
        "Method.invoke()",
        "Invokes a method reflectively",
        "The actual method call is invisible to static analysis"
    ),
    
    CONSTRUCTOR_NEW_INSTANCE(
        "Constructor.newInstance()",
        "Creates an object instance reflectively",
        "Object creation bypasses normal 'new' keyword analysis"
    ),
    
    CLASS_NEW_INSTANCE(
        "Class.newInstance() [deprecated]",
        "Deprecated way to create instances reflectively",
        "Should be replaced with Constructor.newInstance()"
    ),
    
    FIELD_ACCESS(
        "Field.get() / Field.set()",
        "Accesses or modifies a field reflectively",
        "Field access bypasses normal field reference analysis"
    );
    
    private final String pattern;
    private final String description;
    private final String implication;
    
    ReflectiveType(String pattern, String description, String implication) {
        this.pattern = pattern;
        this.description = description;
        this.implication = implication;
    }
    
    public String getPattern() { return pattern; }
    public String getDescription() { return description; }
    public String getImplication() { return implication; }
}
