package org.springframework.web.servlet.view.json;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public abstract class AbstractJackson2View extends AbstractView {

    private ObjectMapper objectMapper;

    private JsonEncoding encoding = JsonEncoding.UTF8;

    @Nullable
    private Boolean prettyPrint;

    private boolean disableCaching = true;

    protected boolean updateContentLength = false;

    protected AbstractJackson2View(ObjectMapper objectMapper, String contentType) {
        this.objectMapper = objectMapper;
        configurePrettyPrint();
        setContentType(contentType);
        setExposePathVariables(false);
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        configurePrettyPrint();
    }

    public final ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }

    public void setEncoding(JsonEncoding encoding) {
        Assert.notNull(encoding, "'encoding' must not be null");
        this.encoding = encoding;
    }

    public final JsonEncoding getEncoding() {
        return this.encoding;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
        configurePrettyPrint();
    }

    private void configurePrettyPrint() {
        if (this.prettyPrint != null) {
            this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
        }
    }

    public void setDisableCaching(boolean disableCaching) {
        this.disableCaching = disableCaching;
    }

    public void setUpdateContentLength(boolean updateContentLength) {
        this.updateContentLength = updateContentLength;
    }

    @Override
    protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
        setResponseContentType(request, response);
        response.setCharacterEncoding(this.encoding.getJavaName());
        if (this.disableCaching) {
            response.addHeader("Cache-Control", "no-store");
        }
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ByteArrayOutputStream temporaryStream = null;
        OutputStream stream;
        if (this.updateContentLength) {
            temporaryStream = createTemporaryOutputStream();
            stream = temporaryStream;
        } else {
            stream = response.getOutputStream();
        }
        Object value = filterAndWrapModel(model, request);
        writeContent(stream, value);
        if (temporaryStream != null) {
            writeToResponse(response, temporaryStream);
        }
    }

    protected Object filterAndWrapModel(Map<String, Object> model, HttpServletRequest request) {
        Object value = filterModel(model);
        Class<?> serializationView = (Class<?>) model.get(JsonView.class.getName());
        FilterProvider filters = (FilterProvider) model.get(FilterProvider.class.getName());
        if (serializationView != null || filters != null) {
            MappingJacksonValue container = new MappingJacksonValue(value);
            if (serializationView != null) {
                container.setSerializationView(serializationView);
            }
            if (filters != null) {
                container.setFilters(filters);
            }
            value = container;
        }
        return value;
    }

    protected void writeContent(OutputStream stream, Object object) throws IOException {
        JsonGenerator generator = this.objectMapper.getFactory().createGenerator(stream, this.encoding);
        writePrefix(generator, object);
        Object value = object;
        Class<?> serializationView = null;
        FilterProvider filters = null;
        if (value instanceof MappingJacksonValue) {
            MappingJacksonValue container = (MappingJacksonValue) value;
            value = container.getValue();
            serializationView = container.getSerializationView();
            filters = container.getFilters();
        }
        ObjectWriter objectWriter = (serializationView != null ? this.objectMapper.writerWithView(serializationView) : this.objectMapper.writer());
        if (filters != null) {
            objectWriter = objectWriter.with(filters);
        }
        objectWriter.writeValue(generator, value);
        writeSuffix(generator, object);
        generator.flush();
    }

    public abstract void setModelKey(String modelKey);

    protected abstract Object filterModel(Map<String, Object> model);

    protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
    }

    protected void writeSuffix(JsonGenerator generator, Object object) throws IOException {
    }

}
