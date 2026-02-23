package com.aashish.model;

/**
 * Represents a detected reflective call pattern in bytecode.
 */
public record ReflectiveCall(
    ReflectiveType type,
    String callerClass,
    String callerMethod,
    String pattern,
    String potentialTarget
) {
    public String toReadableString() {
        String target = potentialTarget != null 
            ? " -> \"" + potentialTarget + "\"" 
            : "";
        return pattern + " in " + callerClass + "." + callerMethod + target;
    }
}
