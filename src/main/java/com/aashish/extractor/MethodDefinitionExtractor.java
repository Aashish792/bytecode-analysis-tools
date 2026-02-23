package com.aashish.extractor;

import com.aashish.model.MethodSignature;
import java.io.IOException;
import java.util.Set;

/**
 * Interface for extracting method definitions from JAR files.
 */
public interface MethodDefinitionExtractor {
    Set<MethodSignature> extract(String jarPath) throws IOException;
}
