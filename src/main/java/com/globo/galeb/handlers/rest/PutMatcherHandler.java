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

import com.globo.galeb.core.HttpCode;
import com.globo.galeb.core.ManagerService;
import com.globo.galeb.core.SafeJsonObject;
import com.globo.galeb.core.ServerResponse;
import com.globo.galeb.core.bus.Queue;

public class PutMatcherHandler implements Handler<HttpServerRequest> {

    private final Logger log;
    private final Queue queue;
    private final String classId;

    public PutMatcherHandler(String id, final Logger log, final Queue queue) {
        this.log = log;
        this.queue = queue;
        this.classId = id;
    }

    @Override
    public void handle(final HttpServerRequest req) {
        final ServerResponse serverResponse = new ServerResponse(req, log, null, false);
        final ManagerService managerService = new ManagerService(classId, log);

        managerService.setRequest(req).setResponse(serverResponse);

        if (!managerService.checkMethodOk("PUT") ||
            !managerService.checkUriOk() ||
            !managerService.checkIdPresent()) {
            return;
        }

        req.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer body) {
                String bodyStr = body.toString();
                String uri = req.uri();
                String id = "";

                if (req.params()!=null) {
                    id = req.params().contains("param1") ? req.params().get("param1") : "";
                }

                SafeJsonObject bodyJson = new SafeJsonObject(body.toString());

                if (!managerService.checkIdConsistency(bodyJson, id)) {
                    return;
                }

                int statusCode = managerService.statusFromMessageSchema(bodyStr, uri);

                if (statusCode==HttpCode.Ok) {
                    queue.queueToChange(bodyJson, uri);
                    statusCode = HttpCode.Accepted;
                }

                serverResponse.setStatusCode(statusCode)
                    .setMessage(HttpCode.getMessage(statusCode, true))
                    .setId(classId)
                    .end();
            }
        });

    }
}
