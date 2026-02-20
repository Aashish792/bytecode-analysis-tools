package com.aashish.model;

import java.util.Objects;

/**
 * Represents a method call instruction found in bytecode.
 * 
 * Captures both the call target and the call site (where the call originates).
 * 
 * @param owner        Target class (internal name)
 * @param name         Target method name
 * @param descriptor   Target method descriptor
 * @param invokeType   Type of invocation (virtual, static, etc.)
 * @param callerClass  Class containing the call
 * @param callerMethod Method containing the call
 */
public record MethodCall(
    String owner,
    String name,
    String descriptor,
    InvokeType invokeType,
    String callerClass,
    String callerMethod
) {
    public MethodCall {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(name);
        Objects.requireNonNull(descriptor);
        Objects.requireNonNull(invokeType);
        Objects.requireNonNull(callerClass);
        Objects.requireNonNull(callerMethod);
    }
    
    /**
     * Converts to MethodSignature for matching against definitions.
     */
    public MethodSignature toSignature() {
        return new MethodSignature(owner, name, descriptor);
    }
    
    /**
     * Human-readable format for display.
     */
    public String toReadableString() {
        return String.format("%s %s.%s", 
            invokeType, 
            owner.replace("/", "."), 
            name);
    }
    
    /**
     * Full format including call site.
     */
    public String toDetailedString() {
        return String.format("%s %s.%s [called from %s.%s]",
            invokeType,
            owner.replace("/", "."),
            name,
            callerClass,
            callerMethod);
    }
}
