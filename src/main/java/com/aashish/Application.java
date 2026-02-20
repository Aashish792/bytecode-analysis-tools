package com.aashish;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bytecode Analysis Tools for Reproducible Builds Research
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * FEATURES
 * ═══════════════════════════════════════════════════════════════════════════
 * 1. JAR Dependency Analysis
 *    - Finds method calls between JARs
 *    - Detects all 5 JVM invoke types
 *    - Identifies reflection patterns
 * 
 * 2. Build Variability Analysis
 *    - Compares JARs built from same source
 *    - Explains WHY they differ
 *    - Provides actionable recommendations
 * 
 * 3. Web UI
 *    - Beautiful dark theme interface
 *    - Drag-and-drop file upload
 *    - Real-time results display
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * QUICK START
 * ═══════════════════════════════════════════════════════════════════════════
 * mvn clean package -DskipTests
 * java -jar target/bytecode-analysis-tools-1.0.0.jar
 * Open: http://localhost:8080
 * 
 * @author Aashish K C
 */
@SpringBootApplication
public class Application {
    
    public static void main(String[] args) {
        printBanner();
        SpringApplication.run(Application.class, args);
    }
    
    private static void printBanner() {
        System.out.println("""
            
            ╔═══════════════════════════════════════════════════════════════════╗
            ║     BYTECODE ANALYSIS TOOLS FOR REPRODUCIBLE BUILDS              ║
            ║                        by Aashish K C                             ║
            ╠═══════════════════════════════════════════════════════════════════╣
            ║                                                                   ║
            ║  Features:                                                        ║
            ║    • JAR dependency analysis with reflection detection            ║
            ║    • Build variability analysis (diff/explain)                    ║
            ║    • Beautiful web UI                                             ║
            ║                                                                   ║
            ╠═══════════════════════════════════════════════════════════════════╣
            ║  Web UI:  http://localhost:8080                                  ║
            ║  API:     POST /api/analyze                                      ║
            ║           POST /api/diff                                         ║
            ╚═══════════════════════════════════════════════════════════════════╝
            """);
    }
}
