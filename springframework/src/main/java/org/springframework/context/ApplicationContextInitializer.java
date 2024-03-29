package org.springframework.context;

public interface ApplicationContextInitializer<C extends ConfigurableApplicationContext> {

    void initialize(C applicationContext);

}
