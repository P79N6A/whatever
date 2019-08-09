package org.springframework.boot.convert;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.StringValueResolver;

public class ApplicationConversionService extends FormattingConversionService {

    private static volatile ApplicationConversionService sharedInstance;

    public ApplicationConversionService() {
        this(null);
    }

    public ApplicationConversionService(StringValueResolver embeddedValueResolver) {
        if (embeddedValueResolver != null) {
            setEmbeddedValueResolver(embeddedValueResolver);
        }
        configure(this);
    }

    public static ConversionService getSharedInstance() {
        ApplicationConversionService sharedInstance = ApplicationConversionService.sharedInstance;
        if (sharedInstance == null) {
            synchronized (ApplicationConversionService.class) {
                sharedInstance = ApplicationConversionService.sharedInstance;
                if (sharedInstance == null) {
                    sharedInstance = new ApplicationConversionService();
                    ApplicationConversionService.sharedInstance = sharedInstance;
                }
            }
        }
        return sharedInstance;
    }

    public static void configure(FormatterRegistry registry) {
        DefaultConversionService.addDefaultConverters(registry);
        DefaultFormattingConversionService.addDefaultFormatters(registry);
        addApplicationFormatters(registry);
        addApplicationConverters(registry);
    }

    public static void addApplicationConverters(ConverterRegistry registry) {
        addDelimitedStringConverters(registry);
        registry.addConverter(new StringToDurationConverter());
        registry.addConverter(new DurationToStringConverter());
        registry.addConverter(new NumberToDurationConverter());
        registry.addConverter(new DurationToNumberConverter());
        registry.addConverter(new StringToDataSizeConverter());
        registry.addConverter(new NumberToDataSizeConverter());
        registry.addConverterFactory(new StringToEnumIgnoringCaseConverterFactory());
    }

    public static void addDelimitedStringConverters(ConverterRegistry registry) {
        ConversionService service = (ConversionService) registry;
        registry.addConverter(new ArrayToDelimitedStringConverter(service));
        registry.addConverter(new CollectionToDelimitedStringConverter(service));
        registry.addConverter(new DelimitedStringToArrayConverter(service));
        registry.addConverter(new DelimitedStringToCollectionConverter(service));
    }

    public static void addApplicationFormatters(FormatterRegistry registry) {
        registry.addFormatter(new CharArrayFormatter());
        registry.addFormatter(new InetAddressFormatter());
        registry.addFormatter(new IsoOffsetFormatter());
    }

}
