package com.aashish.model;

import java.util.Objects;

/**
 * Immutable representation of a method signature.
 * Used as key for matching calls against definitions.
 * 
 * The combination of (owner, name, descriptor) uniquely identifies a method
 * in Java bytecode, as Java supports method overloading but not return-type overloading.
 * 
 * @param owner      Internal class name (e.g., "java/util/List")
 * @param name       Method name (e.g., "add")  
 * @param descriptor Method descriptor including params and return type (e.g., "(Ljava/lang/Object;)Z")
 */
public record MethodSignature(String owner, String name, String descriptor) {
    
    public MethodSignature {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(descriptor, "descriptor cannot be null");
    }
    
    /**
     * Returns human-readable format: "java.util.List.add"
     */
    public String toReadableString() {
        return owner.replace("/", ".") + "." + name;
    }
    
    /**
     * Returns short format: "List.add"
     */
    public String toShortString() {
        String shortOwner = owner.contains("/") 
            ? owner.substring(owner.lastIndexOf("/") + 1) 
            : owner;
        return shortOwner + "." + name;
    }
    
    /**
     * Returns full signature with descriptor for debugging.
     */
    public String toFullString() {
        return owner.replace("/", ".") + "." + name + descriptor;
    }
}
