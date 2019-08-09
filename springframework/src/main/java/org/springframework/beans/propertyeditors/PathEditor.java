package org.springframework.beans.propertyeditors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathEditor extends PropertyEditorSupport {

    private final ResourceEditor resourceEditor;

    public PathEditor() {
        this.resourceEditor = new ResourceEditor();
    }

    public PathEditor(ResourceEditor resourceEditor) {
        Assert.notNull(resourceEditor, "ResourceEditor must not be null");
        this.resourceEditor = resourceEditor;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        boolean nioPathCandidate = !text.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX);
        if (nioPathCandidate && !text.startsWith("/")) {
            try {
                URI uri = new URI(text);
                if (uri.getScheme() != null) {
                    nioPathCandidate = false;
                    // Let's try NIO file system providers via Paths.get(URI)
                    setValue(Paths.get(uri).normalize());
                    return;
                }
            } catch (URISyntaxException | FileSystemNotFoundException ex) {
                // Not a valid URI (let's try as Spring resource location),
                // or a URI scheme not registered for NIO (let's try URL
                // protocol handlers via Spring's resource mechanism).
            }
        }
        this.resourceEditor.setAsText(text);
        Resource resource = (Resource) this.resourceEditor.getValue();
        if (resource == null) {
            setValue(null);
        } else if (!resource.exists() && nioPathCandidate) {
            setValue(Paths.get(text).normalize());
        } else {
            try {
                setValue(resource.getFile().toPath());
            } catch (IOException ex) {
                throw new IllegalArgumentException("Failed to retrieve file for " + resource, ex);
            }
        }
    }

    @Override
    public String getAsText() {
        Path value = (Path) getValue();
        return (value != null ? value.toString() : "");
    }

}
