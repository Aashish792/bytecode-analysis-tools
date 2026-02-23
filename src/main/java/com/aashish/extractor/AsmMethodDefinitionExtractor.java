package com.aashish.extractor;

import com.aashish.model.MethodSignature;
import org.objectweb.asm.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * ASM-based implementation for extracting method definitions.
 */
public class AsmMethodDefinitionExtractor implements MethodDefinitionExtractor {

    @Override
    public Set<MethodSignature> extract(String jarPath) throws IOException {
        Set<MethodSignature> methods = new HashSet<>();
        
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            throw new IOException("JAR file not found: " + jarPath);
        }
        
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (isValidClassFile(entry)) {
                    try {
                        extractMethodsFromClass(jar, entry, methods);
                    } catch (Exception e) {
                        // Skip problematic classes, continue with others
                    }
                }
            }
        }
        
        return methods;
    }
    
    private boolean isValidClassFile(JarEntry entry) {
        String entryName = entry.getName();
        return entryName.endsWith(".class")
            && !entryName.equals("module-info.class")
            && !entryName.equals("package-info.class")
            && !entryName.contains("META-INF/versions/");
    }
    
    private void extractMethodsFromClass(JarFile jar, JarEntry entry, 
                                          Set<MethodSignature> methods) throws IOException {
        try (InputStream is = jar.getInputStream(entry)) {
            byte[] bytes = is.readAllBytes();
            ClassReader reader = new ClassReader(bytes);
            reader.accept(new MethodDefinitionVisitor(methods), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
    }
    
    private static class MethodDefinitionVisitor extends ClassVisitor {
        private final Set<MethodSignature> methods;
        private String className;
        
        MethodDefinitionVisitor(Set<MethodSignature> methods) {
            super(Opcodes.ASM9);
            this.methods = methods;
        }
        
        @Override
        public void visit(int version, int access, String classname, String signature,
                         String superName, String[] interfaces) {
            this.className = classname;
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor,
                                         String signature, String[] exceptions) {
            // Skip synthetic and bridge methods
            if ((access & (Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE)) == 0) {
                methods.add(new MethodSignature(className, methodName, descriptor));
            }
            return null;
        }
    }
}
