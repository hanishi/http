package org.springframework.integration.http.inbound;

import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.integration.http.inbound.continuation.Continuation;
import org.springframework.integration.http.inbound.continuation.Continuations;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class AsyncContextServletMessagingGateway extends HttpRequestHandlingEndpointSupport implements
        HttpRequestHandler {
    private static final String CONTINUATION_ID = "continuation_id";
    private static final long TIMEOUT = 300000;
    private volatile boolean convertExceptions;
    private long timeout = TIMEOUT;

    public AsyncContextServletMessagingGateway() {
        super(false);
    }

    public void setConvertExceptions(boolean convertExceptions) {
        this.convertExceptions = convertExceptions;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    protected void doStart() {
        SubscribableChannel channel = (SubscribableChannel) this.getReplyChannel();
        channel.subscribe(message -> {
            String continuationId = message.getHeaders().get(CONTINUATION_ID, String.class);
            Continuation continuation = Continuations.getContinuation(Integer.parseInt(continuationId));
            if (continuation != null && !continuation.isExpired()) {
                continuation.setReply(message);
            } else {
                logger.info("client is gone!");
            }
        });
    }

    public final void handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws IOException {
        Object responseContent = null;
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
        Continuation continuation = Continuations.getContinuation(servletRequest, timeout);
        request.getHeaders().set(CONTINUATION_ID, continuation.getId().toString());
        RequestEntity<Object> httpEntity = prepareRequestEntity(request);
        Message<?> responseMessage = continuation.dispatch(servletRequest);
        try {
            if (continuation.isExpired()) {
                setStatusCodeIfNeeded(response, httpEntity);
            } else {
                if (responseMessage == null) {
                    responseMessage = doHandleRequest(servletRequest, httpEntity, servletResponse);
                }
                if (responseMessage != null) {
                    MimeType mimeType = responseMessage.getHeaders().get(MessageHeaders.CONTENT_TYPE, MimeType.class);
                    responseMessage = MessageBuilder.fromMessage(responseMessage).setHeader(MessageHeaders.CONTENT_TYPE, mimeType.toString()).build();
                    responseContent = setupResponseAndConvertReply(response, responseMessage);
                }
            }
        } catch (Exception e) {
            responseContent = handleExceptionInternal(e);
        }
        if (responseContent != null) {
            if (responseContent instanceof HttpStatus) {
                response.setStatusCode((HttpStatus) responseContent);
            } else {
                if (responseContent instanceof ResponseEntity) {
                    ResponseEntity<?> responseEntity = (ResponseEntity<?>) responseContent;
                    responseContent = responseEntity.getBody();
                    response.setStatusCode(responseEntity.getStatusCode());

                    HttpHeaders outputHeaders = response.getHeaders();
                    HttpHeaders entityHeaders = responseEntity.getHeaders();

                    entityHeaders.entrySet()
                            .stream()
                            .filter(entry -> !outputHeaders.containsKey(entry.getKey()))
                            .forEach(entry -> outputHeaders.put(entry.getKey(), entry.getValue()));
                }
                if (responseContent != null) {
                    writeResponse(responseContent, response, request.getHeaders().getAccept());
                } else {
                    response.flush();
                }
            }
        }
    }

    private Object handleExceptionInternal(Exception ex) throws IOException {
        if (this.convertExceptions && isExpectReply()) {
            return ex;
        } else {
            if (ex instanceof IOException) {
                throw (IOException) ex;
            } else if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new MessagingException("error occurred handling HTTP request", ex);
            }
        }
    }

    private void writeResponse(Object content, ServletServerHttpResponse response, List<MediaType> acceptTypesArg)
            throws IOException {

        List<MediaType> acceptTypes = acceptTypesArg;
        if (CollectionUtils.isEmpty(acceptTypes)) {
            acceptTypes = Collections.singletonList(MediaType.ALL);
        }
        for (HttpMessageConverter<?> converter : getMessageConverters()) {
            for (MediaType acceptType : acceptTypes) {
                if (converter.canWrite(content.getClass(), acceptType)) {
                    @SuppressWarnings("unchecked")
                    HttpMessageConverter<Object> converterToUse = (HttpMessageConverter<Object>) converter;
                    converterToUse.write(content, acceptType, response);
                    return;
                }
            }
        }
        throw new MessagingException("Could not convert reply: no suitable HttpMessageConverter found for type ["
                + content.getClass().getName() + "] and accept types [" + acceptTypes + "]");
    }
}
