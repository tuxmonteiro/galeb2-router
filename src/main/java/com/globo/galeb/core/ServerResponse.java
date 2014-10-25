/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.globo.galeb.core;

import com.globo.galeb.exceptions.AbstractHttpException;
import com.globo.galeb.logger.impl.NcsaLogExtendedFormatter;
import com.globo.galeb.metrics.ICounter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.logging.Logger;

/**
 * Class ServerResponse.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class ServerResponse {

    /** The Constant UNDEF. */
    private static final String UNDEF = "UNDEF";

    /** The httpServerRequest. */
    private final HttpServerRequest req;

    /** The logger. */
    private final Logger log;

    /** The counter. */
    private final ICounter counter;

    /** is enabled access log? */
    private final boolean enableAccessLog;

    /** The httpServerResponse. */
    private final HttpServerResponse resp;

    /** The message. */
    private String message = "";

    /** The id. */
    private String id = "";

    /** The header host. */
    private String headerHost = "";

    /** The backend id. */
    private String backendId = "";

    /**
     * Convert Exception to HttpCode.
     *
     * @param e the expection
     * @return the http code status
     */
    private int exceptionToHttpCode(final Throwable e) {
        if (e instanceof AbstractHttpException) {
            return ((AbstractHttpException)e).getHttpCode();
        } else {
            return HttpCode.SERVICE_UNAVAILABLE;
        }
    }

    /**
     * Instantiates a new server response.
     *
     * @param req the req
     * @param log the log
     * @param counter the counter
     * @param enableAccessLog the enable access log
     */
    public ServerResponse(final HttpServerRequest req,
                          final Logger log,
                          final ICounter counter,
                          boolean enableAccessLog) {
        this.log = log;
        this.counter = counter;
        this.enableAccessLog = enableAccessLog;
        this.req = req;
        this.resp = req.response();
        resp.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                showErrorAndClose(event);
            }
        });
    }

    /**
     * Sets the id.
     *
     * @param id the id
     * @return this
     */
    public ServerResponse setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the backend id.
     *
     * @param backendId the backend id
     * @return this
     */
    public ServerResponse setBackendId(String backendId) {
        this.backendId = backendId;
        return this;
    }

    /**
     * Sets the header host.
     *
     * @param headerHost the header host
     * @return this
     */
    public ServerResponse setHeaderHost(String headerHost) {
        this.headerHost = headerHost;
        return this;
    }

    /**
     * Sets the message.
     *
     * @param message the message
     * @return this
     */
    public ServerResponse setMessage(final String message) {
        this.message = message;
        return this;
    }

    /**
     * Sets the status code.
     *
     * @param code the code
     * @return this
     */
    public ServerResponse setStatusCode(Integer code) {
        resp.setStatusCode(code);
        resp.setStatusMessage(HttpCode.getMessage(code));
        return this;
    }

    /**
     * Sets the headers.
     *
     * @param headers the headers
     * @return this
     */
    public ServerResponse setHeaders(final MultiMap headers) {
        resp.headers().set(headers);
        return this;
    }

    /**
     * Show error and finish connection (and close, if necessary).
     *
     * @param event the event/exception
     */
    public void showErrorAndClose(final Throwable event) {

        int statusCode = exceptionToHttpCode(event);
        setStatusCode(statusCode);

        String logMessage = String.format("FAIL with HttpStatus %d%s: %s",
                statusCode,
                !"".equals(headerHost) ? String.format(" (virtualhost: %s)", headerHost) : "",
                HttpCode.getMessage(statusCode, false));

        if (statusCode>=HttpCode.INTERNAL_SERVER_ERROR) {
            log.error(logMessage);
        } else {
            log.warn(logMessage);
        }

        endResponse();

        try {
            closeResponse();
        } catch (IllegalStateException e) {
            // Response has already been finish?
            log.debug(e.getMessage());
        } catch (RuntimeException e2) {
            log.error(String.format("FAIL: statusCode %d, Error > %s", resp.getStatusCode(), e2.getMessage()));
        }
    }

    /**
     * Close response.
     */
    public void closeResponse() throws RuntimeException {
            resp.close();
    }

    /**
     * Real end method.
     *
     * @param message the message
     */
    private void realEnd() throws RuntimeException {
        if (!"".equals(message)) {
            resp.end(message);
        } else {
            resp.end();
        }
    }

    /**
     * Finish the connection.
     */
    public void endResponse() {
        logRequest();
        sendRequestCount();
        try {
            realEnd();
        } catch (RuntimeException e) {
            log.debug(e);
        }
    }

    /**
     * Log the request.
     *
     * @param enable the enable
     */
    public void logRequest() {

        if (enableAccessLog) {

            Integer code = resp.getStatusCode();
            String httpLogMessage = new NcsaLogExtendedFormatter()
                                        .setRequestData(req)
                                        .getFormatedLog();

            if (HttpCode.isServerError(code.intValue())) {
                log.error(httpLogMessage);
            } else {
                log.info(httpLogMessage);
            }

        }
    }

    /**
     * Send request count to counter.
     *
     * @param code the code
     */
    public void sendRequestCount() {
        int code = HttpCode.INTERNAL_SERVER_ERROR;
        if (req!=null) {
            code = resp.getStatusCode();
        }
        if (counter!=null) {
            if (!"".equals(headerHost) && !UNDEF.equals(headerHost) &&
                    !"".equals(backendId) && !UNDEF.equals(backendId)) {
                counter.httpCode(headerHost, backendId, code);
            } else if (!"".equals(id) && !UNDEF.equals(id)) {
                counter.httpCode(id, code);
            }
        }
    }

    public void setChunked(Boolean enableChunked) {
        this.resp.setChunked(enableChunked);
    }

}
