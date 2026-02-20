package com.aashish.analyzer;

import com.aashish.model.*;
import com.aashish.model.DiffResult.*;
import org.objectweb.asm.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.*;

/**
 * Analyzes and EXPLAINS differences between two JAR files built from the same source.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * PURPOSE
 * ═══════════════════════════════════════════════════════════════════════════
 * When the same source code is built in different environments, the resulting
 * binaries may differ. This tool identifies and explains those differences.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * CONNECTION TO RESEARCH
 * ═══════════════════════════════════════════════════════════════════════════
 * The ICST 2025 paper "Towards Cross-Build Differential Testing" found that:
 * 
 * 1. JDK version mismatches cause real runtime failures
 *    - ByteBuffer.flip() returns Buffer in Java 8, ByteBuffer in Java 11
 *    - This causes NoSuchMethodError at runtime
 * 
 * 2. Using -target flag alone is NOT sufficient
 *    - Must use -release flag to constrain standard library API
 *    - See JEP 247: https://openjdk.org/jeps/247
 * 
 * 3. 3,541 binary pairs analyzed, found 48+ test failures due to JDK mismatch
 * 
 * This tool helps identify such issues BEFORE they cause production failures.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * HOW IT WORKS
 * ═══════════════════════════════════════════════════════════════════════════
 * 1. Extract class files from both JARs
 * 2. Compute SHA-256 hash of each class file
 * 3. Compare hashes to identify differing classes
 * 4. For differing classes, analyze WHY they differ:
 *    - Bytecode version (Java 8 vs Java 11)
 *    - Method/field count changes
 *    - Debug info presence
 * 5. Extract manifest metadata (Build-Jdk-Spec, Created-By)
 * 6. Provide actionable recommendations
 * 
 * @author Aashish K C
 */
public class BuildDiffAnalyzer {
    
    /**
     * Compares two JARs and returns detailed diff report.
     */
    public DiffResult compare(String jar1Path, String jar2Path) throws IOException {
        long startTime = System.currentTimeMillis();
        
        // Extract class info from both JARs
        Map<String, ClassInfo> jar1Classes = extractAllClassInfo(jar1Path);
        Map<String, ClassInfo> jar2Classes = extractAllClassInfo(jar2Path);
        
        // Extract manifest metadata
        JarMetadata meta1 = extractMetadata(jar1Path);
        JarMetadata meta2 = extractMetadata(jar2Path);
        
        // Compute differences
        List<ClassDiff> differences = computeDifferences(jar1Classes, jar2Classes);
        
        long endTime = System.currentTimeMillis();
        
        return new DiffResult(
            jar1Path, jar2Path,
            meta1, meta2,
            jar1Classes.size(), jar2Classes.size(),
            differences,
            endTime - startTime
        );
    }
    
    private Map<String, ClassInfo> extractAllClassInfo(String jarPath) throws IOException {
        Map<String, ClassInfo> classes = new HashMap<>();
        
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (entry.getName().endsWith(".class")) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        byte[] bytes = is.readAllBytes();
                        ClassInfo info = analyzeClassBytes(entry.getName(), bytes);
                        classes.put(info.name(), info);
                    }
                }
            }
        }
        
        return classes;
    }
    
    private ClassInfo analyzeClassBytes(String entryName, byte[] bytes) throws IOException {
        String hash = computeHash(bytes);
        ClassReader reader = new ClassReader(bytes);
        ClassInfoCollector collector = new ClassInfoCollector();
        reader.accept(collector, 0); // Don't skip anything - we need debug info
        
        return new ClassInfo(
            entryName.replace(".class", ""),
            hash,
            collector.version,
            collector.methodCount,
            collector.fieldCount,
            collector.hasLineNumbers,
            collector.hasLocalVariables,
            collector.sourceFile
        );
    }
    
    private JarMetadata extractMetadata(String jarPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath)) {
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();
                return new JarMetadata(
                    attrs.getValue("Build-Jdk-Spec"),
                    attrs.getValue("Created-By"),
                    attrs.getValue("Built-By"),
                    attrs.getValue("Build-Timestamp"),
                    attrs.getValue("Main-Class")
                );
            }
        }
        return JarMetadata.empty();
    }
    
    private String computeHash(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 16); // Short hash for display
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    private List<ClassDiff> computeDifferences(Map<String, ClassInfo> jar1,
                                                Map<String, ClassInfo> jar2) {
        List<ClassDiff> diffs = new ArrayList<>();
        
        Set<String> allClasses = new HashSet<>();
        allClasses.addAll(jar1.keySet());
        allClasses.addAll(jar2.keySet());
        
        for (String className : allClasses) {
            ClassInfo c1 = jar1.get(className);
            ClassInfo c2 = jar2.get(className);
            
            if (c1 == null) {
                diffs.add(new ClassDiff(className, DiffType.ONLY_IN_JAR2, 
                    "Class only exists in JAR 2", null));
            } else if (c2 == null) {
                diffs.add(new ClassDiff(className, DiffType.ONLY_IN_JAR1,
                    "Class only exists in JAR 1", null));
            } else if (!c1.hash().equals(c2.hash())) {
                String reason = explainDifference(c1, c2);
                String details = buildDetails(c1, c2);
                diffs.add(new ClassDiff(className, DiffType.DIFFERENT, reason, details));
            }
            // If hashes match, classes are identical - no diff entry needed
        }
        
        return diffs;
    }
    
    /**
     * Analyzes two class files and explains WHY they differ.
     */
    private String explainDifference(ClassInfo c1, ClassInfo c2) {
        List<String> reasons = new ArrayList<>();
        
        // Check bytecode version (most important - causes runtime failures)
        if (c1.bytecodeVersion() != c2.bytecodeVersion()) {
            reasons.add(String.format("Bytecode version: %s vs %s",
                c1.getJavaVersion(), c2.getJavaVersion()));
        }
        
        // Check structural changes
        if (c1.methodCount() != c2.methodCount()) {
            reasons.add(String.format("Method count: %d vs %d",
                c1.methodCount(), c2.methodCount()));
        }
        
        if (c1.fieldCount() != c2.fieldCount()) {
            reasons.add(String.format("Field count: %d vs %d",
                c1.fieldCount(), c2.fieldCount()));
        }
        
        // Check debug info (common source of differences)
        if (c1.hasLineNumbers() != c2.hasLineNumbers()) {
            reasons.add("Line number info differs");
        }
        
        if (c1.hasLocalVariables() != c2.hasLocalVariables()) {
            reasons.add("Local variable debug info differs");
        }
        
        // If no specific reason found, it's likely internal bytecode differences
        if (reasons.isEmpty()) {
            reasons.add("Bytecode differs (likely: constant pool ordering, " +
                       "optimization differences, or timestamp metadata)");
        }
        
        return String.join("; ", reasons);
    }
    
    private String buildDetails(ClassInfo c1, ClassInfo c2) {
        return String.format(
            "JAR1: %s, %d methods, %d fields, debug=%b | " +
            "JAR2: %s, %d methods, %d fields, debug=%b",
            c1.getJavaVersion(), c1.methodCount(), c1.fieldCount(), c1.hasDebugInfo(),
            c2.getJavaVersion(), c2.methodCount(), c2.fieldCount(), c2.hasDebugInfo()
        );
    }
    
    /**
     * ASM ClassVisitor to collect class metadata.
     */
    private static class ClassInfoCollector extends ClassVisitor {
        int version;
        int methodCount = 0;
        int fieldCount = 0;
        boolean hasLineNumbers = false;
        boolean hasLocalVariables = false;
        String sourceFile;
        
        ClassInfoCollector() {
            super(Opcodes.ASM9);
        }
        
        @Override
        public void visit(int version, int access, String name, String signature,
                         String superName, String[] interfaces) {
            this.version = version;
        }
        
        @Override
        public void visitSource(String source, String debug) {
            this.sourceFile = source;
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            methodCount++;
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitLineNumber(int line, Label start) {
                    hasLineNumbers = true;
                }
                
                @Override
                public void visitLocalVariable(String name, String desc, String sig,
                                              Label start, Label end, int index) {
                    hasLocalVariables = true;
                }
            };
        }
        
        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            fieldCount++;
            return null;
        }
    }
}
