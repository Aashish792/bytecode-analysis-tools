package com.aashish;

import com.aashish.analyzer.*;
import com.aashish.extractor.*;
import com.aashish.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeAnalysisToolsTest {

    @Nested
    @DisplayName("MethodSignature Tests")
    class MethodSignatureTests {
        
        @Test
        @DisplayName("Should create valid signature")
        void testCreation() {
            MethodSignature sig = new MethodSignature("java/util/List", "add", "(Ljava/lang/Object;)Z");
            assertEquals("java/util/List", sig.owner());
            assertEquals("add", sig.methodName());
        }
        
        @Test
        @DisplayName("Should format readable string")
        void testReadable() {
            MethodSignature sig = new MethodSignature("com/example/MyClass", "process", "()V");
            assertEquals("com.example.MyClass.process", sig.toReadableString());
        }
        
        @Test
        @DisplayName("Should work in HashSet")
        void testHashSet() {
            MethodSignature sig1 = new MethodSignature("Test", "method", "()V");
            MethodSignature sig2 = new MethodSignature("Test", "method", "()V");
            Set<MethodSignature> set = new HashSet<>();
            set.add(sig1);
            assertTrue(set.contains(sig2));
            assertEquals(1, set.size());
        }
        
        @Test
        @DisplayName("Should reject null")
        void testNullRejection() {
            assertThrows(NullPointerException.class, () -> new MethodSignature(null, "m", "()V"));
        }
    }

    @Nested
    @DisplayName("InvokeType Tests")
    class InvokeTypeTests {
        
        @ParameterizedTest
        @CsvSource({"182,INVOKEVIRTUAL", "183,INVOKESPECIAL", "184,INVOKESTATIC", "185,INVOKEINTERFACE"})
        void testOpcodeMapping(int opcode, String expected) {
            assertEquals(InvokeType.valueOf(expected), InvokeType.fromOpcode(opcode));
        }
        
        @Test
        @DisplayName("Should default to INVOKEDYNAMIC for unknown")
        void testUnknown() {
            assertEquals(InvokeType.INVOKEDYNAMIC, InvokeType.fromOpcode(999));
        }
        
        @Test
        @DisplayName("All types should have descriptions")
        void testDescriptions() {
            for (InvokeType type : InvokeType.values()) {
                assertNotNull(type.getDescription());
                assertFalse(type.getDescription().isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("MethodCall Tests")
    class MethodCallTests {
        
        @Test
        @DisplayName("Should convert to signature")
        void testToSignature() {
            MethodCall call = new MethodCall("Owner", "method", "()V", InvokeType.INVOKEVIRTUAL, "Caller", "call");
            MethodSignature sig = call.toSignature();
            assertEquals("Owner", sig.owner());
            assertEquals("method", sig.methodName());
        }
        
        @Test
        @DisplayName("Should format readable string")
        void testReadable() {
            MethodCall call = new MethodCall("com/example/Svc", "run", "()V", InvokeType.INVOKESTATIC, "C", "m");
            assertTrue(call.toReadableString().contains("INVOKESTATIC"));
            assertTrue(call.toReadableString().contains("com.example.Svc"));
        }
    }

    @Nested
    @DisplayName("ReflectiveType Tests")
    class ReflectiveTypeTests {
        
        @Test
        @DisplayName("All types should have pattern and description")
        void testFields() {
            for (ReflectiveType type : ReflectiveType.values()) {
                assertNotNull(type.getPattern());
                assertNotNull(type.getDescription());
            }
        }
    }

    @Nested
    @DisplayName("AnalysisResult Tests")
    class AnalysisResultTests {
        
        @Test
        @DisplayName("Builder should create valid result")
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
            assertEquals(100, result.getTargetMethodCount());
            assertEquals(2, result.getMatchingCallCount());
        }
        
        @Test
        @DisplayName("Should calculate coverage")
        void testCoverage() {
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
        @DisplayName("Should group by invoke type")
        void testGroupByType() {
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
            AnalysisResult with = AnalysisResult.builder()
                .matchingCalls(Set.of())
                .reflectiveCalls(List.of(new ReflectiveCall(ReflectiveType.CLASS_FOR_NAME, "C", "m", "p", null)))
                .build();
            
            AnalysisResult without = AnalysisResult.builder()
                .matchingCalls(Set.of())
                .reflectiveCalls(List.of())
                .build();
            
            assertTrue(with.hasReflectionWarnings());
            assertFalse(without.hasReflectionWarnings());
        }
    }

    @Nested
    @DisplayName("DiffResult Tests")
    class DiffResultTests {
        
        @Test
        @DisplayName("Should detect JDK mismatch")
        void testJdkMismatch() {
            DiffResult.JarMetadata m1 = new DiffResult.JarMetadata("8", null, null, null, null, null);
            DiffResult.JarMetadata m2 = new DiffResult.JarMetadata("11", null, null, null, null, null);
            
            DiffResult result = new DiffResult("j1", "j2", m1, m2, 100, 100, List.of(), 10, List.of());
            
            assertTrue(result.hasJdkMismatch());
        }
        
        @Test
        @DisplayName("Should not detect mismatch when same")
        void testNoMismatch() {
            DiffResult.JarMetadata m1 = new DiffResult.JarMetadata("11", null, null, null, null, null);
            DiffResult.JarMetadata m2 = new DiffResult.JarMetadata("11", null, null, null, null, null);
            
            DiffResult result = new DiffResult("j1", "j2", m1, m2, 100, 100, List.of(), 10, List.of());
            
            assertFalse(result.hasJdkMismatch());
        }
        
        @Test
        @DisplayName("Should count differences")
        void testCounting() {
            List<DiffResult.ClassDiff> diffs = List.of(
                new DiffResult.ClassDiff("A", DiffResult.DiffType.DIFFERENT, "r", null),
                new DiffResult.ClassDiff("B", DiffResult.DiffType.DIFFERENT, "r", null),
                new DiffResult.ClassDiff("C", DiffResult.DiffType.ONLY_IN_JAR1, "r", null)
            );
            
            DiffResult result = new DiffResult("j1", "j2", 
                DiffResult.JarMetadata.empty(), DiffResult.JarMetadata.empty(),
                100, 99, diffs, 10, List.of());
            
            assertEquals(2, result.getDifferentCount());
            assertEquals(1, result.getOnlyInJar1Count());
            assertFalse(result.areIdentical());
        }
        
        @Test
        @DisplayName("Should be identical when no diffs")
        void testIdentical() {
            DiffResult result = new DiffResult("j1", "j2",
                DiffResult.JarMetadata.empty(), DiffResult.JarMetadata.empty(),
                100, 100, List.of(), 10, List.of());
            
            assertTrue(result.areIdentical());
        }
        
        @Test
        @DisplayName("Should provide recommendation")
        void testRecommendation() {
            DiffResult.JarMetadata m1 = new DiffResult.JarMetadata("8", null, null, null, null, null);
            DiffResult.JarMetadata m2 = new DiffResult.JarMetadata("11", null, null, null, null, null);
            
            DiffResult result = new DiffResult("j1", "j2", m1, m2, 100, 100, List.of(), 10, List.of());
            
            assertTrue(result.getRecommendation().contains("-release"));
        }
    }

    @Nested
    @DisplayName("ClassInfo Tests")
    class ClassInfoTests {
        
        @ParameterizedTest
        @CsvSource({"52,Java 8", "55,Java 11", "61,Java 17", "65,Java 21"})
        void testJavaVersion(int bytecodeVersion, String expected) {
            ClassInfo info = new ClassInfo("Test", "hash", bytecodeVersion, 10, 5, true, true, "Test.java", 1000);
            assertEquals(expected, info.getJavaVersion());
        }
        
        @Test
        @DisplayName("Should detect debug info")
        void testDebugInfo() {
            ClassInfo with = new ClassInfo("T", "h", 52, 10, 5, true, false, "T.java", 100);
            ClassInfo without = new ClassInfo("T", "h", 52, 10, 5, false, false, "T.java", 100);
            
            assertTrue(with.hasDebugInfo());
            assertFalse(without.hasDebugInfo());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Analyzer should handle missing files")
        void testMissingFiles() {
            JarDependencyAnalyzer analyzer = new JarDependencyAnalyzer();
            AnalysisResult result = analyzer.analyze("nonexistent.jar", "also-nonexistent.jar");
            assertTrue(result.hasErrors());
        }
        
        @Test
        @DisplayName("DiffAnalyzer should handle missing files")
        void testDiffMissingFiles() {
            BuildDiffAnalyzer analyzer = new BuildDiffAnalyzer();
            DiffResult result = analyzer.compare("nonexistent.jar", "also-nonexistent.jar");
            assertFalse(result.getWarnings().isEmpty());
        }
        
        @Test
        @DisplayName("Extractors should be injectable")
        void testDI() {
            MethodDefinitionExtractor mockDef = path -> Set.of();
            MethodCallExtractor mockCall = path -> MethodCallExtractor.ExtractionResult.empty();
            
            JarDependencyAnalyzer analyzer = new JarDependencyAnalyzer(mockDef, mockCall);
            assertNotNull(analyzer);
        }
    }
}
