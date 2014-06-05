/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas;

import static lbaas.Constants.CONF_PORT;
import io.netty.handler.codec.http.HttpResponseStatus;
import lbaas.exceptions.BadRequestException;
import lbaas.verticles.StatsDClient;
import lbaas.verticles.StatsDClient.TypeStatsdMessage;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

public class Server {

    private final Vertx vertx;
    private final JsonObject conf;
    private final Logger log;
    private final StatsDClient statsdClient;

    public Server(final Vertx vertx, final Container container) {
        this.vertx = vertx;
        this.conf = container.config();
        this.log = container.logger();
        String statsdHost = conf.getString("StatsdHost","127.0.0.1");
        Integer statsdPort = conf.getInteger("statsdPort", 8125);
        this.statsdClient = new StatsDClient(statsdHost, statsdPort);
    }

    public void start(
            final Object caller,
            final Handler<HttpServerRequest> handlerHttpServerRequest,
            final Integer defaultPort) {
        Integer port = conf.getInteger(CONF_PORT,defaultPort);
        vertx.createHttpServer().requestHandler(handlerHttpServerRequest)
            .setTCPKeepAlive(conf.getBoolean("serverTCPKeepAlive",true))
            .listen(port);
        log.info(String.format("[%s] Server listen: %d/tcp", caller.toString(), port));
    }

    public void showErrorAndClose(final HttpServerRequest req, final Throwable event) {

        if (event instanceof java.util.concurrent.TimeoutException) {
            returnStatus(req, 504);
        } else if (event instanceof BadRequestException) {
            returnStatus(req, 400);
        } else {
            returnStatus(req, 502);
        }

        close(req);
    }

    public void close(final HttpServerRequest req) {
        try {
            req.response().close();
        } catch (RuntimeException e) {
            // Ignore already closed
            return;
        }
    }


    public void returnStatus(final HttpServerRequest req, Integer code) {
        returnStatus(req, code, "");
    }

    public void returnStatus(final HttpServerRequest req, Integer code, String message) {
        req.response().setStatusCode(code);
        req.response().setStatusMessage(HttpResponseStatus.valueOf(code).reasonPhrase());
        String messageReturn = message;
        if (message != null) {
            if ("".equals(message)) {
                req.response().headers().set("Content-Type", "application/json");
                JsonObject json = new JsonObject(String.format("{ \"status_message\":\"%s\"}", req.response().getStatusMessage()));
                messageReturn = json.encodePrettily();
            }
            try {
                req.response().end(messageReturn);
            } catch (java.lang.IllegalStateException e) {
                // Response has already been written ?
                return;
            }
        } else {
            try {
                req.response().end();
            } catch (java.lang.IllegalStateException e) {
                // Response has already been written ?
                return;
            }
        }
        if (conf.getBoolean("enableStatsd",false)) {
            String virtualhost = req.headers().get("Host").split(":")[0];
            statsdClient.sendStatsd(TypeStatsdMessage.COUNT, String.format("%s.httpCode%d:%d", virtualhost, code, 1));

        }
    }

}
