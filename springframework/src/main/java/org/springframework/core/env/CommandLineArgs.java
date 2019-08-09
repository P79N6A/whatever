package org.springframework.core.env;

import org.springframework.lang.Nullable;

import java.util.*;

class CommandLineArgs {

    private final Map<String, List<String>> optionArgs = new HashMap<>();

    private final List<String> nonOptionArgs = new ArrayList<>();

    public void addOptionArg(String optionName, @Nullable String optionValue) {
        if (!this.optionArgs.containsKey(optionName)) {
            this.optionArgs.put(optionName, new ArrayList<>());
        }
        if (optionValue != null) {
            this.optionArgs.get(optionName).add(optionValue);
        }
    }

    public Set<String> getOptionNames() {
        return Collections.unmodifiableSet(this.optionArgs.keySet());
    }

    public boolean containsOption(String optionName) {
        return this.optionArgs.containsKey(optionName);
    }

    @Nullable
    public List<String> getOptionValues(String optionName) {
        return this.optionArgs.get(optionName);
    }

    public void addNonOptionArg(String value) {
        this.nonOptionArgs.add(value);
    }

    public List<String> getNonOptionArgs() {
        return Collections.unmodifiableList(this.nonOptionArgs);
    }

}
