/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
package com.globo.galeb.core;

import com.globo.galeb.exceptions.BadRequestException;
import com.globo.galeb.logger.impl.NcsaLogExtendedFormatter;
import com.globo.galeb.metrics.ICounter;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.logging.Logger;

public class ServerResponse {

    private final HttpServerRequest req;
    private final Logger log;
    private final ICounter counter;
    private final boolean enableAccessLog;
    private final HttpServerResponse resp;
    private String message = "";
    private String id = "";
    private String headerHost = "";

    private int exceptionToHttpCode(final Throwable e) {
        if (e instanceof java.util.concurrent.TimeoutException) {
            return HttpCode.GatewayTimeout;
        } else if (e instanceof BadRequestException) {
            return HttpCode.BadRequest;
        } else {
            return HttpCode.BadGateway;
        }
    }

    public ServerResponse(final HttpServerRequest req,
                          final Logger log,
                          final ICounter counter,
                          boolean enableAccessLog) {
        this.log = log;
        this.counter = counter;
        this.enableAccessLog = enableAccessLog;
        this.req = req;
        this.resp = req.response();
    }

    public ServerResponse setId(String id) {
        this.id = id;
        return this;
    }

    public ServerResponse setHeaderHost(String headerHost) {
        this.headerHost = headerHost;
        return this;
    }

    public ServerResponse setMessage(final String message) {
        this.message = message;
        return this;
    }

    public ServerResponse setStatusCode(Integer code) {

        resp.setStatusCode(code);
        String message = HttpCode.getMessage(code);
        resp.setStatusMessage(message);
        return this;
    }

    public ServerResponse setHeaders(final MultiMap headers) {
        resp.headers().set(headers);
        return this;
    }

    public void showErrorAndClose(final Throwable event) {

        int statusCode = exceptionToHttpCode(event);
        setStatusCode(statusCode);

        end();

        String message = String.format("FAIL with HttpStatus %d%s: %s",
                statusCode,
                !"".equals(headerHost) ? " (virtualhost: "+headerHost+")" : "",
                HttpCode.getMessage(statusCode, false));

        if (statusCode>=HttpCode.InternalServerError) {
            log.error(message);
        } else {
            log.warn(message);
        }

        closeResponse();
    }

    public void closeResponse() {
        try {
            resp.close();
        } catch (RuntimeException ignoreAlreadyClose) {
            return;
        }
    }

    private void realEnd(String message) {

        try {
            if (!"".equals(message)) {

                resp.end(message);
            } else {
                resp.end();
            }
        } catch (RuntimeException e) {
            if (e instanceof java.lang.IllegalStateException) {
                // Response has already been written ? Ignore.
                log.debug(e.getMessage());
            } else {
                log.error(String.format("FAIL: statusCode %d, Error > %s", resp.getStatusCode(), e.getMessage()));
            }
            return;
        }
    }

    public void end() {
        logRequest(enableAccessLog);
        sendRequestCount(id, resp.getStatusCode());
        realEnd(message);
    }

    public void logRequest(boolean enable) {

        if (enableAccessLog) {
            Integer code = resp.getStatusCode();
            String message = "";
            int codeFamily = code.intValue()/100;
            // TODO: Dependency Injection
            String httpLogMessage = new NcsaLogExtendedFormatter()
                                        .setRequestData(req, message)
                                        .getFormatedLog();
            switch (codeFamily) {
                case 5: // SERVER_ERROR
                    log.error(httpLogMessage);
                    break;
                case 0: // OTHER,
                case 1: // INFORMATIONAL
                case 2: // SUCCESSFUL
                case 3: // REDIRECTION
                case 4: // CLIENT_ERROR
                default:
                    log.info(httpLogMessage);
                    break;
            }
        }
    }

    public void sendRequestCount(String id, int code) {
        if (counter!=null && !"".equals(id)) {
            counter.httpCode(id, code);
        }
    }

}
