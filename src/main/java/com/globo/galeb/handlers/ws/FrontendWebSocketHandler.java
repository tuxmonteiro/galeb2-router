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

import java.util.Map.Entry;

import com.globo.galeb.core.Farm;
import com.globo.galeb.core.Virtualhost;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Class FrontendWebSocketHandler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class FrontendWebSocketHandler implements Handler<ServerWebSocket> {

//    /** The vertx. */
//    private final Vertx vertx;

    /** The logger. */
    private final Logger log;

    /** The farm. */
    private Farm farm;

    /** The http header host. */
    private final String httpHeaderHost = HttpHeaders.HOST.toString();

    /**
     * Instantiates a new frontend web socket handler.
     *
     * @param vertx the vertx
     * @param container the container
     * @param farm the farm
     */
    public FrontendWebSocketHandler(
            final Vertx vertx,
            final Container container,
            final Farm farm) {
//        this.vertx = vertx;
        this.farm = farm;
        this.log = container.logger();
    }

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(final ServerWebSocket serverWebSocket) {

        String hostnameWithPort = "";
        for (Entry<String, String> e: serverWebSocket.headers().entries()) {
            if (e.getKey().equalsIgnoreCase(httpHeaderHost)) {
                hostnameWithPort = e.getValue();
            }
        }
        log.info(String.format("Received request for host %s '%s'",
                hostnameWithPort, serverWebSocket.uri()));

        if ("".equals(hostnameWithPort)) {
            log.warn("Host UNDEF");
            serverWebSocket.close();
            return;
        }

        String hostname = hostnameWithPort.split(":")[0];

        final Virtualhost virtualserver = farm.getEntityById(hostname);

        if (virtualserver==null) {
            log.warn(String.format("Host: %s UNDEF", hostname));
            serverWebSocket.close();
            return;
        }

        if (virtualserver.getEntities().isEmpty()) {
            log.warn(String.format("Host %s without backends", hostname));
            serverWebSocket.close();
            return;
        }

//        final Backend backend = virtualserver.getChoice(new RequestData(serverWebSocket))
//                .setKeepAlive(true)
//                .setKeepAliveTimeOut(Long.MAX_VALUE)
//                .setKeepAliveMaxRequest(Long.MAX_VALUE)
//                .setConnectionTimeout(10000)
//                .setMaxPoolSize(10);

//        String backendId = backend.toString();
//        log.info(backend);
//
//        RemoteUser remoteUser = new RemoteUser(serverWebSocket.remoteAddress());
//
//        final HttpClient httpClient = backend.connect(remoteUser);
//        final BackendWebSocketHandler backendWebSocketHandler =
//                new BackendWebSocketHandler(vertx, log, backendId, serverWebSocket);

//        httpClient.connectWebsocket(serverWebSocket.uri(),
//                WebSocketVersion.RFC6455, serverWebSocket.headers(), backendWebSocketHandler);
//
//        serverWebSocket.dataHandler(new Handler<Buffer>() {
//
//            @Override
//            public void handle(Buffer buffer) {
//                backendWebSocketHandler.forwardToBackend(buffer);
//            }
//
//        });
//
//        serverWebSocket.closeHandler(new Handler<Void>() {
//            @Override
//            public void handle(Void event) {
//                log.debug("Frontend WebSocket was closed");
//                backendWebSocketHandler.closeWS();
//            }
//        });

    }
}
