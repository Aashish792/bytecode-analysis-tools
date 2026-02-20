package com.aashish.extractor;

import com.aashish.model.MethodSignature;
import org.objectweb.asm.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * ASM-based implementation for extracting method definitions.
 * 
 * Uses ASM's visitor pattern for memory-efficient analysis.
 * Does not load classes into JVM - works purely on bytecode.
 * 
 * WHY ASM?
 * - Industry standard (used by Spring, Gradle, Hibernate)
 * - Fastest bytecode library available
 * - Event-driven visitor pattern = low memory footprint
 * - Supports all Java versions including Java 21
 */
public class AsmMethodDefinitionExtractor implements MethodDefinitionExtractor {
    
    @Override
    public Set<MethodSignature> extract(String jarPath) throws IOException {
        Set<MethodSignature> methods = new HashSet<>();
        
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (isValidClassFile(entry)) {
                    extractMethodsFromClass(jar, entry, methods);
                }
            }
        }
        
        return methods;
    }
    
    private boolean isValidClassFile(JarEntry entry) {
        String name = entry.getName();
        return name.endsWith(".class")
            && !name.equals("module-info.class")
            && !name.equals("package-info.class")
            && !name.contains("$"); // Skip inner classes for cleaner results
    }
    
    private void extractMethodsFromClass(JarFile jar, JarEntry entry, 
                                          Set<MethodSignature> methods) throws IOException {
        try (InputStream is = jar.getInputStream(entry)) {
            ClassReader reader = new ClassReader(is);
            reader.accept(new MethodDefinitionVisitor(methods), ClassReader.SKIP_DEBUG);
        }
    }
    
    /**
     * ASM ClassVisitor that collects method definitions.
     */
    private static class MethodDefinitionVisitor extends ClassVisitor {
        private final Set<MethodSignature> methods;
        private String className;
        
        MethodDefinitionVisitor(Set<MethodSignature> methods) {
            super(Opcodes.ASM9);
            this.methods = methods;
        }
        
        @Override
        public void visit(int version, int access, String name, String signature,
                         String superName, String[] interfaces) {
            this.className = name;
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            // Skip synthetic and bridge methods (compiler-generated)
            boolean isSynthetic = (access & Opcodes.ACC_SYNTHETIC) != 0;
            boolean isBridge = (access & Opcodes.ACC_BRIDGE) != 0;
            
            if (!isSynthetic && !isBridge) {
                methods.add(new MethodSignature(className, name, descriptor));
            }
            
            return null; // We don't need to visit method body
        }
    }
}
