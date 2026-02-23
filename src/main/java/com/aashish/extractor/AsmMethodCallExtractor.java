package com.aashish.extractor;

import com.aashish.model.*;
import org.objectweb.asm.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * ASM-based implementation for extracting method calls with reflection detection.
 */
public class AsmMethodCallExtractor implements MethodCallExtractor {

    @Override
    public ExtractionResult extract(String jarPath) throws IOException {
        Set<MethodCall> directCalls = new HashSet<>();
        List<ReflectiveCall> reflectiveCalls = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
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
                        String className = entryToClassName(entry);
                        extractCallsFromClass(jar, entry, className, directCalls, reflectiveCalls);
                    } catch (Exception e) {
                        errors.add("Error processing " + entry.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        return new ExtractionResult(directCalls, reflectiveCalls, errors);
    }
    
    private boolean isValidClassFile(JarEntry entry) {
        String entryName = entry.getName();
        return entryName.endsWith(".class")
            && !entryName.equals("module-info.class")
            && !entryName.equals("package-info.class")
            && !entryName.contains("META-INF/versions/");
    }
    
    private String entryToClassName(JarEntry entry) {
        return entry.getName().replace("/", ".").replace(".class", "");
    }
    
    private void extractCallsFromClass(JarFile jar, JarEntry entry, String className,
                                        Set<MethodCall> directCalls,
                                        List<ReflectiveCall> reflectiveCalls) throws IOException {
        try (InputStream is = jar.getInputStream(entry)) {
            byte[] bytes = is.readAllBytes();
            ClassReader reader = new ClassReader(bytes);
            reader.accept(
                new MethodCallVisitor(className, directCalls, reflectiveCalls),
                ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES
            );
        }
    }
    
    private static class MethodCallVisitor extends ClassVisitor {
        private final String sourceClass;
        private final Set<MethodCall> directCalls;
        private final List<ReflectiveCall> reflectiveCalls;
        private String currentMethod = "<unknown>";
        
        MethodCallVisitor(String sourceClass, Set<MethodCall> directCalls,
                         List<ReflectiveCall> reflectiveCalls) {
            super(Opcodes.ASM9);
            this.sourceClass = sourceClass;
            this.directCalls = directCalls;
            this.reflectiveCalls = reflectiveCalls;
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor,
                                         String signature, String[] exceptions) {
            currentMethod = methodName;
            return new InstructionVisitor();
        }
        
        private class InstructionVisitor extends MethodVisitor {
            private String lastStringConstant = null;
            private String secondLastStringConstant = null;
            
            InstructionVisitor() {
                super(Opcodes.ASM9);
            }
            
            @Override
            public void visitLdcInsn(Object value) {
                if (value instanceof String s) {
                    secondLastStringConstant = lastStringConstant;
                    lastStringConstant = s;
                }
            }
            
            @Override
            public void visitMethodInsn(int opcode, String owner, String methodName,
                                       String descriptor, boolean isInterface) {
                try {
                    // Record the direct call
                    InvokeType invokeType = InvokeType.fromOpcode(opcode);
                    directCalls.add(new MethodCall(
                        owner, methodName, descriptor,
                        invokeType,
                        sourceClass, currentMethod
                    ));
                    
                    // Check for reflection patterns
                    ReflectiveCall reflection = detectReflection(owner, methodName);
                    if (reflection != null) {
                        reflectiveCalls.add(reflection);
                    }
                } catch (Exception e) {
                    // Skip problematic instructions
                }
            }
            
            @Override
            public void visitInvokeDynamicInsn(String methodName, String descriptor,
                                              Handle bootstrapMethod,
                                              Object... bootstrapArgs) {
                try {
                    directCalls.add(new MethodCall(
                        "java/lang/invoke/LambdaMetafactory",
                        methodName,
                        descriptor,
                        InvokeType.INVOKEDYNAMIC,
                        sourceClass, currentMethod
                    ));
                    
                    // Check bootstrap args for reflection
                    for (Object arg : bootstrapArgs) {
                        if (arg instanceof Handle h) {
                            if (h.getOwner().startsWith("java/lang/reflect/")) {
                                reflectiveCalls.add(new ReflectiveCall(
                                    ReflectiveType.METHOD_INVOKE,
                                    sourceClass, currentMethod,
                                    "Lambda with reflection",
                                    null
                                ));
                            }
                        }
                    }
                } catch (Exception e) {
                    // Skip problematic instructions
                }
            }
            
            private ReflectiveCall detectReflection(String owner, String methodName) {
                // Class.forName(String)
                if ("java/lang/Class".equals(owner) && "forName".equals(methodName)) {
                    return new ReflectiveCall(
                        ReflectiveType.CLASS_FOR_NAME,
                        sourceClass, currentMethod,
                        "Class.forName()",
                        lastStringConstant
                    );
                }
                
                // Class.getMethod()
                if ("java/lang/Class".equals(owner) && "getMethod".equals(methodName)) {
                    return new ReflectiveCall(
                        ReflectiveType.GET_METHOD,
                        sourceClass, currentMethod,
                        "getMethod()",
                        lastStringConstant
                    );
                }
                
                // Class.getDeclaredMethod()
                if ("java/lang/Class".equals(owner) && "getDeclaredMethod".equals(methodName)) {
                    return new ReflectiveCall(
                        ReflectiveType.GET_DECLARED_METHOD,
                        sourceClass, currentMethod,
                        "getDeclaredMethod()",
                        lastStringConstant
                    );
                }
                
                // Method.invoke()
                if ("java/lang/reflect/Method".equals(owner) && "invoke".equals(methodName)) {
                    return new ReflectiveCall(
                        ReflectiveType.METHOD_INVOKE,
                        sourceClass, currentMethod,
                        "Method.invoke()",
                        null
                    );
                }
                
                // Constructor.newInstance()
                if ("java/lang/reflect/Constructor".equals(owner) && "newInstance".equals(methodName)) {
                    return new ReflectiveCall(
                        ReflectiveType.CONSTRUCTOR_NEW_INSTANCE,
                        sourceClass, currentMethod,
                        "Constructor.newInstance()",
                        null
                    );
                }
                
                // Class.newInstance() [deprecated]
                if ("java/lang/Class".equals(owner) && "newInstance".equals(methodName)) {
                    return new ReflectiveCall(
                        ReflectiveType.CLASS_NEW_INSTANCE,
                        sourceClass, currentMethod,
                        "Class.newInstance()",
                        lastStringConstant
                    );
                }
                
                // Field.get()
                if ("java/lang/reflect/Field".equals(owner) && "get".equals(methodName)) {
                    return new ReflectiveCall(
                        ReflectiveType.FIELD_GET,
                        sourceClass, currentMethod,
                        "Field.get()",
                        null
                    );
                }
                
                // Field.set()
                if ("java/lang/reflect/Field".equals(owner) && "set".equals(methodName)) {
                    return new ReflectiveCall(
                        ReflectiveType.FIELD_SET,
                        sourceClass, currentMethod,
                        "Field.set()",
                        null
                    );
                }
                
                // Object.getClass()
                if ("java/lang/Object".equals(owner) && "getClass".equals(methodName)) {
                    return new ReflectiveCall(
                        ReflectiveType.GET_CLASS,
                        sourceClass, currentMethod,
                        "getClass()",
                        null
                    );
                }
                
                // Class.getName() / getSimpleName() / getCanonicalName()
                if ("java/lang/Class".equals(owner) && 
                    (methodName.equals("getName") || methodName.equals("getSimpleName") || methodName.equals("getCanonicalName"))) {
                    return new ReflectiveCall(
                        ReflectiveType.CLASS_GET_NAME,
                        sourceClass, currentMethod,
                        "Class." + methodName + "()",
                        null
                    );
                }
                
                return null;
            }
        }
    }
}
