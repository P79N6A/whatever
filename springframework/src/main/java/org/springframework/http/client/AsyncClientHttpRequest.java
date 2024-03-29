/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.client;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpRequest;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;

@Deprecated
public interface AsyncClientHttpRequest extends HttpRequest, HttpOutputMessage {

    /**
     * Execute this request asynchronously, resulting in a Future handle.
     * {@link ClientHttpResponse} that can be read.
     *
     * @return the future response result of the execution
     * @throws IOException in case of I/O errors
     */
    ListenableFuture<ClientHttpResponse> executeAsync() throws IOException;

}
