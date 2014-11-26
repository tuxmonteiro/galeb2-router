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
package com.globo.galeb.server;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.exceptions.AbstractHttpException;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.logger.impl.NcsaLogExtendedFormatter;
import com.globo.galeb.metrics.ICounter;
import com.globo.galeb.rulereturn.HttpCode;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

/**
 * Class ServerResponse.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class ServerResponse {

    /** The httpServerRequest. */
    private final HttpServerRequest req;

    /** The logger. */
    private SafeLogger log = null;

    /** The counter. */
    private ICounter counter = null;

    /** is enabled access log? */
    private boolean enableAccessLog = false;

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
     * Define logger if necessary.
     */
    private void defineLoggerIfNecessary() {
        if (log==null) {
            log = new SafeLogger();
        }
    }

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
     */
    public ServerResponse(final HttpServerRequest req) {
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

        defineLoggerIfNecessary();
        if (statusCode>=HttpCode.INTERNAL_SERVER_ERROR) {
            log.error(logMessage);
            log.debug(getStackTrace(event));
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
            defineLoggerIfNecessary();
            log.debug(e);
        }
    }

    /**
     * Log the request.
     */
    public void logRequest() {

        if (enableAccessLog) {

            Integer code = resp.getStatusCode();
            String httpLogMessage = new NcsaLogExtendedFormatter()
                                        .setRequestData(req)
                                        .getFormatedLog();

            defineLoggerIfNecessary();
            if (HttpCode.isServerError(code.intValue())) {
                log.error(httpLogMessage);
            } else {
                log.info(httpLogMessage);
            }

        }
    }

    /**
     * Send request count to counter.
     */
    public void sendRequestCount() {
        int code = HttpCode.INTERNAL_SERVER_ERROR;
        if (req!=null) {
            code = resp.getStatusCode();
        }
        if (counter!=null) {
            if (!"".equals(headerHost) && !IJsonable.UNDEF.equals(headerHost) &&
                    !"".equals(backendId) && !IJsonable.UNDEF.equals(backendId)) {
                counter.httpCode(headerHost, backendId, code);
            } else if (!"".equals(id) && !IJsonable.UNDEF.equals(id)) {
                counter.httpCode(id, code);
            }
        }
    }

    /**
     * Sets the chunked.
     *
     * @param enableChunked the enable chunked
     * @return the server response
     */
    public ServerResponse setChunked(Boolean enableChunked) {
        this.resp.setChunked(enableChunked);
        return this;
    }

    /**
     * Sets the counter.
     *
     * @param counter the counter
     * @return the server response
     */
    public ServerResponse setCounter(final ICounter counter) {
        this.counter = counter;
        return this;
    }

    /**
     * Sets the enable access log.
     *
     * @param enableAccessLog the enable access log
     * @return the server response
     */
    public ServerResponse setEnableAccessLog(boolean enableAccessLog) {
        this.enableAccessLog = enableAccessLog;
        return this;
    }

    /**
     * Sets the log.
     *
     * @param log the log
     * @return the server response
     */
    public ServerResponse setLog(final SafeLogger alog) {
        this.log = alog;
        return this;
    }

}
