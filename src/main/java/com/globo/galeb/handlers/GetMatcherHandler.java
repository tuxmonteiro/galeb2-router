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

import java.io.UnsupportedEncodingException;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.core.entity.impl.Farm;
import com.globo.galeb.core.rulereturn.HttpCode;
import com.globo.galeb.core.server.ManagerService;
import com.globo.galeb.core.server.Server;
import com.globo.galeb.core.server.ServerResponse;

/**
 * Class GetMatcherHandler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class GetMatcherHandler implements Handler<HttpServerRequest> {

    /** The log. */
    private final Logger log;

    /** The http server name. */
    private String httpServerName = null;

    /** The farm. */
    private final Farm farm;

    /** The class id. */
    private final String classId;

    /**
     * Instantiates a new gets the matcher handler.
     *
     * @param id the id from uri
     * @param log the logger
     * @param farm the farm
     */
    public GetMatcherHandler(String id, final Logger log, final Farm farm) {
        this.log = log;
        this.classId = id;
        this.farm = farm;
    }

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(HttpServerRequest req) {
        final ServerResponse serverResponse = new ServerResponse(req).setLog(log);
        ManagerService managerService = new ManagerService(classId, log).setRequest(req).setResponse(serverResponse);

        if (httpServerName==null) {
            httpServerName = req.headers().contains(HttpHeaders.HOST) ? req.headers().get(HttpHeaders.HOST) : "SERVER";
            Server.setHttpServerName(httpServerName);
        }

        if (!managerService.checkUriOk()) {
            return;
        }

        String uriBase = "";
        String id = "";
        String message = "";

        if (req.params()!=null) {
            uriBase = req.params().contains("param0") ? req.params().get("param0") : "";
            id = req.params().contains("param1") ? req.params().get("param1") : "";
        }

        try {
            uriBase = java.net.URLDecoder.decode(uriBase, "UTF-8");
            id = java.net.URLDecoder.decode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            serverResponse.setStatusCode(HttpCode.BAD_REQUEST)
                                .setId(id)
                                .endResponse();
            log.error("Unsupported Encoding");
            return;
        }

        switch (uriBase) {
            case "version":
                message = new JsonObject().putNumber("version", farm.getVersion()).encodePrettily();
                break;
            case "farm":
                message = farm.getFarmJson();
                break;
            case "virtualhost":
                message = farm.getVirtualhostJson(id);
                break;
            case "backend":
                message = farm.getBackendJson(id);
                break;
            default:
                message = "";
                break;
        }

        int statusCode = HttpCode.OK;
        if ("".equals(message)||"{}".equals(message)||"[ ]".equals(message)) {
            statusCode = HttpCode.NOT_FOUND;
            message = HttpCode.getMessage(statusCode, true);
        }

        serverResponse.setStatusCode(statusCode)
            .setMessage(message)
            .setId(id)
            .endResponse();
        log.info(String.format("GET /%s", uriBase));
    }

}
