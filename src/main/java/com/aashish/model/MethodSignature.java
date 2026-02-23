package com.aashish.model;

import java.util.Objects;

/**
 * Immutable representation of a method signature.
 * Used as key for matching calls against definitions.
 */
public record MethodSignature(String owner, String methodName, String descriptor) {
    
    public MethodSignature {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(methodName, "methodName cannot be null");
        Objects.requireNonNull(descriptor, "descriptor cannot be null");
    }
    
    public String toReadableString() {
        return owner.replace("/", ".") + "." + methodName;
    }
    
    public String toShortString() {
        String shortOwner = owner.contains("/") 
            ? owner.substring(owner.lastIndexOf("/") + 1) 
            : owner;
        return shortOwner + "." + methodName;
    }
}
