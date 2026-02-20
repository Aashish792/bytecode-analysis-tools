package com.aashish.model;

/**
 * Represents a detected reflective call pattern in bytecode.
 * 
 * WHY THIS MATTERS:
 * Static analysis cannot see dependencies loaded via reflection.
 * By detecting these patterns, we warn users that the dependency
 * graph may be incomplete.
 * 
 * Example: Class.forName("com.example.Service").getMethod("process").invoke(...)
 * - The dependency on "com.example.Service" is invisible to normal static analysis
 * - This tool detects the pattern and reports it as a warning
 * 
 * @param type           Type of reflection pattern
 * @param callerClass    Class where reflection was found
 * @param callerMethod   Method where reflection was found
 * @param pattern        Human-readable pattern description
 * @param potentialTarget Extracted target class/method name if detectable (from LDC instruction)
 */
public record ReflectiveCall(
    ReflectiveType type,
    String callerClass,
    String callerMethod,
    String pattern,
    String potentialTarget
) {
    /**
     * Human-readable format for display.
     */
    public String toReadableString() {
        String target = potentialTarget != null 
            ? " → target: \"" + potentialTarget + "\"" 
            : "";
        return String.format("%s in %s.%s%s", pattern, callerClass, callerMethod, target);
    }
}
