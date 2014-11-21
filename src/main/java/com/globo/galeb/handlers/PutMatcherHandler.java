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
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.rulereturn.HttpCode;
import com.globo.galeb.core.server.ManagerService;
import com.globo.galeb.core.server.ServerResponse;

/**
 * Class PutMatcherHandler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class PutMatcherHandler implements Handler<HttpServerRequest> {

    /** The logger. */
    private final Logger log;

    /** The queue service. */
    private final IQueueService queueService;

    /** The class id. */
    private final String classId;

    /**
     * Instantiates a new put matcher handler.
     *
     * @param id the id from uri
     * @param log the logger
     * @param queueService the queue service
     */
    public PutMatcherHandler(String id, final Logger log, final IQueueService queueService) {
        this.log = log;
        this.queueService = queueService;
        this.classId = id;
    }

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(final HttpServerRequest req) {
        final ServerResponse serverResponse = new ServerResponse(req).setLog(log);
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

                JsonObject bodyJson = new JsonObject(bodyStr);

                if (!managerService.checkIdConsistency(bodyJson, id)) {
                    return;
                }

                int statusCode = managerService.statusFromMessageSchema(bodyStr, uri);

                if (statusCode==HttpCode.OK) {
                    queueService.queueToChange(bodyJson, uri);
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
