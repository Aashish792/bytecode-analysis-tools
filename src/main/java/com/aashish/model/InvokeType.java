package com.aashish.model;

import org.objectweb.asm.Opcodes;

/**
 * Represents all 5 JVM method invocation types.
 * 
 * Understanding invoke types is crucial for bytecode analysis:
 * - INVOKEVIRTUAL: Standard instance method dispatch (most common)
 * - INVOKESTATIC: No receiver object needed
 * - INVOKESPECIAL: Bypasses virtual dispatch (constructors, super, private)
 * - INVOKEINTERFACE: Interface method with runtime lookup
 * - INVOKEDYNAMIC: Bootstrap method determines target (lambdas, method refs)
 */
public enum InvokeType {
    
    INVOKEVIRTUAL(Opcodes.INVOKEVIRTUAL, "Instance method", "obj.method()"),
    INVOKESTATIC(Opcodes.INVOKESTATIC, "Static method", "Class.method()"),
    INVOKESPECIAL(Opcodes.INVOKESPECIAL, "Constructor/super/private", "new Obj(), super.method()"),
    INVOKEINTERFACE(Opcodes.INVOKEINTERFACE, "Interface method", "list.size()"),
    INVOKEDYNAMIC(Opcodes.INVOKEDYNAMIC, "Lambda/method reference", "x -> x.toString()");
    
    private final int opcode;
    private final String description;
    private final String example;
    
    InvokeType(int opcode, String description, String example) {
        this.opcode = opcode;
        this.description = description;
        this.example = example;
    }
    
    public int getOpcode() { return opcode; }
    public String getDescription() { return description; }
    public String getExample() { return example; }
    
    /**
     * Maps JVM opcode to InvokeType.
     */
    public static InvokeType fromOpcode(int opcode) {
        return switch (opcode) {
            case Opcodes.INVOKEVIRTUAL -> INVOKEVIRTUAL;
            case Opcodes.INVOKESTATIC -> INVOKESTATIC;
            case Opcodes.INVOKESPECIAL -> INVOKESPECIAL;
            case Opcodes.INVOKEINTERFACE -> INVOKEINTERFACE;
            case Opcodes.INVOKEDYNAMIC -> INVOKEDYNAMIC;
            default -> throw new IllegalArgumentException("Unknown opcode: " + opcode);
        };
    }
}
