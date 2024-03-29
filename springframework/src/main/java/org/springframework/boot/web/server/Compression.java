package org.springframework.boot.web.server;

import org.springframework.util.unit.DataSize;

public class Compression {

    private boolean enabled = false;

    private String[] mimeTypes = new String[]{"text/html", "text/xml", "text/plain", "text/css", "text/javascript", "application/javascript", "application/json", "application/xml"};

    private String[] excludedUserAgents = null;

    private DataSize minResponseSize = DataSize.ofKilobytes(2);

    public boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String[] getMimeTypes() {
        return this.mimeTypes;
    }

    public void setMimeTypes(String[] mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public String[] getExcludedUserAgents() {
        return this.excludedUserAgents;
    }

    public void setExcludedUserAgents(String[] excludedUserAgents) {
        this.excludedUserAgents = excludedUserAgents;
    }

    public DataSize getMinResponseSize() {
        return this.minResponseSize;
    }

    public void setMinResponseSize(DataSize minSize) {
        this.minResponseSize = minSize;
    }

}
