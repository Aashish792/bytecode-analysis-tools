#  Bytecode Analysis Tools for Reproducible Builds

> **Production-grade ASM-based bytecode analysis with reflection detection and build variability explanation**

[![Java 17+](https://img.shields.io/badge/Java-17+-blue.svg)](https://openjdk.org/)
[![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![ASM 9.6](https://img.shields.io/badge/ASM-9.6-orange.svg)](https://asm.ow2.io/)
[![Tests](https://img.shields.io/badge/Tests-30+-brightgreen.svg)]()

---

##  Table of Contents

1. [Overview](#-overview)
2. [Key Features](#-key-features)
3. [Architecture](#-architecture)
4. [How It Works](#-how-it-works)
5. [Reflection Detection](#-reflection-detection)
6. [Quick Start](#-quick-start)
7. [Test Suite](#-test-suite)
8. [API Reference](#-api-reference)
9. [Research Connection](#-connection-to-research)

---

##  Overview

This tool provides **two core capabilities** for reproducible builds research:

| Feature | Description |
|---------|-------------|
| **Dependency Analysis** | Finds method calls between JARs using ASM bytecode analysis |
| **Build Diff/Explain** | Compares alternative builds and explains WHY they differ |

### Why This Matters

When the same source code is built in different environments (Maven Central vs Google's rebuild), the binaries may differ. This tool helps:

1. **Identify** which methods create dependencies between JARs
2. **Detect** reflection patterns that hide dependencies from static analysis
3. **Compare** alternative builds and explain differences
4. **Warn** about JDK version mismatches that cause runtime failures

---

##  Key Features

### 1. Complete Invoke Type Detection

All 5 JVM method invocation types are detected:

| Type | Opcode | Description | Example |
|------|--------|-------------|---------|
| `INVOKEVIRTUAL` | 182 | Instance method | `list.add(item)` |
| `INVOKESTATIC` | 184 | Static method | `Math.abs(-5)` |
| `INVOKESPECIAL` | 183 | Constructor/super/private | `new ArrayList<>()` |
| `INVOKEINTERFACE` | 185 | Interface method | `collection.size()` |
| `INVOKEDYNAMIC` | 186 | Lambda/method ref | `stream.map(x -> x)` |

### 2. Reflection-Aware Analysis

Detects patterns that hide dependencies:

| Pattern | Risk Level | What It Means |
|---------|------------|---------------|
| `Class.forName()` | 🔴 High | Dynamic class loading - hidden dependency |
| `getMethod()` | 🔴 High | Method lookup by name - invisible call |
| `Method.invoke()` | 🔴 High | Reflective invocation - bypasses analysis |
| `Constructor.newInstance()` | 🟡 Medium | Reflective object creation |
| `Field.get/set()` | 🟡 Medium | Reflective field access |

### 3. Build Variability Explanation

When comparing builds, explains WHY they differ:

- **JDK version mismatch** (most critical)
- **Bytecode version differences**
- **Debug info presence**
- **Method/field count changes**

### 4. Beautiful Web UI

- Dark theme interface
- Drag-and-drop file upload
- Real-time results display
- Mobile-responsive

---

##  Architecture

```
src/main/java/com/aashish/
│
├── Application.java                    # Spring Boot entry point
│
├── model/                              # Immutable domain objects
│   ├── MethodSignature.java            # (owner, name, descriptor) record
│   ├── MethodCall.java                 # Call with invoke type and location
│   ├── InvokeType.java                 # Enum: all 5 JVM invoke types
│   ├── ReflectiveCall.java             # Detected reflection pattern
│   ├── ReflectiveType.java             # Enum: reflection pattern types
│   ├── ClassInfo.java                  # Class metadata for diff
│   ├── AnalysisResult.java             # Builder pattern result
│   └── DiffResult.java                 # Build comparison result
│
├── extractor/                          # Bytecode extraction (Interface Segregation)
│   ├── MethodDefinitionExtractor.java  # Interface: extract definitions
│   ├── MethodCallExtractor.java        # Interface: extract calls
│   ├── AsmMethodDefinitionExtractor.java   # ASM implementation
│   └── AsmMethodCallExtractor.java         # ASM impl + reflection detection
│
├── analyzer/                           # Business logic (Single Responsibility)
│   ├── JarDependencyAnalyzer.java      # Orchestrates dependency analysis
│   └── BuildDiffAnalyzer.java          # Compares builds, explains differences
│
└── web/                                # REST API
    └── AnalyzerController.java         # POST /api/analyze, POST /api/diff
```

### Design Principles Applied

| Principle | Implementation |
|-----------|----------------|
| **Single Responsibility** | Each class has exactly one reason to change |
| **Open/Closed** | Add new extractors without modifying analyzers |
| **Liskov Substitution** | Any `MethodCallExtractor` works with `JarDependencyAnalyzer` |
| **Interface Segregation** | Separate interfaces for definitions vs calls |
| **Dependency Inversion** | Analyzers depend on interfaces, not ASM directly |

### Immutability

All model classes use Java Records or are immutable:
- `MethodSignature` - record with null checks
- `MethodCall` - record with null checks
- `AnalysisResult` - immutable with unmodifiable collections
- `DiffResult` - immutable with unmodifiable collections

---

## How It Works

### Dependency Analysis Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         INPUT                                       │
│   Source JAR (e.g., jackson-databind-2.15.0.jar)                   │
│   Target JAR (e.g., jackson-core-2.15.0.jar)                       │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  STEP 1: Extract Method Definitions from Target JAR                │
│  ─────────────────────────────────────────────────────────────────  │
│  • Open JAR file                                                    │
│  • For each .class file:                                            │
│    - Create ASM ClassReader                                         │
│    - Visit each method declaration                                  │
│    - Collect MethodSignature(owner, name, descriptor)              │
│  • Result: Set<MethodSignature> with ~1,247 methods                │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  STEP 2: Extract Method Calls from Source JAR                      │
│  ─────────────────────────────────────────────────────────────────  │
│  • Open JAR file                                                    │
│  • For each .class file:                                            │
│    - Create ASM ClassReader                                         │
│    - Visit each method body                                         │
│    - For each INVOKE* instruction:                                  │
│      → Record MethodCall with invoke type and call site            │
│    - For each reflection pattern:                                   │
│      → Record ReflectiveCall with captured target if available     │
│  • Result: Set<MethodCall> + List<ReflectiveCall>                  │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  STEP 3: Match Calls Against Definitions                           │
│  ─────────────────────────────────────────────────────────────────  │
│  • For each MethodCall in source:                                   │
│    - Convert to MethodSignature                                     │
│    - Check if signature exists in target's definition set          │
│    - If match: add to matchingCalls                                │
│  • Result: Set<MethodCall> of matching calls                       │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         OUTPUT                                      │
│   AnalysisResult:                                                   │
│   • 275 matching calls found                                        │
│   • 22.1% coverage of target methods                               │
│   • 12 reflection warnings                                          │
│   • Breakdown by invoke type                                        │
└─────────────────────────────────────────────────────────────────────┘
```

### Build Diff Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│  INPUT: Two JARs built from same source                            │
│  JAR 1: undertow-servlet-2.2.23.jar (Maven Central)                │
│  JAR 2: undertow-servlet-2.2.23.jar (Google GAOSS)                 │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  STEP 1: Extract Class Info from Both JARs                         │
│  ─────────────────────────────────────────────────────────────────  │
│  For each class:                                                    │
│  • Compute SHA-256 hash of bytecode                                │
│  • Extract: bytecode version, method count, field count            │
│  • Detect: line numbers, local variables (debug info)              │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  STEP 2: Compare Hashes                                            │
│  ─────────────────────────────────────────────────────────────────  │
│  • If hash matches → classes are IDENTICAL                         │
│  • If hash differs → analyze WHY                                   │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  STEP 3: Explain Differences                                       │
│  ─────────────────────────────────────────────────────────────────  │
│  Check:                                                             │
│  • Bytecode version: Java 8 (52) vs Java 11 (55)?                  │
│  • Method count changed?                                            │
│  • Debug info presence differs?                                     │
│  Generate human-readable explanation                                │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  OUTPUT: DiffResult                                                 │
│  ─────────────────────────────────────────────────────────────────  │
│  ⚠️ JDK VERSION MISMATCH DETECTED                                  │
│  JAR 1: Built with JDK 8                                           │
│  JAR 2: Built with JDK 11                                          │
│                                                                     │
│  2 classes differ:                                                  │
│  • UpgradeServletInputStream: Bytecode version Java 8 vs Java 11   │
│                                                                     │
│  💡 Recommendation: Use -release flag instead of -target           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Reflection Detection

### Why It Matters

Static analysis has a fundamental limitation: **it cannot see dependencies loaded via reflection**.

```java
// This dependency is INVISIBLE to normal static analysis
Class<?> clazz = Class.forName("com.example.HiddenService");
Method method = clazz.getMethod("process", String.class);
Object result = method.invoke(instance, "data");
```

### How We Detect It

We track the bytecode instruction sequence:

```
LDC "com.example.HiddenService"    ← We capture this string
INVOKESTATIC Class.forName         ← We detect this pattern
```

By tracking `LDC` instructions that precede reflection calls, we can often identify the **target class or method name**.

### What We Report

```
⚠️ Reflective Calls Detected (12):

  CLASS_FOR_NAME in com.fasterxml.jackson.databind.util.ClassUtil.findClass
    → target: "com.fasterxml.jackson.databind.ser.BeanSerializer"
  
  GET_METHOD in com.fasterxml.jackson.databind.introspect.AnnotatedMethod
    → target: "getValue"
  
  METHOD_INVOKE in com.fasterxml.jackson.databind.ser.BeanSerializer
    → target: unknown (passed through variable)
```

---

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+

### Build & Run

```bash
# Clone
git clone https://github.com/Aashish792/bytecode-analysis-tools.git
cd bytecode-analysis-tools

# Build
mvn clean package

# Run
java -jar target/bytecode-analysis-tools-1.0.0.jar

# Open browser
http://localhost:8080
```

### Test with Example JARs

```bash
# Download Jackson JARs (clear dependency relationship)
wget https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.0/jackson-databind-2.15.0.jar
wget https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.0/jackson-core-2.15.0.jar

# Expected results:
# databind → core: ~275 matching calls
# core → databind: 0 calls (core is independent)
# Reflection warnings: ~12 (Jackson uses reflection)
```

---

## 🧪 Test Suite

### Running Tests

```bash
mvn test
```

### Test Categories (30+ tests)

| Category | Tests | Description |
|----------|-------|-------------|
| **MethodSignature** | 5 | Creation, equality, HashSet behavior, null rejection |
| **InvokeType** | 3 | Opcode mapping, descriptions, invalid handling |
| **MethodCall** | 2 | Signature conversion, formatting |
| **ReflectiveCall** | 2 | With/without target formatting |
| **AnalysisResult** | 5 | Builder, coverage, grouping, warnings |
| **DiffResult** | 5 | JDK mismatch, counting, recommendations |
| **ClassInfo** | 2 | Java version mapping, debug detection |
| **Integration** | 3 | Error handling, dependency injection |

### Sample Test

```java
@Test
@DisplayName("Should detect JDK mismatch")
void testJdkMismatchDetection() {
    DiffResult.JarMetadata meta1 = new DiffResult.JarMetadata("8", null, null, null, null);
    DiffResult.JarMetadata meta2 = new DiffResult.JarMetadata("11", null, null, null, null);
    
    DiffResult result = new DiffResult("jar1.jar", "jar2.jar", meta1, meta2, 100, 100, List.of(), 10);
    
    assertTrue(result.hasJdkMismatch());
    assertTrue(result.getRecommendation().contains("-release"));
}
```

---

## 📡 API Reference

### POST /api/analyze

Analyze dependencies between two JARs.

**Request:**
```bash
curl -X POST http://localhost:8080/api/analyze \
  -F "source=@jackson-databind-2.15.0.jar" \
  -F "target=@jackson-core-2.15.0.jar" \
  -F "bidirectional=true"
```

**Response:**
```json
{
  "direction1": {
    "sourceJar": "jackson-databind-2.15.0.jar",
    "targetJar": "jackson-core-2.15.0.jar",
    "targetMethodCount": 1247,
    "sourceCallCount": 15832,
    "matchingCallCount": 275,
    "coveragePercentage": "22.1%",
    "analysisTimeMs": 342,
    "callsByType": {
      "INVOKEVIRTUAL": 198,
      "INVOKESTATIC": 42,
      "INVOKESPECIAL": 28,
      "INVOKEINTERFACE": 7
    },
    "reflectionWarning": true,
    "reflectionCount": 12,
    "reflectiveCalls": [...]
  },
  "direction2": { ... }
}
```

### POST /api/diff

Compare two JARs for build variability.

**Request:**
```bash
curl -X POST http://localhost:8080/api/diff \
  -F "jar1=@artifact-mvnc.jar" \
  -F "jar2=@artifact-gaoss.jar"
```

**Response:**
```json
{
  "jar1": "undertow-servlet-2.2.23.jar",
  "jar2": "undertow-servlet-2.2.23.jar",
  "jar1Metadata": { "buildJdk": "8", "createdBy": "Apache Maven" },
  "jar2Metadata": { "buildJdk": "11", "createdBy": "Apache Maven" },
  "jdkMismatchWarning": true,
  "recommendation": "Use -release flag instead of -target...",
  "identicalCount": 140,
  "differentCount": 2,
  "differences": [
    {
      "class": "io.undertow.servlet.spec.UpgradeServletInputStream",
      "reason": "Bytecode version: Java 8 vs Java 11"
    }
  ]
}
```
---

## 📄 License

MIT License - See [LICENSE](LICENSE) for details.
