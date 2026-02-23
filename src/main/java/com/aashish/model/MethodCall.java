package com.aashish.model;

import java.util.Objects;

/**
 * Represents a method call instruction found in bytecode.
 */
public record MethodCall(
    String owner,
    String methodName,
    String descriptor,
    InvokeType invokeType,
    String callerClass,
    String callerMethod
) {
    public MethodCall {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(methodName);
        Objects.requireNonNull(descriptor);
        Objects.requireNonNull(invokeType);
        Objects.requireNonNull(callerClass);
        Objects.requireNonNull(callerMethod);
    }
    
    public MethodSignature toSignature() {
        return new MethodSignature(owner, methodName, descriptor);
    }
    
    public String toReadableString() {
        return invokeType + " " + owner.replace("/", ".") + "." + methodName;
    }
}
