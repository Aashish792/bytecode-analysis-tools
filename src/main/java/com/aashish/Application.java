package com.aashish;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    
    public static void main(String[] args) {
        System.out.println("""
            
            ╔══════════════════════════════════════════════════════════════╗
            ║         BYTECODE ANALYSIS TOOLS                             ║
            ║                   by Aashish K C                             ║
            ╠══════════════════════════════════════════════════════════════╣
            ║  Web UI:  http://localhost:8080                             ║
            ║  API:     POST /api/analyze                                 ║
            ║           POST /api/diff                                    ║
            ║           POST /api/scan                                    ║
            ╚══════════════════════════════════════════════════════════════╝
            """);
        
        SpringApplication.run(Application.class, args);
    }
}
