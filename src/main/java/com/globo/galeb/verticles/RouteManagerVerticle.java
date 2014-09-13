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
package com.globo.galeb.verticles;

import java.io.UnsupportedEncodingException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.globo.galeb.core.HttpCode;
import com.globo.galeb.core.IEventObserver;
import com.globo.galeb.core.QueueMap;
import com.globo.galeb.core.QueueMap.ACTION;
import com.globo.galeb.core.Server;
import com.globo.galeb.core.ServerResponse;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.exceptions.RouterException;
import com.globo.galeb.metrics.CounterWithStatsd;
import com.globo.galeb.metrics.ICounter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class RouteManagerVerticle extends Verticle implements IEventObserver {
    private static String routeManagerId = "route_manager";
    private final Map<String, Virtualhost> virtualhosts = new HashMap<>();
    private Logger log;
    private Server server;
    private Long version = 0L;

    private enum UriSupported {
        VIRTUALHOST,
        BACKEND
    }

    @Override
    public void start() {
        log = container.logger();
        final JsonObject conf = container.config();
        final ICounter counter = new CounterWithStatsd(conf, vertx, log);
        server = new Server(vertx, container, counter);

        startHttpServer(conf);
        new QueueMap(this, virtualhosts).register();

        log.info(String.format("Instance %s started", this.toString()));
    }

    @Override
    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public void postAddEvent(String message) {
        return;
    };

    @Override
    public void postDelEvent(String message) {
        return;
    };

    private long getVersion() {
        return this.version;
    }

    // TODO: REFACTOR THIS, PLEASE
    private void startHttpServer(final JsonObject serverConf) throws RuntimeException {
        final EventBus eb = this.getVertx().eventBus();
        final Logger log = this.getContainer().logger();
        final Server server = this.server;

        RouteMatcher routeMatcher = new RouteMatcher();

        //        @Deprecated
        routeMatcher.get("/route", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req, log, null, false);
                serverResponse.setStatusCode(HttpCode.Ok)
                    .setMessage(getRoutes().encodePrettily())
                    .setId(routeManagerId)
                    .end();
                log.info("GET /route");
            }
        });

        //      @Deprecated
        routeMatcher.get("/version",new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                JsonObject versionJson = new JsonObject(String.format("{\"version\":%d}", getVersion()));

                final ServerResponse serverResponse = new ServerResponse(req, log, null, false);
                serverResponse.setStatusCode(HttpCode.Ok)
                    .setMessage(versionJson.encodePrettily())
                    .setId(routeManagerId)
                    .end();
                log.info(String.format("GET /version: %d", getVersion()));
            }
        });

        // VirtualHost       @Deprecated
        routeMatcher.delete("/virtualhost", virtualhostHandlerAction(ACTION.DEL)); // ALL
        routeMatcher.delete("/virtualhost/:id", virtualhostHandlerAction(ACTION.DEL)); // Only ID

        routeMatcher.get("/virtualhost", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req, log, null, false);
                serverResponse.setStatusCode(HttpCode.Ok)
                    .setMessage(getVirtualHosts(""))
                    .setId(routeManagerId)
                    .end();
                log.info("GET /virtualhost");
            }
        });

        //      @Deprecated
        routeMatcher.get("/virtualhost/:id", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                String virtualhost = req.params() != null && req.params().contains("id") ? req.params().get("id"): "";

                final ServerResponse serverResponse = new ServerResponse(req, log, null, false);
                serverResponse.setStatusCode(HttpCode.Ok)
                    .setMessage(getVirtualHosts(virtualhost))
                    .setId(routeManagerId)
                    .end();
                log.info(String.format("GET /virtualhost/%s", virtualhost));
            }
        });

        //      @Deprecated
        routeMatcher.delete("/backend/:id", backendHandlerAction(eb, log, ACTION.DEL)); // Only with ID

        routeMatcher.post("/:uriBase", postMethodHandler());

        // Others methods/uris/etc
        routeMatcher.noMatch(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req, log, null, false);
                int statusCode = HttpCode.BadRequest;
                serverResponse.setStatusCode(statusCode)
                    .setMessage(HttpCode.getMessage(statusCode, true))
                    .setId(routeManagerId)
                    .end();
                log.warn(String.format("%s %s not supported", req.method(), req.uri()));
            }
        });

        server.setDefaultPort(9090).setHttpServerRequestHandler(routeMatcher).start(this);
    }

    private Handler<HttpServerRequest> backendHandlerAction(final EventBus eb, final Logger log, final QueueMap.ACTION action) {
        return new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req, log, null, false);

                String[] backendWithPort = null;
                try {
                    backendWithPort = req.params() != null && req.params().contains("id") ?
                            java.net.URLDecoder.decode(req.params().get("id"), "UTF-8").split(":") : null;
                } catch (UnsupportedEncodingException e) {}

                final String backend = backendWithPort != null ? backendWithPort[0]:"";
                final String port = backendWithPort != null ? backendWithPort[1]:"";
                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        int statusCode = HttpCode.Ok;
                        try {
                            final JsonObject json = new JsonObject(body.toString());
                            String jsonVirtualHost = json.containsField("id") ? json.getString("id") : "";
                            if (action.equals(ACTION.DEL)) {
                                JsonArray backends = json.containsField("backends") ? json.getArray("backends"): null;
                                if (backends!=null && !backends.toList().isEmpty() && !backends.get(0).equals(new JsonObject(String.format("{\"host\":\"%s\",\"port\":%s}", backend, port)))) {
                                    throw new RouterException("Backend not found");
                                }
                            }
                            if ("".equals(jsonVirtualHost)) {
                                throw new RouterException("Virtualhost name null");
                            }
                            setRoute(json, action, req.uri());
                        } catch (Exception e) {
                            log.error(String.format("backendHandlerAction FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            statusCode = HttpCode.BadRequest;
                        } finally {
                            serverResponse.setStatusCode(statusCode)
                                .setMessage(HttpCode.getMessage(statusCode, true))
                                .setId(routeManagerId)
                                .end();
                        }
                    }
                });
            }
        };
    }

    private String getVirtualHosts(String virtualhost) {
        if (!"".equals(virtualhost)) {
            JsonArray routes = getRoutes().getArray("routes");
            Iterator<Object> it = routes.iterator();
            while (it.hasNext()) {
                JsonObject route = (JsonObject) it.next();
                if (route.getString("id").equalsIgnoreCase(virtualhost)) {
                    return route.encodePrettily();
                }
            }
            return new JsonObject("{}").encodePrettily();
        }
        return getRoutes().encodePrettily();
    }

    private Handler<HttpServerRequest> virtualhostHandlerAction(final QueueMap.ACTION action) {
        final Logger log = this.getContainer().logger();

        return new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req, log, null, false);

                String virtualhostRequest = "";
                try {
                    virtualhostRequest = req.params() != null && req.params().contains("id") ?
                            java.net.URLDecoder.decode(req.params().get("id"), "UTF-8") : "";
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage());
                }
                final String virtualhost = virtualhostRequest;
                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        int statusCode = HttpCode.Ok;
                        try {
                            final JsonObject json = new JsonObject(body.toString());
                            String jsonVirtualHost = json.containsField("id") ? json.getString("id") : "";
                            if ("".equals(jsonVirtualHost)) {
                                throw new RouterException("Virtualhost name null");
                            }
                            if (action.equals(ACTION.DEL) && !jsonVirtualHost.equals(virtualhost) && "".equals(virtualhost)) {
                                throw new RouterException("Virtualhost: inconsistent reference");
                            }

                            setRoute(json, action, req.uri());

                        } catch (Exception e) {
                            log.error(String.format("virtualHostHandlerAction FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            statusCode = HttpCode.BadRequest;
                        } finally {
                            serverResponse.setStatusCode(statusCode)
                                .setMessage(HttpCode.getMessage(statusCode, true))
                                .setId(routeManagerId)
                                .end();
                        }
                    }
                });
            }
        };
    }

    private Handler<HttpServerRequest> postMethodHandler() {

        return new Handler<HttpServerRequest>() {

            @Override
            public void handle(final HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req, log, null, false);

                if (!checkMethodOk(req, serverResponse, "POST") || !checkUriOk(req, serverResponse)) {
                    return;
                }

                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        String bodyStr = body.toString();
                        int statusCode = postMessageStatus(bodyStr);

                        if (statusCode==HttpCode.Ok) {
                            setRoute(new JsonObject(bodyStr), ACTION.ADD, req.uri());
                        }

                        serverResponse.setStatusCode(statusCode)
                            .setMessage(HttpCode.getMessage(statusCode, true))
                            .setId(routeManagerId)
                            .end();
                    }
                });

            }
        };
    }

    public int postMessageStatus(String message) {
        return postMessageStatus(message, true);
    }

    public int postMessageStatus(String message, boolean registerLog) {

        JsonObject json = new JsonObject();
        JsonArray jsonRoutes = new JsonArray();
        String key = "";

        try {
            json = new JsonObject(message);
        } catch (DecodeException ex) {
            if (registerLog) log.error(String.format("Json decode error: %s", message));
            return HttpCode.BadRequest;
        }
        if (json.containsField("routes")) {
            try {
                jsonRoutes = json.getArray("routes");
            } catch (DecodeException e) {
                if (registerLog) log.error(String.format("Routes has to be ARRAY: %s", message));
                return HttpCode.BadRequest;
            }
        } else {
            if (registerLog) log.error(String.format("Routes not found: %s", message));
            return HttpCode.BadRequest;
        }

        for (Object routeObj: jsonRoutes) {
            JsonObject routeJson = (JsonObject) routeObj;
            if (!routeJson.containsField("id")) {
                if (registerLog) log.error(String.format("ID not found: %s", routeJson.toString()));
                return HttpCode.BadRequest;
            } else {
                key = routeJson.getString("id");
                if ("".equals(key)) {
                    if (registerLog) log.error(String.format("ID is invalid: %s", routeJson.toString()));
                    return HttpCode.BadRequest;
                }
            }
        }
        return HttpCode.Ok;
    }

    private String getRequestId(HttpServerRequest req) {
        String id = "";
        try {
            id = req.params() != null && req.params().contains("id") ?
                    java.net.URLDecoder.decode(req.params().get("id"), "UTF-8") : "";
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        return id;
    }

    private boolean checkIdPresent(final ServerResponse serverResponse, String id) {
        if ("".equals(id)) {
            endResponse(serverResponse, HttpCode.BadRequest, String.format("ID absent", id));
            return false;
        }
        return true;
    }

    private boolean checkIdAbsent(final ServerResponse serverResponse, String id) {
        if (!"".equals(id)) {
            endResponse(serverResponse, HttpCode.BadRequest, String.format("ID %s not supported", id));
            return false;
        }
        return true;
    }

    private boolean checkUriOk(final HttpServerRequest req, final ServerResponse serverResponse) {
        String uriBase = "";
        try {
            uriBase = req.params() != null && req.params().contains("uriBase") ?
                    java.net.URLDecoder.decode(req.params().get("uriBase"), "UTF-8") : "";
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
            endResponse(serverResponse, HttpCode.BadRequest, e.getMessage());
            return false;
        }
        for (UriSupported uriEnum : EnumSet.allOf(UriSupported.class)) {
            if (uriBase.equals(uriEnum.toString().toLowerCase())) {
                return true;
            }
        }
        endResponse(serverResponse, HttpCode.BadRequest, String.format("URI /%s not supported", uriBase));
        return false;
    }

    private boolean checkMethodOk(HttpServerRequest req, ServerResponse serverResponse, String string) {
        String method = req.method();
        if (!"POST".equalsIgnoreCase(method)) {
            endResponse(serverResponse, HttpCode.MethotNotAllowed, "Method Not Allowed");
            return false;
        }
        return true;
    }

    public void setRoute(final JsonObject json, final QueueMap.ACTION action, final String uri) throws RuntimeException {
        JsonArray jsonRoutes = null;
        Long timestamp = 0L;
        jsonRoutes = json.getArray("routes");
        timestamp = json.getLong("version");

        Iterator<Object> it = jsonRoutes.iterator();
        while (it.hasNext()) {
            String vhost_id;
            JsonObject properties;
            String hostWithPort;
            JsonArray backends = null;
            JsonObject jsonTemp = (JsonObject) it.next();

            if (jsonTemp.containsField("id")) {
                vhost_id = jsonTemp.getString("id");
            } else {
                throw new RouterException("virtualhost id undef");
            }
            if (jsonTemp.containsField("properties")) {
                try {
                    properties = jsonTemp.getObject("properties");
                } catch (DecodeException e) {
                    properties = new JsonObject();
                }
            } else {
                properties = new JsonObject();
            }
            if (jsonTemp.containsField("backends") && jsonTemp.getArray("backends").size()>0) {
                backends = jsonTemp.getArray("backends");
                Iterator<Object> backendsIterator = backends.iterator();
                while (backendsIterator.hasNext()) {
                    JsonObject backendJson = (JsonObject) backendsIterator.next();
                    hostWithPort = backendJson.containsField("id") ? backendJson.getString("id"):"";
                    if ("".equals(hostWithPort)) {
                        throw new RouterException("Backend undef");
                    }
                    String message = QueueMap.buildMessage(vhost_id,
                                                           backendJson.encode(),
                                                           uri,
                                                           properties.toString());
                    sendAction(message, action);
                }
            } else {
                String message = QueueMap.buildMessage(vhost_id, "{}", uri, properties.toString());
                sendAction(message, action);
            }

        }
        sendAction(String.format("%d", timestamp), ACTION.SET_VERSION);
    }

    private boolean endResponse(final ServerResponse serverResponse, int statusCode, String message) {
        if (statusCode < HttpCode.BadRequest) {
            log.info(message);
        } else {
            log.warn(message);
        }
        boolean isOk = true;
        try {
        serverResponse.setStatusCode(statusCode)
            .setMessage(HttpCode.getMessage(statusCode, true))
            .setId(routeManagerId)
            .end();
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            isOk = false;
        }
        return isOk;
    }

    private void sendAction(String message, final QueueMap.ACTION action) {
        final EventBus eb = this.getVertx().eventBus();
        final Logger log = this.getContainer().logger();

        eb.publish(action.toString(), message);
        log.debug(String.format("Sending %s to %s",message, action.toString()));

    }

     private JsonObject getRoutes() {
        JsonObject routes = new JsonObject();
        routes.putNumber("version", getVersion());
        JsonArray vhosts = new JsonArray();

        for (String vhost : virtualhosts.keySet()) {
            Virtualhost virtualhost = virtualhosts.get(vhost);
            if (virtualhost==null) {
                continue;
            }
            vhosts.add(virtualhost.toJson());
        }
        routes.putArray("routes", vhosts);
        return routes;
    }

}
