package com.aashish.extractor;

import com.aashish.model.*;
import java.io.IOException;
import java.util.*;

/**
 * Interface for extracting method calls from JAR files.
 */
public interface MethodCallExtractor {
    
    record ExtractionResult(
        Set<MethodCall> directCalls,
        List<ReflectiveCall> reflectiveCalls,
        List<String> errors
    ) {
        public ExtractionResult {
            directCalls = Collections.unmodifiableSet(new HashSet<>(directCalls));
            reflectiveCalls = Collections.unmodifiableList(new ArrayList<>(reflectiveCalls));
            errors = Collections.unmodifiableList(new ArrayList<>(errors));
        }
        
        public static ExtractionResult empty() {
            return new ExtractionResult(Set.of(), List.of(), List.of());
        }
    }
    
    ExtractionResult extract(String jarPath) throws IOException;
}
