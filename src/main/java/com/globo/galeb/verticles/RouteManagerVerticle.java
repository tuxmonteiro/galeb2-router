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
import com.globo.galeb.core.MessageBus;
import com.globo.galeb.core.QueueMap;
import com.globo.galeb.core.Serializable;
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

        routeMatcher.post("/:uriBase", postMethodHandler());
        routeMatcher.delete("/:uriBase/:id", deleteMethodWithIdHandler());

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
                            String jsonVirtualHost = json.containsField(Serializable.jsonIdFieldName) ?
                                    json.getString(Serializable.jsonIdFieldName) : "";
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
                if (route.getString(Serializable.jsonIdFieldName).equalsIgnoreCase(virtualhost)) {
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
                            String jsonVirtualHost = json.containsField(Serializable.jsonIdFieldName) ?
                                    json.getString(Serializable.jsonIdFieldName) : "";
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
                        int statusCode = statusFromMessageSchema(bodyStr, req.uri());

                        if (statusCode==HttpCode.Ok) {
                            sendMessageToBus(new JsonObject(bodyStr), ACTION.ADD, req.uri());
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

    private Handler<HttpServerRequest> deleteMethodWithIdHandler() {

        return new Handler<HttpServerRequest>() {

            @Override
            public void handle(final HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req, log, null, false);
                final String id = getRequestId(req);
                if (!checkMethodOk(req, serverResponse, "DELETE") ||
                        !checkUriOk(req, serverResponse) ||
                        !checkIdPresent(serverResponse, id)) {
                    return;
                }

                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        String bodyStr = body.toString();
                        JsonObject bodyJson = jsonIsOk(bodyStr) ?
                                new JsonObject(body.toString()) : new JsonObject();

                        if (!checkIdConsistency(serverResponse, bodyJson, id)) {
                            return;
                        }
                        int statusCode = statusFromMessageSchema(bodyStr, req.uri());

                        if (statusCode==HttpCode.Ok) {
                            sendMessageToBus(bodyJson, ACTION.DEL, req.uri());
                            statusCode = HttpCode.Accepted;
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

    public int statusFromMessageSchema(String message, String uri) {
        return statusFromMessageSchema(message, uri, true);
    }

    public int statusFromMessageSchema(String message, String uri, boolean registerLog) {

        String key = "";

        if (!jsonIsOk(message)) {
            if (registerLog) log.error(String.format("Json decode error: %s", message));
            return HttpCode.BadRequest;
        }

        JsonObject json = new JsonObject(message);

        if (!json.containsField("version")) {
            if (registerLog) log.error(String.format("Version is mandatory: %s", message));
            return HttpCode.BadRequest;
        }

        if (!uri.startsWith("/route")) {
            if (!json.containsField(Serializable.jsonIdFieldName)) {
                if (registerLog) log.error(String.format("ID is mandatory: %s", message));
                return HttpCode.BadRequest;
            }
        } else {
            JsonArray jsonRoutes = new JsonArray();

            if (!hasRoutes(json)) {
                if (registerLog) log.error(String.format("Reading routes failed: %s", message));
                return HttpCode.BadRequest;
            } else {
                jsonRoutes = json.getArray("routes");
            }

            for (Object routeObj: jsonRoutes) {
                JsonObject routeJson = (JsonObject) routeObj;
                if (!routeJson.containsField(Serializable.jsonIdFieldName)) {
                    if (registerLog) log.error(String.format("ID not found: %s", routeJson.toString()));
                    return HttpCode.BadRequest;
                } else {
                    key = routeJson.getString(Serializable.jsonIdFieldName);
                    if ("".equals(key)) {
                        if (registerLog) log.error(String.format("ID is invalid: %s", routeJson.toString()));
                        return HttpCode.BadRequest;
                    }
                }
            }
        }
        return HttpCode.Ok;
    }

    private String getRequestId(HttpServerRequest req) {
        String id = "";
        try {
            id = req.params() != null && req.params().contains(Serializable.jsonIdFieldName) ?
                    java.net.URLDecoder.decode(req.params().get(Serializable.jsonIdFieldName), "UTF-8") : "";
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

    private boolean checkIdConsistency(final ServerResponse serverResponse, JsonObject entityJson, String idFromUri) {
        String idFromJson = entityJson.getString(Serializable.jsonIdFieldName, "");
        if ("".equals(idFromJson) || "".equals(idFromUri) || !idFromJson.equals(idFromUri)) {
            endResponse(serverResponse, HttpCode.BadRequest, String.format("IDs inconsistents: bodyId(%s) not equal uriId(%s)", idFromJson, idFromUri));
            return false;
        }
        return true;
    }

    private boolean jsonIsOk(String message) {
        boolean jsonOk = false;
        try {
            new JsonObject(message);
            jsonOk = true;
        } catch (DecodeException ignore) {
            // jsonOk = false
        }
        return jsonOk;
    }

    private boolean hasRoutes(final JsonObject jsonMessage) {
        boolean routesOk = false;
        if (jsonMessage.containsField("routes")) {
            try {
                jsonMessage.getArray("routes");
                routesOk = true;
            } catch (DecodeException isNotArray) {
                // routesOk = false // because ins't array
            }
        } // else { routesOk = false // because not exist }
        return routesOk;
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

    private boolean checkMethodOk(HttpServerRequest req, ServerResponse serverResponse, String methodId) {
        String method = req.method();
        if (!methodId.equalsIgnoreCase(method)) {
            endResponse(serverResponse, HttpCode.MethotNotAllowed, "Method Not Allowed");
            return false;
        }
        return true;
    }

    public void sendMessageToBus(JsonObject json, QueueMap.ACTION action, final String uri) {

        Long timestamp = 0L;
        try {
            timestamp = json.getLong("version");
        } catch (DecodeException e) {
            log.error(e.getMessage());
            return;
        }
        json.removeField("version");

        String parentId = json.getString(MessageBus.parentIdFieldName, "");
        json.removeField(parentId);

        MessageBus messageBus = new MessageBus()
                                    .setUri(uri)
                                    .setEntity(json.encode());

        if (!"".equals(parentId)) {
            messageBus.setParentId(parentId);
        }

        String message = messageBus.make().toString();

        sendAction(message, action);
        sendAction(String.format("%d", timestamp), ACTION.SET_VERSION);


    }

    public void setRoute(final JsonObject json, final QueueMap.ACTION action, final String uri) throws RuntimeException {
        JsonArray jsonRoutes = null;
        Long timestamp = 0L;
        jsonRoutes = json.getArray("routes");
        timestamp = json.getLong("version");

        Iterator<Object> it = jsonRoutes.iterator();
        while (it.hasNext()) {
            String vhost_id;
            String hostWithPort;
            JsonArray backends = null;
            JsonObject jsonTemp = (JsonObject) it.next();

            if (jsonTemp.containsField(Serializable.jsonIdFieldName)) {
                vhost_id = jsonTemp.getString(Serializable.jsonIdFieldName);
            } else {
                throw new RouterException("virtualhost id undef");
            }
            JsonObject vhost_properties = jsonTemp.getObject(Serializable.jsonPropertiesFieldName, new JsonObject());
            JsonObject vhostJson = new JsonObject()
                                            .putString(Serializable.jsonIdFieldName, vhost_id)
                                            .putString(Serializable.jsonPropertiesFieldName, vhost_properties.encode());
            if (jsonTemp.containsField(Virtualhost.backendsFieldName) && jsonTemp.getArray(Virtualhost.backendsFieldName).size()>0) {
                backends = jsonTemp.getArray(Virtualhost.backendsFieldName);
                Iterator<Object> backendsIterator = backends.iterator();
                while (backendsIterator.hasNext()) {
                    JsonObject backendJson = (JsonObject) backendsIterator.next();
                    hostWithPort = backendJson.containsField(Serializable.jsonIdFieldName) ? backendJson.getString(Serializable.jsonIdFieldName):"";
                    if ("".equals(hostWithPort)) {
                        throw new RouterException("Backend undef");
                    }
                    String message = new MessageBus()
                                            .setEntity(backendJson)
                                            .setParentId(vhostJson.getString(Serializable.jsonIdFieldName))
                                            .setUri(uri)
                                            .make()
                                            .toString();
                    sendAction(message, action);
                }
            } else {
                String message = new MessageBus()
                                        .setEntity(vhostJson)
                                        .setUri(uri)
                                        .make()
                                        .toString();
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
