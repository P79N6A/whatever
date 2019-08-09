package org.springframework.http.client;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpRequest;

import java.io.IOException;

public interface ClientHttpRequest extends HttpRequest, HttpOutputMessage {

    ClientHttpResponse execute() throws IOException;

}
