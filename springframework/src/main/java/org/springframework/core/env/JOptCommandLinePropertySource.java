//
//
// package org.springframework.core.env;
//
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.List;
//
// import joptsimple.OptionSet;
// import joptsimple.OptionSpec;
//
// import org.springframework.lang.Nullable;
// import org.springframework.util.CollectionUtils;
// import org.springframework.util.StringUtils;
//
// public class JOptCommandLinePropertySource extends CommandLinePropertySource<OptionSet> {
//
//     public JOptCommandLinePropertySource(OptionSet options) {
//         super(options);
//     }
//
//     public JOptCommandLinePropertySource(String name, OptionSet options) {
//         super(name, options);
//     }
//
//     @Override
//     protected boolean containsOption(String name) {
//         return this.source.has(name);
//     }
//
//     @Override
//     public String[] getPropertyNames() {
//         List<String> names = new ArrayList<>();
//         for (OptionSpec<?> spec : this.source.specs()) {
//             String lastOption = CollectionUtils.lastElement(spec.options());
//             if (lastOption != null) {
//                 // Only the longest name is used for enumerating
//                 names.add(lastOption);
//             }
//         }
//         return StringUtils.toStringArray(names);
//     }
//
//     @Override
//     @Nullable
//     public List<String> getOptionValues(String name) {
//         List<?> argValues = this.source.valuesOf(name);
//         List<String> stringArgValues = new ArrayList<>();
//         for (Object argValue : argValues) {
//             stringArgValues.add(argValue.toString());
//         }
//         if (stringArgValues.isEmpty()) {
//             return (this.source.has(name) ? Collections.emptyList() : null);
//         }
//         return Collections.unmodifiableList(stringArgValues);
//     }
//
//     @Override
//     protected List<String> getNonOptionArgs() {
//         List<?> argValues = this.source.nonOptionArguments();
//         List<String> stringArgValues = new ArrayList<>();
//         for (Object argValue : argValues) {
//             stringArgValues.add(argValue.toString());
//         }
//         return (stringArgValues.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(stringArgValues));
//     }
//
// }
