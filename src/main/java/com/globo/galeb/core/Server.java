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

import static com.globo.galeb.core.Constants.CONF_PORT;

import com.globo.galeb.metrics.ICounter;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Class Server.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class Server {

    /** The http server name. */
    private static String httpServerName = "SERVER";

    /** The vertx. */
    private final Vertx vertx;

    /** The conf. */
    private final JsonObject conf;

    /** The log. */
    private final Logger log;

    /** The http server. */
    private final HttpServer httpServer;

    /** The port. */
    private Integer port = 9000;

    /**
     * Gets the http server name.
     *
     * @return the http server name
     */
    public static String getHttpServerName() {
        return httpServerName;
    }

    /**
     * Sets the http server name.
     *
     * @param httpServerName the new http server name
     */
    public static void setHttpServerName(String httpServerName) {
        Server.httpServerName = httpServerName;
    }

    /**
     * Instantiates a new server.
     *
     * @param vertx the vertx
     * @param container the container
     * @param counter the counter
     */
    public Server(final Vertx vertx, final Container container, final ICounter counter) {
        this.vertx = vertx;
        this.conf = container.config();
        this.log = container.logger();
        this.httpServer = vertx.createHttpServer();

        if (this.conf.containsField("serverTCPKeepAlive")) {
            this.httpServer.setTCPKeepAlive(this.conf.getBoolean("serverTCPKeepAlive",true));
        }
        if (this.conf.containsField("serverReceiveBufferSize")) {
            this.httpServer.setReceiveBufferSize(this.conf.getInteger("serverReceiveBufferSize"));
        }
        if (this.conf.containsField("serverSendBufferSize")) {
            this.httpServer.setSendBufferSize(this.conf.getInteger("serverSendBufferSize"));
        }
        if (this.conf.containsField("serverAcceptBacklog")) {
            this.httpServer.setAcceptBacklog(this.conf.getInteger("serverAcceptBacklog"));
        }
    }

    /**
     * Start server (call listen).
     *
     * @param caller the caller
     * @return the server
     */
    public Server start(final Object caller) {

        this.port = conf.getInteger(CONF_PORT, port);

        try {
        httpServer.listen(port, new Handler<AsyncResult<HttpServer>>() {
                @Override
                public void handle(AsyncResult<HttpServer> asyncResult) {
                    if (asyncResult.succeeded()) {
                        log.info(String.format("[%s] Server listen: %d/tcp", caller.toString(), port));
                        EventBus eb = vertx.eventBus();
                        eb.publish("init.server", String.format("{ \"id\": \"%s\", \"status\": \"started\" }", caller.toString()));
                    } else {
                        log.fatal(String.format("[%s] Could not start server port: %d/tcp", caller.toString(), port));
                    }
                }
            });
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            log.debug(e.getStackTrace());
        }
        return this;
    }

    /**
     * Sets the http server request handler.
     *
     * @param httpServerRequestHandler the http server request handler
     * @return the server
     */
    public Server setHttpServerRequestHandler(final Handler<HttpServerRequest> httpServerRequestHandler) {
        httpServer.requestHandler(httpServerRequestHandler);
        return this;
    }

    /**
     * Sets the websocket server request handler.
     *
     * @param websocketServerRequestHandler the websocket server request handler
     * @return the server
     */
    public Server setWebsocketServerRequestHandler(final Handler<ServerWebSocket> websocketServerRequestHandler) {
        httpServer.websocketHandler(websocketServerRequestHandler);
        return this;
    }

    /**
     * Sets the default server port.
     *
     * @param defaultPort the default port
     * @return the server
     */
    public Server setDefaultPort(Integer defaultPort) {
        this.port = defaultPort;
        return this;
    }

}
