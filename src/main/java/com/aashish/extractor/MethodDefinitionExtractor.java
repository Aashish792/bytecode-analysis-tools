package com.aashish.extractor;

import com.aashish.model.*;
import java.io.IOException;
import java.util.*;

/**
 * Interface for extracting method definitions from JAR files.
 * 
 * DESIGN PRINCIPLE: Interface Segregation
 * - Separate interface for definitions vs calls
 * - Allows different implementations (ASM, BCEL, etc.)
 * - Enables easy testing with mock implementations
 */
public interface MethodDefinitionExtractor {
    
    /**
     * Extracts all public/protected method definitions from a JAR.
     * 
     * @param jarPath Path to the JAR file
     * @return Set of method signatures (owner, name, descriptor)
     * @throws IOException If JAR cannot be read
     */
    Set<MethodSignature> extract(String jarPath) throws IOException;
}
