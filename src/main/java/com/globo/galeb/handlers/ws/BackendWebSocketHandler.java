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
package com.globo.galeb.handlers.ws;

import java.util.LinkedList;
import java.util.Queue;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.core.bus.IQueueService;

/**
 * Class BackendWebSocketHandler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class BackendWebSocketHandler implements Handler<WebSocket> {

    /** The vertx. */
    private final Vertx vertx;

    /** The logger. */
    private final Logger log;

    /** The backend id. */
    private final String backendId;

    /** The server web socket. */
    private final ServerWebSocket serverWebSocket;

    /** Is frontend WebSocket closed? */
    private boolean frontendWSisClosed;

    /** Is backend WebSocker closed */
    private boolean backendWSisClosed;

    /** The messages queue. */
    private final Queue<Buffer> messages = new LinkedList<Buffer>();

    /** The websocket. */
    private WebSocket websocket;

    /** The initial request time. */
    private Long initialRequestTime = null;

    /**
     * Instantiates a new backend web socket handler.
     *
     * @param vertx the vertx
     * @param log the log
     * @param backendId the backend id
     * @param serverWebSocket the server web socket
     */
    public BackendWebSocketHandler(
            final Vertx vertx,
            final Logger log,
            final String backendId,
            final ServerWebSocket serverWebSocket) {
        this.vertx = vertx;
        this.backendId = backendId;
        this.serverWebSocket = serverWebSocket;
        this.log = log;
    }

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(final WebSocket websocket) {

        this.websocket = websocket;
        websocket.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                log.error(String.format("backend: %s, message: %s", backendId, event.getMessage()));
                vertx.eventBus().publish(IQueueService.QUEUE_HEALTHCHECK_FAIL, backendId );
                websocket.close();
            }
        });

        writeWebSocket(websocket, messages);

        websocket.dataHandler(new Handler<Buffer>() {

            @Override
            public void handle(Buffer buffer) {
                writeServerWebSocket(buffer);
            }

        });

        websocket.closeHandler(new Handler<Void>() {

            @Override
            public void handle(Void event) {
                backendWSisClosed = true;
                log.debug("Backend WebSocket was closed");
                if (! frontendWSisClosed) {
                    frontendWSisClosed = true;
                    serverWebSocket.close();
                }
            }
        });
    }

    /**
     * Write websocket.
     *
     * @param ws the websocket
     * @param messages the messages queue
     */
    public void writeWebSocket(final WebSocket ws, final Queue<Buffer> messages) {
        while (!messages.isEmpty()) {
            writeWebSocket(ws, messages.poll());
        }
    }

    /**
     * Write websocket.
     *
     * @param ws the websocket
     * @param message the message (buffer)
     */
    public void writeWebSocket(final WebSocket ws, Buffer message) {
        if (!ws.writeQueueFull()) {
            ws.write(message);
        } else {
            ws.pause();
            ws.drainHandler(new VoidHandler() {
                @Override
                protected void handle() {
                    ws.resume();
                }
            });
        }

    }

    /**
     * Write websocket (server side).
     *
     * @param buffer the buffer
     */
    public void writeServerWebSocket(Buffer buffer) {
        if (serverWebSocket!=null) {
            if (!serverWebSocket.writeQueueFull()) {
                serverWebSocket.write(buffer);
            } else {
                serverWebSocket.pause();
                serverWebSocket.drainHandler(new VoidHandler() {
                    @Override
                    protected void handle() {
                        serverWebSocket.resume();
                    }
                });
            }
        }
    }

    /**
     * Gets the initial request time.
     *
     * @return the initial request time
     */
    public Long getInitialRequestTime() {
        return initialRequestTime;
    }

    /**
     * Sets the initial request time.
     *
     * @param initialRequestTime the initial request time
     * @return the backend web socket handler
     */
    public BackendWebSocketHandler setInitialRequestTime(Long initialRequestTime) {
        this.initialRequestTime = initialRequestTime;
        return this;
    }

    /**
     * Forward to backend.
     *
     * @param buffer the buffer
     */
    public void forwardToBackend(Buffer buffer) {
        messages.add(buffer);
        if (websocket!=null) {
            writeWebSocket(websocket, messages);
        }
    }

    /**
     * Close websocket.
     */
    public void closeWS() {
        frontendWSisClosed = true;
        if (! backendWSisClosed) {
            backendWSisClosed = true;
            websocket.close();
        }
    }

}
