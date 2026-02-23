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
 * Analyzes and explains differences between two JAR files.
 */
public class BuildDiffAnalyzer {
    
    public DiffResult compare(String jar1Path, String jar2Path) {
        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        
        try {
            // Extract class info from both JARs
            Map<String, ClassInfo> jar1Classes = extractAllClassInfo(jar1Path, warnings);
            Map<String, ClassInfo> jar2Classes = extractAllClassInfo(jar2Path, warnings);
            
            // Extract manifest metadata
            JarMetadata meta1 = extractMetadata(jar1Path);
            JarMetadata meta2 = extractMetadata(jar2Path);
            
            // Check for JDK mismatch
            if (meta1.buildJdk() != null && meta2.buildJdk() != null 
                && !meta1.buildJdk().equals(meta2.buildJdk())) {
                warnings.add("JDK version mismatch: " + meta1.buildJdk() + " vs " + meta2.buildJdk());
            }
            
            // Compute differences
            List<ClassDiff> differences = computeDifferences(jar1Classes, jar2Classes);
            
            long endTime = System.currentTimeMillis();
            
            return new DiffResult(
                jar1Path, jar2Path,
                meta1, meta2,
                jar1Classes.size(), jar2Classes.size(),
                differences,
                endTime - startTime,
                warnings
            );
            
        } catch (Exception e) {
            warnings.add("Analysis error: " + e.getMessage());
            return new DiffResult(
                jar1Path, jar2Path,
                JarMetadata.empty(), JarMetadata.empty(),
                0, 0,
                List.of(),
                System.currentTimeMillis() - startTime,
                warnings
            );
        }
    }
    
    private Map<String, ClassInfo> extractAllClassInfo(String jarPath, List<String> warnings) throws IOException {
        Map<String, ClassInfo> classes = new HashMap<>();
        
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            throw new IOException("JAR file not found: " + jarPath);
        }
        
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (entry.getName().endsWith(".class") && !entry.getName().contains("META-INF")) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        byte[] bytes = is.readAllBytes();
                        ClassInfo info = analyzeClassBytes(entry.getName(), bytes, entry.getSize());
                        classes.put(info.className(), info);
                    } catch (Exception e) {
                        warnings.add("Skipped " + entry.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        return classes;
    }
    
    private ClassInfo analyzeClassBytes(String entryName, byte[] bytes, long size) throws IOException {
        String hash = computeHash(bytes);
        ClassReader reader = new ClassReader(bytes);
        ClassInfoCollector collector = new ClassInfoCollector();
        reader.accept(collector, 0);
        
        return new ClassInfo(
            entryName.replace(".class", "").replace("/", "."),
            hash,
            collector.version,
            collector.methodCount,
            collector.fieldCount,
            collector.hasLineNumbers,
            collector.hasLocalVariables,
            collector.sourceFile,
            size
        );
    }
    
    private JarMetadata extractMetadata(String jarPath) {
        try (JarFile jar = new JarFile(jarPath)) {
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();
                return new JarMetadata(
                    attrs.getValue("Build-Jdk-Spec"),
                    attrs.getValue("Created-By"),
                    attrs.getValue("Built-By"),
                    attrs.getValue("Build-Timestamp"),
                    attrs.getValue("Main-Class"),
                    attrs.getValue("Manifest-Version")
                );
            }
        } catch (Exception e) {
            // Ignore manifest errors
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
            return sb.toString().substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "hash-error";
        }
    }
    
    private List<ClassDiff> computeDifferences(Map<String, ClassInfo> jar1, Map<String, ClassInfo> jar2) {
        List<ClassDiff> diffs = new ArrayList<>();
        
        Set<String> allClasses = new HashSet<>();
        allClasses.addAll(jar1.keySet());
        allClasses.addAll(jar2.keySet());
        
        for (String className : allClasses) {
            ClassInfo c1 = jar1.get(className);
            ClassInfo c2 = jar2.get(className);
            
            if (c1 == null) {
                diffs.add(new ClassDiff(className, DiffType.ONLY_IN_JAR2, 
                    "Only in JAR 2", null));
            } else if (c2 == null) {
                diffs.add(new ClassDiff(className, DiffType.ONLY_IN_JAR1,
                    "Only in JAR 1", null));
            } else if (!c1.hash().equals(c2.hash())) {
                String reason = explainDifference(c1, c2);
                String details = buildDetails(c1, c2);
                diffs.add(new ClassDiff(className, DiffType.DIFFERENT, reason, details));
            }
        }
        
        return diffs;
    }
    
    private String explainDifference(ClassInfo c1, ClassInfo c2) {
        List<String> reasons = new ArrayList<>();
        
        if (c1.bytecodeVersion() != c2.bytecodeVersion()) {
            reasons.add("Bytecode version: " + c1.getJavaVersion() + " vs " + c2.getJavaVersion());
        }
        if (c1.methodCount() != c2.methodCount()) {
            reasons.add("Method count: " + c1.methodCount() + " vs " + c2.methodCount());
        }
        if (c1.fieldCount() != c2.fieldCount()) {
            reasons.add("Field count: " + c1.fieldCount() + " vs " + c2.fieldCount());
        }
        if (c1.hasLineNumbers() != c2.hasLineNumbers()) {
            reasons.add("Line numbers: " + c1.hasLineNumbers() + " vs " + c2.hasLineNumbers());
        }
        if (c1.hasLocalVariables() != c2.hasLocalVariables()) {
            reasons.add("Local variables: " + c1.hasLocalVariables() + " vs " + c2.hasLocalVariables());
        }
        if (c1.size() != c2.size()) {
            reasons.add("Size: " + c1.size() + " vs " + c2.size() + " bytes");
        }
        
        if (reasons.isEmpty()) {
            reasons.add("Bytecode differs (constant pool, timestamps, or optimizations)");
        }
        
        return String.join("; ", reasons);
    }
    
    private String buildDetails(ClassInfo c1, ClassInfo c2) {
        return String.format("JAR1: %s, %d methods | JAR2: %s, %d methods",
            c1.getJavaVersion(), c1.methodCount(),
            c2.getJavaVersion(), c2.methodCount());
    }
    
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
        public void visit(int version, int access, String className, String signature,
                         String superName, String[] interfaces) {
            this.version = version;
        }
        
        @Override
        public void visitSource(String source, String debug) {
            this.sourceFile = source;
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor,
                                         String signature, String[] exceptions) {
            methodCount++;
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitLineNumber(int line, Label start) {
                    hasLineNumbers = true;
                }
                
                @Override
                public void visitLocalVariable(String varName, String desc, String sig,
                                              Label start, Label end, int index) {
                    hasLocalVariables = true;
                }
            };
        }
        
        @Override
        public FieldVisitor visitField(int access, String fieldName, String descriptor,
                                       String signature, Object value) {
            fieldCount++;
            return null;
        }
    }
}
