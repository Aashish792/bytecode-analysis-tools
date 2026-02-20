package com.aashish;

import com.aashish.analyzer.*;
import com.aashish.extractor.*;
import com.aashish.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Bytecode Analysis Tools.
 * 
 * TEST CATEGORIES:
 * 1. Model tests - Verify data classes work correctly
 * 2. InvokeType tests - Verify opcode mapping
 * 3. AnalysisResult tests - Verify builder and computed properties
 * 4. DiffResult tests - Verify build comparison logic
 * 5. Integration tests - Verify full analysis workflow
 * 
 * Run with: mvn test
 */
class BytecodeAnalysisToolsTest {

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. METHOD SIGNATURE TESTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("MethodSignature Tests")
    class MethodSignatureTests {
        
        @Test
        @DisplayName("Should create valid MethodSignature")
        void testCreation() {
            MethodSignature sig = new MethodSignature("java/util/List", "add", "(Ljava/lang/Object;)Z");
            
            assertEquals("java/util/List", sig.owner());
            assertEquals("add", sig.name());
            assertEquals("(Ljava/lang/Object;)Z", sig.descriptor());
        }
        
        @Test
        @DisplayName("Should convert to readable string")
        void testToReadableString() {
            MethodSignature sig = new MethodSignature("com/example/MyClass", "process", "()V");
            assertEquals("com.example.MyClass.process", sig.toReadableString());
        }
        
        @Test
        @DisplayName("Should convert to short string")
        void testToShortString() {
            MethodSignature sig = new MethodSignature("com/example/deep/MyClass", "process", "()V");
            assertEquals("MyClass.process", sig.toShortString());
        }
        
        @Test
        @DisplayName("Should work correctly in HashSet (equality)")
        void testHashSetBehavior() {
            MethodSignature sig1 = new MethodSignature("Test", "method", "()V");
            MethodSignature sig2 = new MethodSignature("Test", "method", "()V");
            MethodSignature sig3 = new MethodSignature("Test", "other", "()V");
            
            Set<MethodSignature> set = new HashSet<>();
            set.add(sig1);
            
            assertTrue(set.contains(sig2), "Equal signatures should be found");
            assertFalse(set.contains(sig3), "Different signatures should not be found");
            assertEquals(1, set.size(), "Duplicate should not be added");
        }
        
        @Test
        @DisplayName("Should reject null values")
        void testNullRejection() {
            assertThrows(NullPointerException.class, 
                () -> new MethodSignature(null, "method", "()V"));
            assertThrows(NullPointerException.class, 
                () -> new MethodSignature("Owner", null, "()V"));
            assertThrows(NullPointerException.class, 
                () -> new MethodSignature("Owner", "method", null));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. INVOKE TYPE TESTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("InvokeType Tests")
    class InvokeTypeTests {
        
        @ParameterizedTest
        @DisplayName("Should map opcodes correctly")
        @CsvSource({
            "182, INVOKEVIRTUAL",
            "183, INVOKESPECIAL",
            "184, INVOKESTATIC",
            "185, INVOKEINTERFACE",
            "186, INVOKEDYNAMIC"
        })
        void testOpcodeMapping(int opcode, String expectedType) {
            InvokeType result = InvokeType.fromOpcode(opcode);
            assertEquals(InvokeType.valueOf(expectedType), result);
        }
        
        @Test
        @DisplayName("Should have descriptions for all types")
        void testDescriptions() {
            for (InvokeType type : InvokeType.values()) {
                assertNotNull(type.getDescription(), type + " should have description");
                assertNotNull(type.getExample(), type + " should have example");
                assertFalse(type.getDescription().isEmpty());
                assertFalse(type.getExample().isEmpty());
            }
        }
        
        @Test
        @DisplayName("Should throw on invalid opcode")
        void testInvalidOpcode() {
            assertThrows(IllegalArgumentException.class, 
                () -> InvokeType.fromOpcode(999));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. METHOD CALL TESTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("MethodCall Tests")
    class MethodCallTests {
        
        @Test
        @DisplayName("Should convert to MethodSignature")
        void testToSignature() {
            MethodCall call = new MethodCall(
                "java/util/List", "add", "(Ljava/lang/Object;)Z",
                InvokeType.INVOKEINTERFACE, "MyClass", "myMethod"
            );
            
            MethodSignature sig = call.toSignature();
            
            assertEquals("java/util/List", sig.owner());
            assertEquals("add", sig.name());
            assertEquals("(Ljava/lang/Object;)Z", sig.descriptor());
        }
        
        @Test
        @DisplayName("Should format readable string")
        void testToReadableString() {
            MethodCall call = new MethodCall(
                "com/example/Service", "process", "()V",
                InvokeType.INVOKEVIRTUAL, "Caller", "call"
            );
            
            String readable = call.toReadableString();
            
            assertTrue(readable.contains("INVOKEVIRTUAL"));
            assertTrue(readable.contains("com.example.Service"));
            assertTrue(readable.contains("process"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. REFLECTIVE CALL TESTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("ReflectiveCall Tests")
    class ReflectiveCallTests {
        
        @Test
        @DisplayName("Should format with target when available")
        void testWithTarget() {
            ReflectiveCall call = new ReflectiveCall(
                ReflectiveType.CLASS_FOR_NAME,
                "MyLoader", "load",
                "Class.forName()",
                "com.example.Target"
            );
            
            String readable = call.toReadableString();
            
            assertTrue(readable.contains("Class.forName()"));
            assertTrue(readable.contains("MyLoader.load"));
            assertTrue(readable.contains("com.example.Target"));
        }
        
        @Test
        @DisplayName("Should format without target")
        void testWithoutTarget() {
            ReflectiveCall call = new ReflectiveCall(
                ReflectiveType.METHOD_INVOKE,
                "Invoker", "invoke",
                "Method.invoke()",
                null
            );
            
            String readable = call.toReadableString();
            
            assertTrue(readable.contains("Method.invoke()"));
            assertFalse(readable.contains("target"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5. ANALYSIS RESULT TESTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("AnalysisResult Tests")
    class AnalysisResultTests {
        
        @Test
        @DisplayName("Should build with all fields")
        void testBuilder() {
            Set<MethodCall> calls = Set.of(
                new MethodCall("T", "m1", "()V", InvokeType.INVOKEVIRTUAL, "C", "c"),
                new MethodCall("T", "m2", "()V", InvokeType.INVOKESTATIC, "C", "c")
            );
            
            AnalysisResult result = AnalysisResult.builder()
                .sourceJar("source.jar")
                .targetJar("target.jar")
                .targetMethodCount(100)
                .sourceCallCount(500)
                .matchingCalls(calls)
                .reflectiveCalls(List.of())
                .analysisTimeMs(42)
                .build();
            
            assertEquals("source.jar", result.getSourceJar());
            assertEquals("target.jar", result.getTargetJar());
            assertEquals(100, result.getTargetMethodCount());
            assertEquals(500, result.getSourceCallCount());
            assertEquals(2, result.getMatchingCallCount());
            assertEquals(42, result.getAnalysisTimeMs());
        }
        
        @Test
        @DisplayName("Should calculate coverage percentage")
        void testCoverageCalculation() {
            // 2 unique methods called out of 100 defined = 2%
            Set<MethodCall> calls = Set.of(
                new MethodCall("T", "m1", "()V", InvokeType.INVOKEVIRTUAL, "C", "c"),
                new MethodCall("T", "m2", "()V", InvokeType.INVOKESTATIC, "C", "c")
            );
            
            AnalysisResult result = AnalysisResult.builder()
                .targetMethodCount(100)
                .matchingCalls(calls)
                .reflectiveCalls(List.of())
                .build();
            
            assertEquals(2.0, result.getCoveragePercentage(), 0.01);
        }
        
        @Test
        @DisplayName("Should group calls by invoke type")
        void testCallsByInvokeType() {
            Set<MethodCall> calls = Set.of(
                new MethodCall("T", "m1", "()V", InvokeType.INVOKEVIRTUAL, "C", "c"),
                new MethodCall("T", "m2", "()V", InvokeType.INVOKEVIRTUAL, "C", "c"),
                new MethodCall("T", "m3", "()V", InvokeType.INVOKESTATIC, "C", "c")
            );
            
            AnalysisResult result = AnalysisResult.builder()
                .matchingCalls(calls)
                .reflectiveCalls(List.of())
                .build();
            
            Map<InvokeType, Long> byType = result.getCallsByInvokeType();
            
            assertEquals(2L, byType.get(InvokeType.INVOKEVIRTUAL));
            assertEquals(1L, byType.get(InvokeType.INVOKESTATIC));
        }
        
        @Test
        @DisplayName("Should detect reflection warnings")
        void testReflectionWarnings() {
            AnalysisResult withReflection = AnalysisResult.builder()
                .matchingCalls(Set.of())
                .reflectiveCalls(List.of(
                    new ReflectiveCall(ReflectiveType.CLASS_FOR_NAME, "C", "m", "pattern", null)
                ))
                .build();
            
            AnalysisResult withoutReflection = AnalysisResult.builder()
                .matchingCalls(Set.of())
                .reflectiveCalls(List.of())
                .build();
            
            assertTrue(withReflection.hasReflectionWarnings());
            assertFalse(withoutReflection.hasReflectionWarnings());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 6. DIFF RESULT TESTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("DiffResult Tests")
    class DiffResultTests {
        
        @Test
        @DisplayName("Should detect JDK mismatch")
        void testJdkMismatchDetection() {
            DiffResult.JarMetadata meta1 = new DiffResult.JarMetadata("8", null, null, null, null);
            DiffResult.JarMetadata meta2 = new DiffResult.JarMetadata("11", null, null, null, null);
            
            DiffResult result = new DiffResult(
                "jar1.jar", "jar2.jar",
                meta1, meta2,
                100, 100,
                List.of(),
                10
            );
            
            assertTrue(result.hasJdkMismatch());
        }
        
        @Test
        @DisplayName("Should not detect mismatch when JDKs match")
        void testNoJdkMismatch() {
            DiffResult.JarMetadata meta1 = new DiffResult.JarMetadata("11", null, null, null, null);
            DiffResult.JarMetadata meta2 = new DiffResult.JarMetadata("11", null, null, null, null);
            
            DiffResult result = new DiffResult(
                "jar1.jar", "jar2.jar",
                meta1, meta2,
                100, 100,
                List.of(),
                10
            );
            
            assertFalse(result.hasJdkMismatch());
        }
        
        @Test
        @DisplayName("Should count differences correctly")
        void testDifferenceCounting() {
            List<DiffResult.ClassDiff> diffs = List.of(
                new DiffResult.ClassDiff("A", DiffResult.DiffType.DIFFERENT, "reason", null),
                new DiffResult.ClassDiff("B", DiffResult.DiffType.DIFFERENT, "reason", null),
                new DiffResult.ClassDiff("C", DiffResult.DiffType.ONLY_IN_JAR1, "reason", null),
                new DiffResult.ClassDiff("D", DiffResult.DiffType.ONLY_IN_JAR2, "reason", null)
            );
            
            DiffResult result = new DiffResult(
                "jar1.jar", "jar2.jar",
                DiffResult.JarMetadata.empty(), DiffResult.JarMetadata.empty(),
                100, 101,
                diffs,
                10
            );
            
            assertEquals(2, result.getDifferentCount());
            assertEquals(1, result.getOnlyInJar1Count());
            assertEquals(1, result.getOnlyInJar2Count());
            assertFalse(result.areIdentical());
        }
        
        @Test
        @DisplayName("Should report identical when no differences")
        void testIdentical() {
            DiffResult result = new DiffResult(
                "jar1.jar", "jar2.jar",
                DiffResult.JarMetadata.empty(), DiffResult.JarMetadata.empty(),
                100, 100,
                List.of(),
                10
            );
            
            assertTrue(result.areIdentical());
        }
        
        @Test
        @DisplayName("Should provide recommendation for JDK mismatch")
        void testRecommendation() {
            DiffResult.JarMetadata meta1 = new DiffResult.JarMetadata("8", null, null, null, null);
            DiffResult.JarMetadata meta2 = new DiffResult.JarMetadata("11", null, null, null, null);
            
            DiffResult result = new DiffResult(
                "jar1.jar", "jar2.jar",
                meta1, meta2,
                100, 100,
                List.of(),
                10
            );
            
            String recommendation = result.getRecommendation();
            
            assertTrue(recommendation.contains("-release"));
            assertTrue(recommendation.contains("JEP 247"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 7. CLASS INFO TESTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("ClassInfo Tests")
    class ClassInfoTests {
        
        @ParameterizedTest
        @DisplayName("Should map bytecode version to Java version")
        @CsvSource({
            "52, Java 8",
            "55, Java 11",
            "61, Java 17",
            "65, Java 21"
        })
        void testJavaVersionMapping(int bytecodeVersion, String expectedJava) {
            ClassInfo info = new ClassInfo("Test", "hash", bytecodeVersion, 10, 5, true, true, "Test.java");
            assertEquals(expectedJava, info.getJavaVersion());
        }
        
        @Test
        @DisplayName("Should detect debug info presence")
        void testDebugInfoDetection() {
            ClassInfo withDebug = new ClassInfo("Test", "hash", 52, 10, 5, true, true, "Test.java");
            ClassInfo withPartialDebug = new ClassInfo("Test", "hash", 52, 10, 5, true, false, "Test.java");
            ClassInfo withoutDebug = new ClassInfo("Test", "hash", 52, 10, 5, false, false, "Test.java");
            
            assertTrue(withDebug.hasDebugInfo());
            assertTrue(withPartialDebug.hasDebugInfo());
            assertFalse(withoutDebug.hasDebugInfo());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 8. INTEGRATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("JarDependencyAnalyzer should handle missing files")
        void testMissingFiles() {
            JarDependencyAnalyzer analyzer = new JarDependencyAnalyzer();
            
            assertThrows(Exception.class, () -> 
                analyzer.analyze("nonexistent1.jar", "nonexistent2.jar")
            );
        }
        
        @Test
        @DisplayName("BuildDiffAnalyzer should handle missing files")
        void testDiffMissingFiles() {
            BuildDiffAnalyzer analyzer = new BuildDiffAnalyzer();
            
            assertThrows(Exception.class, () -> 
                analyzer.compare("nonexistent1.jar", "nonexistent2.jar")
            );
        }
        
        @Test
        @DisplayName("Extractors should be injectable")
        void testDependencyInjection() {
            // Mock extractor that returns empty results
            MethodDefinitionExtractor mockDefExtractor = path -> Set.of();
            MethodCallExtractor mockCallExtractor = path -> 
                new MethodCallExtractor.ExtractionResult(Set.of(), List.of());
            
            JarDependencyAnalyzer analyzer = new JarDependencyAnalyzer(
                mockDefExtractor, 
                mockCallExtractor
            );
            
            assertNotNull(analyzer);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 9. REFLECTIVE TYPE TESTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("ReflectiveType Tests")
    class ReflectiveTypeTests {
        
        @Test
        @DisplayName("All types should have pattern, description, and implication")
        void testAllFieldsPresent() {
            for (ReflectiveType type : ReflectiveType.values()) {
                assertNotNull(type.getPattern(), type + " should have pattern");
                assertNotNull(type.getDescription(), type + " should have description");
                assertNotNull(type.getImplication(), type + " should have implication");
                
                assertFalse(type.getPattern().isEmpty());
                assertFalse(type.getDescription().isEmpty());
                assertFalse(type.getImplication().isEmpty());
            }
        }
    }
}
