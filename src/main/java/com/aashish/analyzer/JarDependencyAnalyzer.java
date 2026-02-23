package com.aashish.analyzer;

import com.aashish.extractor.*;
import com.aashish.model.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for JAR bytecode dependency analysis.
 */
public class JarDependencyAnalyzer {
    
    private final MethodDefinitionExtractor definitionExtractor;
    private final MethodCallExtractor callExtractor;
    
    public JarDependencyAnalyzer() {
        this.definitionExtractor = new AsmMethodDefinitionExtractor();
        this.callExtractor = new AsmMethodCallExtractor();
    }
    
    public JarDependencyAnalyzer(MethodDefinitionExtractor defExt, MethodCallExtractor callExt) {
        this.definitionExtractor = Objects.requireNonNull(defExt);
        this.callExtractor = Objects.requireNonNull(callExt);
    }
    
    public AnalysisResult analyze(String sourceJarPath, String targetJarPath) {
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        
        try {
            // Extract method definitions from target JAR
            Set<MethodSignature> targetMethods = definitionExtractor.extract(targetJarPath);
            
            // Extract method calls from source JAR
            MethodCallExtractor.ExtractionResult extraction = callExtractor.extract(sourceJarPath);
            errors.addAll(extraction.errors());
            
            // Match calls against definitions
            Set<MethodCall> matchingCalls = extraction.directCalls().stream()
                .filter(call -> targetMethods.contains(call.toSignature()))
                .collect(Collectors.toSet());
            
            long endTime = System.currentTimeMillis();
            
            return AnalysisResult.builder()
                .sourceJar(sourceJarPath)
                .targetJar(targetJarPath)
                .targetMethodCount(targetMethods.size())
                .sourceCallCount(extraction.directCalls().size())
                .matchingCalls(matchingCalls)
                .reflectiveCalls(extraction.reflectiveCalls())
                .analysisTimeMs(endTime - startTime)
                .errors(errors)
                .build();
                
        } catch (IOException e) {
            return AnalysisResult.builder()
                .sourceJar(sourceJarPath)
                .targetJar(targetJarPath)
                .analysisTimeMs(System.currentTimeMillis() - startTime)
                .addError("Failed to analyze: " + e.getMessage())
                .build();
        }
    }
    
    public BidirectionalResult analyzeBidirectional(String jar1Path, String jar2Path) {
        AnalysisResult forward = analyze(jar1Path, jar2Path);
        AnalysisResult reverse = analyze(jar2Path, jar1Path);
        return new BidirectionalResult(forward, reverse);
    }
    
    public record BidirectionalResult(
        AnalysisResult forward,
        AnalysisResult reverse
    ) {
        public String getSummary() {
            return String.format("%s -> %s: %d calls | %s -> %s: %d calls",
                extractFileName(forward.getSourceJar()),
                extractFileName(forward.getTargetJar()),
                forward.getMatchingCallCount(),
                extractFileName(reverse.getSourceJar()),
                extractFileName(reverse.getTargetJar()),
                reverse.getMatchingCallCount()
            );
        }
        
        private String extractFileName(String path) {
            if (path == null) return "unknown";
            int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        }
    }
}
