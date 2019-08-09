package org.springframework.format.number;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.annotation.NumberFormat.Style;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

import java.util.Set;

public class NumberFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport implements AnnotationFormatterFactory<NumberFormat> {

    @Override
    public Set<Class<?>> getFieldTypes() {
        return NumberUtils.STANDARD_NUMBER_TYPES;
    }

    @Override
    public Printer<Number> getPrinter(NumberFormat annotation, Class<?> fieldType) {
        return configureFormatterFrom(annotation);
    }

    @Override
    public Parser<Number> getParser(NumberFormat annotation, Class<?> fieldType) {
        return configureFormatterFrom(annotation);
    }

    private Formatter<Number> configureFormatterFrom(NumberFormat annotation) {
        String pattern = resolveEmbeddedValue(annotation.pattern());
        if (StringUtils.hasLength(pattern)) {
            return new NumberStyleFormatter(pattern);
        } else {
            Style style = annotation.style();
            if (style == Style.CURRENCY) {
                return new CurrencyStyleFormatter();
            } else if (style == Style.PERCENT) {
                return new PercentStyleFormatter();
            } else {
                return new NumberStyleFormatter();
            }
        }
    }

}
