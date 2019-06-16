/*
 * Copyright 2017-2018 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author Artem Bilan
 */
@ConfigurationProperties("http")
@Validated
public class HttpSourceProperties {

    /**
     * HTTP endpoint path mapping.
     */
    private String pathPattern = "**/*";

    /**
     * Timeout value for the connection
     */
    private Long timeout = 300000L;

    /**
     * Headers that will be mapped.
     */
    private String[] mappedRequestHeaders = {DefaultHttpHeaderMapper.HTTP_REQUEST_HEADER_NAME_PATTERN, "continuation_id"};

    /**
     * CORS properties.
     */
    @NestedConfigurationProperty
    private HttpSourceCorsProperties cors = new HttpSourceCorsProperties();

    @NotEmpty
    public String getPathPattern() {
        return this.pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    @NotNull
    public Long getTimeout() {
        return this.timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public String[] getMappedRequestHeaders() {
        return this.mappedRequestHeaders;
    }

    public void setMappedRequestHeaders(String[] mappedRequestHeaders) {
        this.mappedRequestHeaders = mappedRequestHeaders;
    }

    public HttpSourceCorsProperties getCors() {
        return this.cors;
    }

    public void setCors(HttpSourceCorsProperties cors) {
        this.cors = cors;
    }

}
