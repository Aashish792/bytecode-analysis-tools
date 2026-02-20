package com.aashish.extractor;

import com.aashish.model.*;
import java.io.IOException;
import java.util.*;

/**
 * Interface for extracting method calls from JAR files.
 * 
 * Implementations should detect:
 * 1. All 5 JVM invoke types (virtual, static, special, interface, dynamic)
 * 2. Reflective call patterns (Class.forName, getMethod, invoke)
 */
public interface MethodCallExtractor {
    
    /**
     * Result container for extraction.
     * Separates direct calls from reflective patterns.
     */
    record ExtractionResult(
        Set<MethodCall> directCalls,
        List<ReflectiveCall> reflectiveCalls
    ) {
        public ExtractionResult {
            directCalls = Collections.unmodifiableSet(new HashSet<>(directCalls));
            reflectiveCalls = Collections.unmodifiableList(new ArrayList<>(reflectiveCalls));
        }
    }
    
    /**
     * Extracts all method calls and reflection patterns from a JAR.
     * 
     * @param jarPath Path to the JAR file
     * @return Extraction result with direct calls and reflective patterns
     * @throws IOException If JAR cannot be read
     */
    ExtractionResult extract(String jarPath) throws IOException;
}
