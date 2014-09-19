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

public class DeleteMatcherHandler implements Handler<HttpServerRequest> {

    private final Logger log;
    private final String classId;
    private final Queue queue;

    public DeleteMatcherHandler(String id, final Logger log, final Queue queue) {
        this.log = log;
        this.classId = id;
        this.queue = queue;
    }

    @Override
    public void handle(final HttpServerRequest req) {
        final ServerResponse serverResponse = new ServerResponse(req, log, null, false);
        final ManagerService managerService = new ManagerService(classId, log);

        managerService.setRequest(req).setResponse(serverResponse);

        if (!managerService.checkMethodOk("DELETE") ||
                !managerService.checkUriOk() ||
                !managerService.checkIdPresent()) {
            return;
        }

        req.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer body) {
                String bodyStr = body.toString();
                String id = "";
                SafeJsonObject bodyJson = new SafeJsonObject(body.toString());
                if (req.params()!=null) {
                    id = req.params().contains("param1") ? req.params().get("param1") : "";
                }

                if (!managerService.checkIdConsistency(bodyJson, id)) {
                    return;
                }
                String uri = req.uri();
                int statusCode = managerService.statusFromMessageSchema(bodyStr, uri);

                if (statusCode==HttpCode.Ok) {
                    queue.queueToDel(bodyJson, uri);
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
