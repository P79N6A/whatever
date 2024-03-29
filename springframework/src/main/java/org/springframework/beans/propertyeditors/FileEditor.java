package org.springframework.beans.propertyeditors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;

public class FileEditor extends PropertyEditorSupport {

    private final ResourceEditor resourceEditor;

    public FileEditor() {
        this.resourceEditor = new ResourceEditor();
    }

    public FileEditor(ResourceEditor resourceEditor) {
        Assert.notNull(resourceEditor, "ResourceEditor must not be null");
        this.resourceEditor = resourceEditor;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (!StringUtils.hasText(text)) {
            setValue(null);
            return;
        }
        // Check whether we got an absolute file path without "file:" prefix.
        // For backwards compatibility, we'll consider those as straight file path.
        File file = null;
        if (!ResourceUtils.isUrl(text)) {
            file = new File(text);
            if (file.isAbsolute()) {
                setValue(file);
                return;
            }
        }
        // Proceed with standard resource location parsing.
        this.resourceEditor.setAsText(text);
        Resource resource = (Resource) this.resourceEditor.getValue();
        // If it's a URL or a path pointing to an existing resource, use it as-is.
        if (file == null || resource.exists()) {
            try {
                setValue(resource.getFile());
            } catch (IOException ex) {
                throw new IllegalArgumentException("Could not retrieve file for " + resource + ": " + ex.getMessage());
            }
        } else {
            // Set a relative File reference and hope for the best.
            setValue(file);
        }
    }

    @Override
    public String getAsText() {
        File value = (File) getValue();
        return (value != null ? value.getPath() : "");
    }

}
