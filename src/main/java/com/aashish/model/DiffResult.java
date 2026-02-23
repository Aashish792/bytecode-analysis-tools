package com.aashish.model;

import java.util.*;

/**
 * Result of comparing two JAR files for build variability.
 */
public final class DiffResult {
    
    private final String jar1Path;
    private final String jar2Path;
    private final JarMetadata jar1Metadata;
    private final JarMetadata jar2Metadata;
    private final int jar1ClassCount;
    private final int jar2ClassCount;
    private final List<ClassDiff> differences;
    private final long analysisTimeMs;
    private final List<String> warnings;
    
    public DiffResult(String jar1Path, String jar2Path, 
                      JarMetadata jar1Metadata, JarMetadata jar2Metadata,
                      int jar1ClassCount, int jar2ClassCount,
                      List<ClassDiff> differences, long analysisTimeMs,
                      List<String> warnings) {
        this.jar1Path = jar1Path;
        this.jar2Path = jar2Path;
        this.jar1Metadata = jar1Metadata;
        this.jar2Metadata = jar2Metadata;
        this.jar1ClassCount = jar1ClassCount;
        this.jar2ClassCount = jar2ClassCount;
        this.differences = Collections.unmodifiableList(new ArrayList<>(differences));
        this.analysisTimeMs = analysisTimeMs;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }
    
    public String getJar1Path() { return jar1Path; }
    public String getJar2Path() { return jar2Path; }
    public JarMetadata getJar1Metadata() { return jar1Metadata; }
    public JarMetadata getJar2Metadata() { return jar2Metadata; }
    public int getJar1ClassCount() { return jar1ClassCount; }
    public int getJar2ClassCount() { return jar2ClassCount; }
    public List<ClassDiff> getDifferences() { return differences; }
    public long getAnalysisTimeMs() { return analysisTimeMs; }
    public List<String> getWarnings() { return warnings; }
    
    public boolean hasJdkMismatch() {
        String jdk1 = jar1Metadata.buildJdk();
        String jdk2 = jar2Metadata.buildJdk();
        return jdk1 != null && jdk2 != null && !jdk1.equals(jdk2);
    }
    
    public boolean hasBytecodeVersionMismatch() {
        return differences.stream()
            .anyMatch(d -> d.reason() != null && d.reason().contains("Bytecode version"));
    }
    
    public long getIdenticalCount() {
        long nonIdentical = differences.stream()
            .filter(d -> d.type() != DiffType.ONLY_IN_JAR2)
            .count();
        return jar1ClassCount - nonIdentical;
    }
    
    public long getDifferentCount() {
        return differences.stream().filter(d -> d.type() == DiffType.DIFFERENT).count();
    }
    
    public long getOnlyInJar1Count() {
        return differences.stream().filter(d -> d.type() == DiffType.ONLY_IN_JAR1).count();
    }
    
    public long getOnlyInJar2Count() {
        return differences.stream().filter(d -> d.type() == DiffType.ONLY_IN_JAR2).count();
    }
    
    public boolean areIdentical() {
        return differences.isEmpty();
    }
    
    public String getRecommendation() {
        if (hasJdkMismatch()) {
            return "JDK version mismatch detected. Use the -release compiler flag " +
                   "(not just -target) to ensure bytecode compatibility.";
        }
        if (hasBytecodeVersionMismatch()) {
            return "Bytecode versions differ. Ensure both builds use the same JDK.";
        }
        if (!areIdentical()) {
            return "Builds differ. Common causes: timestamps, debug info, non-deterministic ordering.";
        }
        return "Builds are identical.";
    }
    
    public record JarMetadata(
        String buildJdk,
        String createdBy,
        String builtBy,
        String timestamp,
        String mainClass,
        String manifestVersion
    ) {
        public static JarMetadata empty() {
            return new JarMetadata(null, null, null, null, null, null);
        }
    }
    
    public record ClassDiff(
        String className,
        DiffType type,
        String reason,
        String details
    ) {}
    
    public enum DiffType {
        IDENTICAL, DIFFERENT, ONLY_IN_JAR1, ONLY_IN_JAR2
    }
}
