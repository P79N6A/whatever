package org.springframework.boot.diagnostics.analyzer;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.ClassUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collections;
import java.util.List;

class NoSuchMethodFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchMethodError> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, NoSuchMethodError cause) {
        String className = extractClassName(cause);
        if (className == null) {
            return null;
        }
        List<URL> candidates = findCandidates(className);
        if (candidates == null) {
            return null;
        }
        URL actual = getActual(className);
        if (actual == null) {
            return null;
        }
        String description = getDescription(cause, className, candidates, actual);
        return new FailureAnalysis(description, "Correct the classpath of your application so that it contains a single," + " compatible version of " + className, cause);
    }

    private String extractClassName(NoSuchMethodError cause) {
        int descriptorIndex = cause.getMessage().indexOf('(');
        if (descriptorIndex == -1) {
            return null;
        }
        String classAndMethodName = cause.getMessage().substring(0, descriptorIndex);
        int methodNameIndex = classAndMethodName.lastIndexOf('.');
        if (methodNameIndex == -1) {
            return null;
        }
        return classAndMethodName.substring(0, methodNameIndex);
    }

    private List<URL> findCandidates(String className) {
        try {
            return Collections.list(NoSuchMethodFailureAnalyzer.class.getClassLoader().getResources(ClassUtils.convertClassNameToResourcePath(className) + ".class"));
        } catch (Throwable ex) {
            return null;
        }
    }

    private URL getActual(String className) {
        try {
            return getClass().getClassLoader().loadClass(className).getProtectionDomain().getCodeSource().getLocation();
        } catch (Throwable ex) {
            return null;
        }
    }

    private String getDescription(NoSuchMethodError cause, String className, List<URL> candidates, URL actual) {
        StringWriter description = new StringWriter();
        PrintWriter writer = new PrintWriter(description);
        writer.println("An attempt was made to call a method that does not" + " exist. The attempt was made from the following location:");
        writer.println();
        writer.print("    ");
        writer.println(cause.getStackTrace()[0]);
        writer.println();
        writer.println("The following method did not exist:");
        writer.println();
        writer.print("    ");
        writer.println(cause.getMessage());
        writer.println();
        writer.println("The method's class, " + className + ", is available from the following locations:");
        writer.println();
        for (URL candidate : candidates) {
            writer.print("    ");
            writer.println(candidate);
        }
        writer.println();
        writer.println("It was loaded from the following location:");
        writer.println();
        writer.print("    ");
        writer.println(actual);
        return description.toString();
    }

}
