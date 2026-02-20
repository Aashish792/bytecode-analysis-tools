package com.aashish.extractor;

import com.aashish.model.*;
import org.objectweb.asm.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * ASM-based implementation for extracting method calls with reflection detection.
 * 
 * KEY CAPABILITIES:
 * 1. Detects all 5 JVM invoke types (virtual, static, special, interface, dynamic)
 * 2. Identifies reflection patterns that hide dependencies
 * 3. Attempts to capture target class/method names from LDC instructions
 * 
 * HOW REFLECTION DETECTION WORKS:
 * When we see code like: Class.forName("com.example.Service")
 * The string "com.example.Service" is loaded via an LDC instruction.
 * We track the last string constant and associate it with reflection calls.
 * 
 * LIMITATIONS:
 * - Cannot track strings passed through variables
 * - Cannot analyze string concatenation
 * - Cannot track reflection in lambdas completely
 */
public class AsmMethodCallExtractor implements MethodCallExtractor {
    
    @Override
    public ExtractionResult extract(String jarPath) throws IOException {
        Set<MethodCall> directCalls = new HashSet<>();
        List<ReflectiveCall> reflectiveCalls = new ArrayList<>();
        
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (isValidClassFile(entry)) {
                    String className = entryToClassName(entry);
                    extractCallsFromClass(jar, entry, className, directCalls, reflectiveCalls);
                }
            }
        }
        
        return new ExtractionResult(directCalls, reflectiveCalls);
    }
    
    private boolean isValidClassFile(JarEntry entry) {
        String name = entry.getName();
        return name.endsWith(".class")
            && !name.equals("module-info.class")
            && !name.equals("package-info.class");
    }
    
    private String entryToClassName(JarEntry entry) {
        return entry.getName()
            .replace("/", ".")
            .replace(".class", "");
    }
    
    private void extractCallsFromClass(JarFile jar, JarEntry entry, String className,
                                        Set<MethodCall> directCalls,
                                        List<ReflectiveCall> reflectiveCalls) throws IOException {
        try (InputStream is = jar.getInputStream(entry)) {
            ClassReader reader = new ClassReader(is);
            reader.accept(
                new MethodCallVisitor(className, directCalls, reflectiveCalls),
                ClassReader.SKIP_DEBUG
            );
        }
    }
    
    /**
     * ASM ClassVisitor that collects method calls and detects reflection.
     */
    private static class MethodCallVisitor extends ClassVisitor {
        private final String sourceClass;
        private final Set<MethodCall> directCalls;
        private final List<ReflectiveCall> reflectiveCalls;
        private String currentMethod = "";
        
        MethodCallVisitor(String sourceClass, Set<MethodCall> directCalls,
                         List<ReflectiveCall> reflectiveCalls) {
            super(Opcodes.ASM9);
            this.sourceClass = sourceClass;
            this.directCalls = directCalls;
            this.reflectiveCalls = reflectiveCalls;
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            currentMethod = name;
            return new InstructionVisitor();
        }
        
        /**
         * Visits individual bytecode instructions within a method.
         */
        private class InstructionVisitor extends MethodVisitor {
            // Track last string constant for reflection target detection
            private String lastStringConstant = null;
            
            InstructionVisitor() {
                super(Opcodes.ASM9);
            }
            
            @Override
            public void visitLdcInsn(Object value) {
                // Track string constants - may be class/method names for reflection
                if (value instanceof String s) {
                    lastStringConstant = s;
                }
            }
            
            @Override
            public void visitMethodInsn(int opcode, String owner, String name,
                                       String descriptor, boolean isInterface) {
                // Record the direct call
                InvokeType invokeType = InvokeType.fromOpcode(opcode);
                directCalls.add(new MethodCall(
                    owner, name, descriptor,
                    invokeType,
                    sourceClass, currentMethod
                ));
                
                // Check for reflection patterns
                ReflectiveCall reflection = detectReflection(owner, name);
                if (reflection != null) {
                    reflectiveCalls.add(reflection);
                    lastStringConstant = null; // Clear after use
                }
            }
            
            @Override
            public void visitInvokeDynamicInsn(String name, String descriptor,
                                              Handle bootstrapMethod,
                                              Object... bootstrapArgs) {
                // INVOKEDYNAMIC - typically lambdas and method references
                directCalls.add(new MethodCall(
                    "java/lang/invoke/LambdaMetafactory",
                    name,
                    descriptor,
                    InvokeType.INVOKEDYNAMIC,
                    sourceClass, currentMethod
                ));
                
                // Check if lambda references reflection
                for (Object arg : bootstrapArgs) {
                    if (arg instanceof Handle h) {
                        if (h.getOwner().startsWith("java/lang/reflect/")) {
                            reflectiveCalls.add(new ReflectiveCall(
                                ReflectiveType.METHOD_INVOKE,
                                sourceClass, currentMethod,
                                "Lambda using reflection",
                                null
                            ));
                        }
                    }
                }
            }
            
            /**
             * Detects reflection patterns and returns ReflectiveCall if found.
             */
            private ReflectiveCall detectReflection(String owner, String name) {
                // Class.forName(String)
                if (owner.equals("java/lang/Class") && name.equals("forName")) {
                    return new ReflectiveCall(
                        ReflectiveType.CLASS_FOR_NAME,
                        sourceClass, currentMethod,
                        "Class.forName()",
                        lastStringConstant
                    );
                }
                
                // Class.getMethod() / getDeclaredMethod()
                if (owner.equals("java/lang/Class") &&
                    (name.equals("getMethod") || name.equals("getDeclaredMethod"))) {
                    return new ReflectiveCall(
                        ReflectiveType.GET_METHOD,
                        sourceClass, currentMethod,
                        name + "()",
                        lastStringConstant
                    );
                }
                
                // Method.invoke()
                if (owner.equals("java/lang/reflect/Method") && name.equals("invoke")) {
                    return new ReflectiveCall(
                        ReflectiveType.METHOD_INVOKE,
                        sourceClass, currentMethod,
                        "Method.invoke()",
                        null
                    );
                }
                
                // Constructor.newInstance()
                if (owner.equals("java/lang/reflect/Constructor") && name.equals("newInstance")) {
                    return new ReflectiveCall(
                        ReflectiveType.CONSTRUCTOR_NEW_INSTANCE,
                        sourceClass, currentMethod,
                        "Constructor.newInstance()",
                        null
                    );
                }
                
                // Class.newInstance() [deprecated]
                if (owner.equals("java/lang/Class") && name.equals("newInstance")) {
                    return new ReflectiveCall(
                        ReflectiveType.CLASS_NEW_INSTANCE,
                        sourceClass, currentMethod,
                        "Class.newInstance()",
                        lastStringConstant
                    );
                }
                
                // Field.get() / Field.set()
                if (owner.equals("java/lang/reflect/Field") &&
                    (name.equals("get") || name.equals("set"))) {
                    return new ReflectiveCall(
                        ReflectiveType.FIELD_ACCESS,
                        sourceClass, currentMethod,
                        "Field." + name + "()",
                        null
                    );
                }
                
                return null;
            }
        }
    }
}
