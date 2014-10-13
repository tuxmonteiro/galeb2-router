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

import java.io.UnsupportedEncodingException;
import java.util.EnumSet;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

public class ManagerService {

    public enum UriSupported {
        VIRTUALHOST,
        BACKEND,
        FARM,
        VERSION
    }

    private final String id;
    private final Logger log;
    private HttpServerRequest req = null;
    private ServerResponse serverResponse = null;

    public ManagerService(String id, final Logger log) {
        this.id = id;
        this.log = log;
    }

    public ManagerService setRequest(final HttpServerRequest req) {
        this.req = req;
        return this;
    }

    public ManagerService setResponse(final ServerResponse resp) {
        this.serverResponse = resp;
        return this;
    }

    public boolean checkUriOk() {
        String uriBase = "";
        try {
            if (req.params() != null) {
                uriBase =  req.params().contains("uriBase") ? req.params().get("uriBase") :
                    req.params().contains("param0") ? req.params().get("param0") : "";
                uriBase = java.net.URLDecoder.decode(uriBase, "UTF-8");
            } else {
                log.error("UriBase is null");
                endResponse(HttpCode.NotFound, "UriBase is null");
                return false;
            }
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
            endResponse(HttpCode.NotFound, e.getMessage());
            return false;
        }
        for (UriSupported uriEnum : EnumSet.allOf(UriSupported.class)) {
            if (uriBase.equals(uriEnum.toString().toLowerCase())) {
                return true;
            }
        }
        endResponse(HttpCode.NotFound, String.format("URI /%s not supported", uriBase));
        return false;
    }

    public boolean checkMethodOk(String methodId) {
        String method = req.method();
        if (!methodId.equalsIgnoreCase(method)) {
            endResponse(HttpCode.MethotNotAllowed, "Method Not Allowed");
            return false;
        }
        return true;
    }

    public boolean checkIdConsistency(JsonObject entityJson, String idFromUri) {
        String idFromJson = entityJson.getString(IJsonable.jsonIdFieldName, "");
        if ("".equals(idFromJson) || "".equals(idFromUri) || !idFromJson.equals(idFromUri)) {
            endResponse(HttpCode.BadRequest,
                    String.format("IDs inconsistents: bodyId(%s) not equal uriId(%s)", idFromJson, idFromUri));
            return false;
        }
        return true;
    }

    public String getRequestId() {
        String idFromUri = "";
        try {
            idFromUri = req.params() != null && req.params().contains(IJsonable.jsonIdFieldName) ?
                    java.net.URLDecoder.decode(req.params().get(IJsonable.jsonIdFieldName), "UTF-8") : "";
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        return idFromUri;
    }


    public boolean checkIdPresent() {
        if ("".equals(getRequestId())) {
            endResponse(HttpCode.BadRequest, "ID absent");
            return false;
        }
        return true;
    }

    public boolean endResponse(int statusCode, String message) {
        if (statusCode < HttpCode.BadRequest) {
            log.info(message);
        } else {
            log.warn(message);
        }
        boolean isOk = true;
        try {
            serverResponse.setStatusCode(statusCode)
                .setMessage(HttpCode.getMessage(statusCode, true))
                .setId(id)
                .end();
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            isOk = false;
        }
        return isOk;
    }

    public int statusFromMessageSchema(String message, String uri, boolean registerLog) {

        SafeJsonObject json = new SafeJsonObject(message);
        if ("{}".equals(json.encode())) {
            if (registerLog) {
                log.error(String.format("Json decode error: %s", message));
            }
            return HttpCode.BadRequest;
        }

        if (!json.containsField("version")) {
            if (registerLog) log.error(String.format("Version is mandatory: %s", message));
            return HttpCode.BadRequest;
        }

        if (!json.containsField(IJsonable.jsonIdFieldName)) {
            if (registerLog) log.error(String.format("ID is mandatory: %s", message));
            return HttpCode.BadRequest;
        }
        return HttpCode.Ok;
    }

    public int statusFromMessageSchema(String message, String uri) {
        return statusFromMessageSchema(message, uri, true);
    }

}
