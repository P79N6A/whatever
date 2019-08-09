package org.springframework.web.multipart.support;

import org.springframework.beans.propertyeditors.ByteArrayPropertyEditor;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class ByteArrayMultipartFileEditor extends ByteArrayPropertyEditor {

    @Override
    public void setValue(@Nullable Object value) {
        if (value instanceof MultipartFile) {
            MultipartFile multipartFile = (MultipartFile) value;
            try {
                super.setValue(multipartFile.getBytes());
            } catch (IOException ex) {
                throw new IllegalArgumentException("Cannot read contents of multipart file", ex);
            }
        } else if (value instanceof byte[]) {
            super.setValue(value);
        } else {
            super.setValue(value != null ? value.toString().getBytes() : null);
        }
    }

    @Override
    public String getAsText() {
        byte[] value = (byte[]) getValue();
        return (value != null ? new String(value) : "");
    }

}
