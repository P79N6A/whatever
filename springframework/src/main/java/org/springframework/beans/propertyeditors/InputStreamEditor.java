package org.springframework.beans.propertyeditors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

public class InputStreamEditor extends PropertyEditorSupport {

    private final ResourceEditor resourceEditor;

    public InputStreamEditor() {
        this.resourceEditor = new ResourceEditor();
    }

    public InputStreamEditor(ResourceEditor resourceEditor) {
        Assert.notNull(resourceEditor, "ResourceEditor must not be null");
        this.resourceEditor = resourceEditor;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        this.resourceEditor.setAsText(text);
        Resource resource = (Resource) this.resourceEditor.getValue();
        try {
            setValue(resource != null ? resource.getInputStream() : null);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to retrieve InputStream for " + resource, ex);
        }
    }

    @Override
    @Nullable
    public String getAsText() {
        return null;
    }

}
