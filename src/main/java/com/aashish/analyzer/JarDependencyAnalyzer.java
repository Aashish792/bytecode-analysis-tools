package com.aashish.analyzer;

import com.aashish.extractor.*;
import com.aashish.model.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for JAR bytecode dependency analysis.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * WHAT IT DOES
 * ═══════════════════════════════════════════════════════════════════════════
 * Given two JAR files (source and target), finds which methods in the source
 * JAR call methods defined in the target JAR.
 * 
 * Example: jackson-databind (source) → jackson-core (target)
 * Result: 275 method calls found (databind uses core's JsonParser, etc.)
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * HOW IT WORKS
 * ═══════════════════════════════════════════════════════════════════════════
 * 1. EXTRACT DEFINITIONS: Scan target JAR, collect all method signatures
 * 2. EXTRACT CALLS: Scan source JAR, collect all method call instructions
 * 3. MATCH: Compare call signatures against definition signatures
 * 4. DETECT REFLECTION: Identify patterns like Class.forName(), Method.invoke()
 * 5. BUILD RESULT: Aggregate statistics and warnings
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * DESIGN PRINCIPLES
 * ═══════════════════════════════════════════════════════════════════════════
 * - Dependency Inversion: Depends on interfaces, not ASM directly
 * - Single Responsibility: Only orchestrates analysis, doesn't extract
 * - Open/Closed: Add new extractors without modifying this class
 * 
 * @author Aashish K C
 */
public class JarDependencyAnalyzer {
    
    private final MethodDefinitionExtractor definitionExtractor;
    private final MethodCallExtractor callExtractor;
    
    /**
     * Creates analyzer with default ASM-based extractors.
     */
    public JarDependencyAnalyzer() {
        this.definitionExtractor = new AsmMethodDefinitionExtractor();
        this.callExtractor = new AsmMethodCallExtractor();
    }
    
    /**
     * Creates analyzer with custom extractors.
     * Useful for testing or alternative implementations.
     */
    public JarDependencyAnalyzer(MethodDefinitionExtractor definitionExtractor,
                                  MethodCallExtractor callExtractor) {
        this.definitionExtractor = Objects.requireNonNull(definitionExtractor);
        this.callExtractor = Objects.requireNonNull(callExtractor);
    }
    
    /**
     * Analyzes dependencies from source JAR to target JAR.
     * 
     * @param sourceJarPath JAR containing the calls (the "client")
     * @param targetJarPath JAR containing the definitions (the "library")
     * @return Complete analysis result
     * @throws IOException If JARs cannot be read
     */
    public AnalysisResult analyze(String sourceJarPath, String targetJarPath) throws IOException {
        long startTime = System.currentTimeMillis();
        
        // Step 1: Extract method definitions from target JAR
        Set<MethodSignature> targetMethods = definitionExtractor.extract(targetJarPath);
        
        // Step 2: Extract method calls and reflection from source JAR
        MethodCallExtractor.ExtractionResult extraction = callExtractor.extract(sourceJarPath);
        
        // Step 3: Match calls against definitions
        Set<MethodCall> matchingCalls = findMatchingCalls(
            extraction.directCalls(),
            targetMethods
        );
        
        long endTime = System.currentTimeMillis();
        
        // Step 4: Build result
        return AnalysisResult.builder()
            .sourceJar(sourceJarPath)
            .targetJar(targetJarPath)
            .targetMethodCount(targetMethods.size())
            .sourceCallCount(extraction.directCalls().size())
            .matchingCalls(matchingCalls)
            .reflectiveCalls(extraction.reflectiveCalls())
            .analysisTimeMs(endTime - startTime)
            .build();
    }
    
    /**
     * Performs bidirectional analysis (A→B and B→A).
     * 
     * WHY BIDIRECTIONAL?
     * Users often don't know which JAR is the "caller" and which is the "library".
     * By analyzing both directions, we provide complete information.
     * 
     * Example: Given jackson-databind and jackson-core:
     * - databind → core: 275 calls (databind uses core)
     * - core → databind: 0 calls (core is independent)
     */
    public BidirectionalResult analyzeBidirectional(String jar1Path, String jar2Path) 
            throws IOException {
        AnalysisResult forward = analyze(jar1Path, jar2Path);
        AnalysisResult reverse = analyze(jar2Path, jar1Path);
        return new BidirectionalResult(forward, reverse);
    }
    
    private Set<MethodCall> findMatchingCalls(Set<MethodCall> calls, 
                                               Set<MethodSignature> definitions) {
        return calls.stream()
            .filter(call -> definitions.contains(call.toSignature()))
            .collect(Collectors.toSet());
    }
    
    /**
     * Result of bidirectional analysis.
     */
    public record BidirectionalResult(
        AnalysisResult forward,
        AnalysisResult reverse
    ) {
        public String getSummary() {
            return String.format(
                "%s → %s: %d calls%n%s → %s: %d calls",
                extractFileName(forward.getSourceJar()),
                extractFileName(forward.getTargetJar()),
                forward.getMatchingCallCount(),
                extractFileName(reverse.getSourceJar()),
                extractFileName(reverse.getTargetJar()),
                reverse.getMatchingCallCount()
            );
        }
        
        private String extractFileName(String path) {
            int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        }
    }
}
