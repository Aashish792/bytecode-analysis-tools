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
│   ├── MethodSignature.java
│   ├── MethodCall.java
│   ├── InvokeType.java
│   ├── ReflectiveCall.java
│   ├── ReflectiveType.java
│   ├── ClassInfo.java
│   ├── AnalysisResult.java
│   └── DiffResult.java
│
├── extractor/
│   ├── MethodDefinitionExtractor.java
│   ├── MethodCallExtractor.java
│   ├── AsmMethodDefinitionExtractor.java
│   └── AsmMethodCallExtractor.java
│
├── analyzer/
│   ├── JarDependencyAnalyzer.java
│   └── BuildDiffAnalyzer.java
│
└── web/
    └── AnalyzerController.java
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
mvn clean package -DskipTests

# Run
java -jar target/bytecode-analysis-tools-1.0.0.jar

# Open
http://localhost:8080
```

---

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

## Test Suite

### Running Tests

```bash
mvn test
```

---

## API Reference

### POST /api/analyze

```bash
curl -X POST http://localhost:8080/api/analyze \
  -F "source=@jackson-databind-2.15.0.jar" \
  -F "target=@jackson-core-2.15.0.jar" \
  -F "bidirectional=true"
```

---

### POST /api/diff

```bash
curl -X POST http://localhost:8080/api/diff \
  -F "jar1=@artifact-mvnc.jar" \
  -F "jar2=@artifact-gaoss.jar"
```

---

## License

MIT License - See [LICENSE](LICENSE) for details.