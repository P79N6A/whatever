package org.springframework.instrument.classloading;

import org.springframework.instrument.InstrumentationSavingAgent;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

public class InstrumentationLoadTimeWeaver implements LoadTimeWeaver {

    private static final boolean AGENT_CLASS_PRESENT = ClassUtils.isPresent("org.springframework.instrument.InstrumentationSavingAgent", InstrumentationLoadTimeWeaver.class.getClassLoader());

    @Nullable
    private final ClassLoader classLoader;

    @Nullable
    private final Instrumentation instrumentation;

    private final List<ClassFileTransformer> transformers = new ArrayList<>(4);

    public InstrumentationLoadTimeWeaver() {
        this(ClassUtils.getDefaultClassLoader());
    }

    public InstrumentationLoadTimeWeaver(@Nullable ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.instrumentation = getInstrumentation();
    }

    /**
     * new AspectJClassBypassingClassFileTransformer(new ClassPreProcessorAgentAdapter())
     * 具体的类加载处理由AspectJ接管
     */
    @Override
    public void addTransformer(ClassFileTransformer transformer) {
        Assert.notNull(transformer, "Transformer must not be null");
        FilteringClassFileTransformer actualTransformer = new FilteringClassFileTransformer(transformer, this.classLoader);
        synchronized (this.transformers) {
            Assert.state(this.instrumentation != null, "Must start with Java agent to use InstrumentationLoadTimeWeaver. See Spring documentation.");
            this.instrumentation.addTransformer(actualTransformer);
            this.transformers.add(actualTransformer);
        }
    }

    @Override
    public ClassLoader getInstrumentableClassLoader() {
        Assert.state(this.classLoader != null, "No ClassLoader available");
        return this.classLoader;
    }

    @Override
    public ClassLoader getThrowawayClassLoader() {
        return new SimpleThrowawayClassLoader(getInstrumentableClassLoader());
    }

    public void removeTransformers() {
        synchronized (this.transformers) {
            if (this.instrumentation != null && !this.transformers.isEmpty()) {
                for (int i = this.transformers.size() - 1; i >= 0; i--) {
                    this.instrumentation.removeTransformer(this.transformers.get(i));
                }
                this.transformers.clear();
            }
        }
    }

    public static boolean isInstrumentationAvailable() {
        return (getInstrumentation() != null);
    }

    @Nullable
    private static Instrumentation getInstrumentation() {
        if (AGENT_CLASS_PRESENT) {
            return InstrumentationAccessor.getInstrumentation();
        } else {
            return null;
        }
    }

    private static class InstrumentationAccessor {

        public static Instrumentation getInstrumentation() {
            return InstrumentationSavingAgent.getInstrumentation();
        }

    }

    private static class FilteringClassFileTransformer implements ClassFileTransformer {

        private final ClassFileTransformer targetTransformer;

        @Nullable
        private final ClassLoader targetClassLoader;

        public FilteringClassFileTransformer(ClassFileTransformer targetTransformer, @Nullable ClassLoader targetClassLoader) {
            this.targetTransformer = targetTransformer;
            this.targetClassLoader = targetClassLoader;
        }

        @Override
        @Nullable
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if (this.targetClassLoader != loader) {
                return null;
            }
            return this.targetTransformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
        }

        @Override
        public String toString() {
            return "FilteringClassFileTransformer for: " + this.targetTransformer.toString();
        }

    }

}
