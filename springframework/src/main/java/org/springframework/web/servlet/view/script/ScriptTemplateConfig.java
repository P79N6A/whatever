package org.springframework.web.servlet.view.script;

import org.springframework.lang.Nullable;

import javax.script.ScriptEngine;
import java.nio.charset.Charset;

public interface ScriptTemplateConfig {

    @Nullable
    ScriptEngine getEngine();

    @Nullable
    String getEngineName();

    @Nullable
    Boolean isSharedEngine();

    @Nullable
    String[] getScripts();

    @Nullable
    String getRenderObject();

    @Nullable
    String getRenderFunction();

    @Nullable
    String getContentType();

    @Nullable
    Charset getCharset();

    @Nullable
    String getResourceLoaderPath();

}
