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
package com.globo.galeb.handlers.rest;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.core.Farm;
import com.globo.galeb.core.HttpCode;
import com.globo.galeb.core.ManagerService;
import com.globo.galeb.core.SafeJsonObject;
import com.globo.galeb.core.ServerResponse;

public class PostMatcherHandler implements Handler<HttpServerRequest> {

    private final Logger log;
    private final Farm farm;
    private final String classId;

    public PostMatcherHandler(String id, final Logger log, final Farm farm) {
        this.log = log;
        this.farm = farm;
        this.classId = id;
    }

    @Override
    public void handle(final HttpServerRequest req) {
        final ServerResponse serverResponse = new ServerResponse(req, log, null, false);
        final ManagerService managerService = new ManagerService(classId, log);

        managerService.setRequest(req).setResponse(serverResponse);

        if (!managerService.checkMethodOk("POST") || !managerService.checkUriOk()) {
            return;
        }

        req.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer body) {
                String bodyStr = body.toString();
                String uri = req.uri();
                int statusCode = managerService.statusFromMessageSchema(bodyStr, uri);

                if (statusCode==HttpCode.Ok) {
                    if (uri.startsWith("/farm")) {
                        farm.getQueueMap().sendGroupActionAdd(new SafeJsonObject(bodyStr), uri);
                    } else {
                        farm.getQueueMap().sendActionAdd(new SafeJsonObject(bodyStr), uri);
                    }
                }

                serverResponse.setStatusCode(statusCode)
                    .setMessage(HttpCode.getMessage(statusCode, true))
                    .setId(classId)
                    .end();
            }
        });

    }
}
