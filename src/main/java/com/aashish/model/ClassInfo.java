package com.aashish.model;

/**
 * Metadata about a single class file for build comparison.
 * 
 * Captures structural information that can help explain
 * why two class files differ.
 */
public record ClassInfo(
    String name,
    String hash,
    int bytecodeVersion,
    int methodCount,
    int fieldCount,
    boolean hasLineNumbers,
    boolean hasLocalVariables,
    String sourceFile
) {
    /**
     * Maps bytecode version to Java version string.
     */
    public String getJavaVersion() {
        return switch (bytecodeVersion) {
            case 45 -> "Java 1.1";
            case 46 -> "Java 1.2";
            case 47 -> "Java 1.3";
            case 48 -> "Java 1.4";
            case 49 -> "Java 5";
            case 50 -> "Java 6";
            case 51 -> "Java 7";
            case 52 -> "Java 8";
            case 53 -> "Java 9";
            case 54 -> "Java 10";
            case 55 -> "Java 11";
            case 56 -> "Java 12";
            case 57 -> "Java 13";
            case 58 -> "Java 14";
            case 59 -> "Java 15";
            case 60 -> "Java 16";
            case 61 -> "Java 17";
            case 62 -> "Java 18";
            case 63 -> "Java 19";
            case 64 -> "Java 20";
            case 65 -> "Java 21";
            case 66 -> "Java 22";
            default -> "Java " + (bytecodeVersion - 44);
        };
    }
    
    /**
     * Returns true if this class has debug information compiled in.
     */
    public boolean hasDebugInfo() {
        return hasLineNumbers || hasLocalVariables;
    }
}
