package org.springframework.instrument;

import java.lang.instrument.Instrumentation;

public final class InstrumentationSavingAgent {

    private static volatile Instrumentation instrumentation;

    private InstrumentationSavingAgent() {
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

}
