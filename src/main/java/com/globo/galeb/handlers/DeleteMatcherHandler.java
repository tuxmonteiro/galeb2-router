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
package com.globo.galeb.handlers;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import com.globo.galeb.bus.IQueueService;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.rulereturn.HttpCode;
import com.globo.galeb.server.ManagerService;
import com.globo.galeb.server.ServerResponse;

/**
 * Class DeleteMatcherHandler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class DeleteMatcherHandler implements Handler<HttpServerRequest> {

    /** The log. */
    private final SafeLogger log;

    /** The class id. */
    private final String classId;

    /** The queue service. */
    private final IQueueService queueService;

    /**
     * Instantiates a new delete matcher handler.
     *
     * @param id the id from uri
     * @param log the logger
     * @param queueService the queue service
     */
    public DeleteMatcherHandler(String id, final SafeLogger log, final IQueueService queueService) {
        this.log = log;
        this.classId = id;
        this.queueService = queueService;
    }

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(final HttpServerRequest req) {
        final ServerResponse serverResponse = new ServerResponse(req).setLog(log);
        final ManagerService managerService = new ManagerService(classId, log);

        managerService.setRequest(req).setResponse(serverResponse);

        if (!managerService.checkMethodOk("DELETE") || !managerService.checkUriOk()) {
            return;
        }

        req.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer body) {
                String bodyStr = body.toString();
                String id = "";
                int statusCode = HttpCode.BAD_REQUEST;
                JsonObject bodyJson = new JsonObject();
                String uri = "";

                if ("".equals(bodyStr)) {
                    log.error("DELETE: body is null");
                } else {
                    bodyJson.mergeIn(new JsonObject(bodyStr));
                    if (req.params()!=null) {
                        id = req.params().contains("param1") ? req.params().get("param1") : "";
                    }

                    if (!"".equals(id) && !managerService.checkIdConsistency(bodyJson, id)) {
                        return;
                    }

                    uri = req.uri();
                    statusCode = managerService.statusFromMessageSchema(bodyStr, uri);
                }

                if (statusCode==HttpCode.OK) {
                    queueService.queueToDel(bodyJson, uri);
                    log.info(String.format("[%s] DEL %s : json '%s'", this.toString(), uri, bodyStr));
                    statusCode = HttpCode.ACCEPTED;
                }

                serverResponse.setStatusCode(statusCode)
                    .setMessage(HttpCode.getMessage(statusCode, true))
                    .setId(classId)
                    .endResponse();
            }
        });

    }

}
