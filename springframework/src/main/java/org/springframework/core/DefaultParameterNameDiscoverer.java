package org.springframework.core;

public class DefaultParameterNameDiscoverer extends PrioritizedParameterNameDiscoverer {

    public DefaultParameterNameDiscoverer() {
        if (!GraalDetector.inImageCode()) {
            if (KotlinDetector.isKotlinReflectPresent()) {
                addDiscoverer(new KotlinReflectionParameterNameDiscoverer());
            }
            addDiscoverer(new StandardReflectionParameterNameDiscoverer());
            addDiscoverer(new LocalVariableTableParameterNameDiscoverer());
        }
    }

}
