/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.cloud.stream.app.http.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.integration.http.dsl.AsyncContextServletEndpointSpec;
import org.springframework.integration.http.inbound.AsyncContextServletMessagingGateway;
import org.springframework.integration.http.inbound.HttpRequestHandlingEndpointSupport;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.util.MimeType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A source module that listens for HTTP requests and emits the body as a message payload. If the Content-Type matches
 * 'text/*' or 'application/json', the payload will be a String, otherwise the payload will be a byte array.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Artem Bilan
 * @author Gary Russell
 * @author Christian Tzolov
 */
@EnableBinding(Processor.class)
@EnableConfigurationProperties({HttpSourceProperties.class})
public class HttpProcessorConfiguration {

    @Autowired
    private Processor channels;

    @Autowired
    private HttpSourceProperties properties;

    @Bean
    public HttpRequestHandlingEndpointSupport httpSourceString() {
        return buildHttpRequestHandlerEndpointSpec("text/*", "application/json")
                .requestPayloadType(String.class)
                .get();
    }

    @Bean
    public HttpRequestHandlingEndpointSupport httpSourceBytes() {
        return buildHttpRequestHandlerEndpointSpec("*/*")
                .get();
    }

    private AsyncContextServletEndpointSpec buildHttpRequestHandlerEndpointSpec(final String... consumes) {
        return new AsyncContextServletEndpointSpec(new AsyncContextServletMessagingGateway(),
                this.properties.getPathPattern())
                .headerMapper(new DefaultHttpHeaderMapper() {

                    {
                        DefaultHttpHeaderMapper.setupDefaultInboundMapper(this);
                        setInboundHeaderNames(properties.getMappedRequestHeaders());
                    }

                    protected Object getHttpHeader(HttpHeaders source, String name) {
                        if (ACCEPT.equalsIgnoreCase(name)) {
                            List<MediaType> mediaTypes = source.getAccept();
                            return mediaTypes.stream().map(MimeType::toString).collect(Collectors.toList());
                        } else {
                            return super.getHttpHeader(source, name);
                        }
                    }
                })
                .setTimeout(properties.getTimeout())
                .requestMapping(requestMapping ->
                        requestMapping.methods(HttpMethod.POST, HttpMethod.GET, HttpMethod.DELETE, HttpMethod.PUT, HttpMethod.OPTIONS)
                                .consumes(consumes))
                .crossOrigin(crossOrigin ->
                        crossOrigin.origin(this.properties.getCors().getAllowedOrigins())
                                .allowedHeaders(this.properties.getCors().getAllowedHeaders())
                                .allowCredentials(this.properties.getCors().getAllowCredentials()))
                .requestChannel(this.channels.output())
                .replyChannel(this.channels.input());
    }
}