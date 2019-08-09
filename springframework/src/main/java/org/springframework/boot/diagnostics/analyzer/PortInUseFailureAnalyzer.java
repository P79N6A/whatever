package org.springframework.boot.diagnostics.analyzer;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.web.server.PortInUseException;

class PortInUseFailureAnalyzer extends AbstractFailureAnalyzer<PortInUseException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, PortInUseException cause) {
        return new FailureAnalysis("Web server failed to start. Port " + cause.getPort() + " was already in use.", "Identify and stop the process that's listening on port " + cause.getPort() + " or configure this " + "application to listen on another port.", cause);
    }

}
