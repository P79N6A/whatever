package org.springframework.format;

import org.springframework.core.convert.converter.ConverterRegistry;

import java.lang.annotation.Annotation;

public interface FormatterRegistry extends ConverterRegistry {

    void addFormatter(Formatter<?> formatter);

    void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter);

    void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser);

    void addFormatterForFieldAnnotation(AnnotationFormatterFactory<? extends Annotation> annotationFormatterFactory);

}
