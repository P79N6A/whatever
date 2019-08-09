package org.springframework.http;

import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

public class MediaTypeEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
        if (StringUtils.hasText(text)) {
            setValue(MediaType.parseMediaType(text));
        } else {
            setValue(null);
        }
    }

    @Override
    public String getAsText() {
        MediaType mediaType = (MediaType) getValue();
        return (mediaType != null ? mediaType.toString() : "");
    }

}
