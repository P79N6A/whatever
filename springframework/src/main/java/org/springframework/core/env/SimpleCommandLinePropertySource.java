package org.springframework.core.env;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.List;

public class SimpleCommandLinePropertySource extends CommandLinePropertySource<CommandLineArgs> {

    public SimpleCommandLinePropertySource(String... args) {
        super(new SimpleCommandLineArgsParser().parse(args));
    }

    public SimpleCommandLinePropertySource(String name, String[] args) {
        super(name, new SimpleCommandLineArgsParser().parse(args));
    }

    @Override
    public String[] getPropertyNames() {
        return StringUtils.toStringArray(this.source.getOptionNames());
    }

    @Override
    protected boolean containsOption(String name) {
        return this.source.containsOption(name);
    }

    @Override
    @Nullable
    protected List<String> getOptionValues(String name) {
        return this.source.getOptionValues(name);
    }

    @Override
    protected List<String> getNonOptionArgs() {
        return this.source.getNonOptionArgs();
    }

}
