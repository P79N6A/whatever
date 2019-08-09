package org.springframework.core.env;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

public abstract class CommandLinePropertySource<T> extends EnumerablePropertySource<T> {

    public static final String COMMAND_LINE_PROPERTY_SOURCE_NAME = "commandLineArgs";

    public static final String DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME = "nonOptionArgs";

    private String nonOptionArgsPropertyName = DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME;

    public CommandLinePropertySource(T source) {
        super(COMMAND_LINE_PROPERTY_SOURCE_NAME, source);
    }

    public CommandLinePropertySource(String name, T source) {
        super(name, source);
    }

    public void setNonOptionArgsPropertyName(String nonOptionArgsPropertyName) {
        this.nonOptionArgsPropertyName = nonOptionArgsPropertyName;
    }

    @Override
    public final boolean containsProperty(String name) {
        if (this.nonOptionArgsPropertyName.equals(name)) {
            return !this.getNonOptionArgs().isEmpty();
        }
        return this.containsOption(name);
    }

    @Override
    @Nullable
    public final String getProperty(String name) {
        if (this.nonOptionArgsPropertyName.equals(name)) {
            Collection<String> nonOptionArguments = this.getNonOptionArgs();
            if (nonOptionArguments.isEmpty()) {
                return null;
            } else {
                return StringUtils.collectionToCommaDelimitedString(nonOptionArguments);
            }
        }
        Collection<String> optionValues = this.getOptionValues(name);
        if (optionValues == null) {
            return null;
        } else {
            return StringUtils.collectionToCommaDelimitedString(optionValues);
        }
    }

    protected abstract boolean containsOption(String name);

    @Nullable
    protected abstract List<String> getOptionValues(String name);

    protected abstract List<String> getNonOptionArgs();

}
