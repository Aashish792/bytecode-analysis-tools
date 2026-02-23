package com.aashish.model;

import org.objectweb.asm.Opcodes;

/**
 * All 5 JVM method invocation types.
 */
public enum InvokeType {
    
    INVOKEVIRTUAL("Instance method", "obj.method()"),
    INVOKESTATIC("Static method", "Class.method()"),
    INVOKESPECIAL("Constructor/super/private", "new Obj()"),
    INVOKEINTERFACE("Interface method", "list.size()"),
    INVOKEDYNAMIC("Lambda/method reference", "x -> x.toString()");
    
    private final String description;
    private final String example;
    
    InvokeType(String description, String example) {
        this.description = description;
        this.example = example;
    }
    
    public String getDescription() { return description; }
    public String getExample() { return example; }
    
    public static InvokeType fromOpcode(int opcode) {
        return switch (opcode) {
            case Opcodes.INVOKEVIRTUAL -> INVOKEVIRTUAL;
            case Opcodes.INVOKESTATIC -> INVOKESTATIC;
            case Opcodes.INVOKESPECIAL -> INVOKESPECIAL;
            case Opcodes.INVOKEINTERFACE -> INVOKEINTERFACE;
            default -> INVOKEDYNAMIC;
        };
    }
}
