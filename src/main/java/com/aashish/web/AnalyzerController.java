package com.aashish.web;

import com.aashish.analyzer.*;
import com.aashish.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * REST API for bytecode analysis tools.
 * 
 * ENDPOINTS:
 * - POST /api/analyze - Analyze dependencies between two JARs
 * - POST /api/diff    - Compare two JARs for build variability
 * 
 * Both endpoints support file upload via multipart/form-data.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalyzerController {
    
    private final JarDependencyAnalyzer dependencyAnalyzer;
    private final BuildDiffAnalyzer diffAnalyzer;
    
    public AnalyzerController() {
        this.dependencyAnalyzer = new JarDependencyAnalyzer();
        this.diffAnalyzer = new BuildDiffAnalyzer();
    }
    
    /**
     * Analyze dependencies between two JARs.
     * 
     * @param source The source JAR (containing calls)
     * @param target The target JAR (containing definitions)
     * @param bidirectional If true, analyze both directions
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeDependencies(
            @RequestParam("source") MultipartFile source,
            @RequestParam("target") MultipartFile target,
            @RequestParam(value = "bidirectional", defaultValue = "true") boolean bidirectional) {
        
        Path sourcePath = null;
        Path targetPath = null;
        
        try {
            sourcePath = saveToTemp(source);
            targetPath = saveToTemp(target);
            
            Map<String, Object> response = new LinkedHashMap<>();
            
            if (bidirectional) {
                var result = dependencyAnalyzer.analyzeBidirectional(
                    sourcePath.toString(), 
                    targetPath.toString()
                );
                response.put("direction1", formatResult(result.forward()));
                response.put("direction2", formatResult(result.reverse()));
                response.put("summary", result.getSummary());
            } else {
                var result = dependencyAnalyzer.analyze(
                    sourcePath.toString(),
                    targetPath.toString()
                );
                response.put("result", formatResult(result));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } finally {
            cleanup(sourcePath, targetPath);
        }
    }
    
    /**
     * Compare two JARs for build variability.
     */
    @PostMapping("/diff")
    public ResponseEntity<?> compareBuild(
            @RequestParam("jar1") MultipartFile jar1,
            @RequestParam("jar2") MultipartFile jar2) {
        
        Path jar1Path = null;
        Path jar2Path = null;
        
        try {
            jar1Path = saveToTemp(jar1);
            jar2Path = saveToTemp(jar2);
            
            DiffResult result = diffAnalyzer.compare(
                jar1Path.toString(),
                jar2Path.toString()
            );
            
            return ResponseEntity.ok(formatDiffResult(result));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } finally {
            cleanup(jar1Path, jar2Path);
        }
    }
    
    private Path saveToTemp(MultipartFile file) throws IOException {
        Path temp = Files.createTempFile("analysis_", ".jar");
        file.transferTo(temp);
        return temp;
    }
    
    private void cleanup(Path... paths) {
        for (Path p : paths) {
            if (p != null) {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            }
        }
    }
    
    private Map<String, Object> formatResult(AnalysisResult r) {
        Map<String, Object> map = new LinkedHashMap<>();
        
        map.put("sourceJar", extractFileName(r.getSourceJar()));
        map.put("targetJar", extractFileName(r.getTargetJar()));
        map.put("targetMethodCount", r.getTargetMethodCount());
        map.put("sourceCallCount", r.getSourceCallCount());
        map.put("matchingCallCount", r.getMatchingCallCount());
        map.put("coveragePercentage", String.format("%.1f%%", r.getCoveragePercentage()));
        map.put("analysisTimeMs", r.getAnalysisTimeMs());
        
        // Calls by type
        Map<String, Long> byType = new LinkedHashMap<>();
        r.getCallsByInvokeType().forEach((k, v) -> byType.put(k.name(), v));
        map.put("callsByType", byType);
        
        // Sample calls
        List<String> samples = r.getMatchingCalls().stream()
            .limit(25)
            .map(MethodCall::toReadableString)
            .toList();
        map.put("sampleCalls", samples);
        
        // Reflection warnings
        if (r.hasReflectionWarnings()) {
            map.put("reflectionWarning", true);
            map.put("reflectionCount", r.getReflectiveCallCount());
            
            List<Map<String, String>> reflections = r.getReflectiveCalls().stream()
                .limit(20)
                .map(rc -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("type", rc.type().name());
                    m.put("pattern", rc.pattern());
                    m.put("location", rc.callerClass() + "." + rc.callerMethod());
                    m.put("target", rc.potentialTarget() != null ? rc.potentialTarget() : "unknown");
                    return m;
                })
                .toList();
            map.put("reflectiveCalls", reflections);
        }
        
        return map;
    }
    
    private Map<String, Object> formatDiffResult(DiffResult r) {
        Map<String, Object> map = new LinkedHashMap<>();
        
        map.put("jar1", extractFileName(r.getJar1Path()));
        map.put("jar2", extractFileName(r.getJar2Path()));
        map.put("analysisTimeMs", r.getAnalysisTimeMs());
        
        // Metadata
        map.put("jar1Metadata", Map.of(
            "buildJdk", nvl(r.getJar1Metadata().buildJdk()),
            "createdBy", nvl(r.getJar1Metadata().createdBy())
        ));
        map.put("jar2Metadata", Map.of(
            "buildJdk", nvl(r.getJar2Metadata().buildJdk()),
            "createdBy", nvl(r.getJar2Metadata().createdBy())
        ));
        
        // Counts
        map.put("jar1ClassCount", r.getJar1ClassCount());
        map.put("jar2ClassCount", r.getJar2ClassCount());
        map.put("identicalCount", r.getIdenticalCount());
        map.put("differentCount", r.getDifferentCount());
        map.put("onlyInJar1", r.getOnlyInJar1Count());
        map.put("onlyInJar2", r.getOnlyInJar2Count());
        map.put("areIdentical", r.areIdentical());
        
        // Warnings
        if (r.hasJdkMismatch()) {
            map.put("jdkMismatchWarning", true);
        }
        map.put("recommendation", r.getRecommendation());
        
        // Differences
        List<Map<String, String>> diffs = r.getDifferences().stream()
            .filter(d -> d.type() == DiffResult.DiffType.DIFFERENT)
            .limit(30)
            .map(d -> Map.of(
                "class", d.className().replace("/", "."),
                "reason", d.reason(),
                "details", d.details() != null ? d.details() : ""
            ))
            .toList();
        map.put("differences", diffs);
        
        return map;
    }
    
    private String extractFileName(String path) {
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
    
    private String nvl(String s) {
        return s != null ? s : "unknown";
    }
}
