package org.springframework.boot.diagnostics;

@FunctionalInterface
public interface FailureAnalyzer {

    FailureAnalysis analyze(Throwable failure);

}
