package com.aashish.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Complete result of JAR dependency analysis.
 */
public final class AnalysisResult {
    
    private final String sourceJar;
    private final String targetJar;
    private final int targetMethodCount;
    private final int sourceCallCount;
    private final Set<MethodCall> matchingCalls;
    private final List<ReflectiveCall> reflectiveCalls;
    private final long analysisTimeMs;
    private final List<String> errors;
    
    private AnalysisResult(Builder builder) {
        this.sourceJar = builder.sourceJar;
        this.targetJar = builder.targetJar;
        this.targetMethodCount = builder.targetMethodCount;
        this.sourceCallCount = builder.sourceCallCount;
        this.matchingCalls = Collections.unmodifiableSet(new HashSet<>(builder.matchingCalls));
        this.reflectiveCalls = Collections.unmodifiableList(new ArrayList<>(builder.reflectiveCalls));
        this.analysisTimeMs = builder.analysisTimeMs;
        this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
    }
    
    public String getSourceJar() { return sourceJar; }
    public String getTargetJar() { return targetJar; }
    public int getTargetMethodCount() { return targetMethodCount; }
    public int getSourceCallCount() { return sourceCallCount; }
    public Set<MethodCall> getMatchingCalls() { return matchingCalls; }
    public List<ReflectiveCall> getReflectiveCalls() { return reflectiveCalls; }
    public long getAnalysisTimeMs() { return analysisTimeMs; }
    public List<String> getErrors() { return errors; }
    
    public int getMatchingCallCount() { return matchingCalls.size(); }
    public int getReflectiveCallCount() { return reflectiveCalls.size(); }
    public boolean hasErrors() { return !errors.isEmpty(); }
    
    public double getCoveragePercentage() {
        if (targetMethodCount == 0) return 0.0;
        long uniqueMethodsCalled = matchingCalls.stream()
            .map(MethodCall::toSignature)
            .distinct()
            .count();
        return (uniqueMethodsCalled * 100.0) / targetMethodCount;
    }
    
    public Map<InvokeType, Long> getCallsByInvokeType() {
        return matchingCalls.stream()
            .collect(Collectors.groupingBy(MethodCall::invokeType, Collectors.counting()));
    }
    
    public Map<ReflectiveType, Long> getReflectionByType() {
        return reflectiveCalls.stream()
            .collect(Collectors.groupingBy(ReflectiveCall::type, Collectors.counting()));
    }
    
    public boolean hasReflectionWarnings() {
        return !reflectiveCalls.isEmpty();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private String sourceJar = "";
        private String targetJar = "";
        private int targetMethodCount = 0;
        private int sourceCallCount = 0;
        private Set<MethodCall> matchingCalls = new HashSet<>();
        private List<ReflectiveCall> reflectiveCalls = new ArrayList<>();
        private long analysisTimeMs = 0;
        private List<String> errors = new ArrayList<>();
        
        public Builder sourceJar(String val) { sourceJar = val; return this; }
        public Builder targetJar(String val) { targetJar = val; return this; }
        public Builder targetMethodCount(int val) { targetMethodCount = val; return this; }
        public Builder sourceCallCount(int val) { sourceCallCount = val; return this; }
        public Builder matchingCalls(Set<MethodCall> val) { matchingCalls = val; return this; }
        public Builder reflectiveCalls(List<ReflectiveCall> val) { reflectiveCalls = val; return this; }
        public Builder analysisTimeMs(long val) { analysisTimeMs = val; return this; }
        public Builder errors(List<String> val) { errors = val; return this; }
        public Builder addError(String error) { errors.add(error); return this; }
        
        public AnalysisResult build() {
            return new AnalysisResult(this);
        }
    }
}
