package org.springframework.boot.autoconfigure.gson;

import com.google.gson.GsonBuilder;

@FunctionalInterface
public interface GsonBuilderCustomizer {

    void customize(GsonBuilder gsonBuilder);

}
