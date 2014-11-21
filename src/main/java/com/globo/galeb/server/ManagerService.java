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
package com.globo.galeb.server;

import java.io.UnsupportedEncodingException;
import java.util.EnumSet;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.rulereturn.HttpCode;

/**
 * Class ManagerService.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class ManagerService {

    /**
     * Enum UriSupported.
     *
     * @author: See AUTHORS file.
     * @version: 1.0.0, Oct 23, 2014.
     */
    public enum UriSupported {

        /** The virtualhost. */
        VIRTUALHOST,

        /** The backend. */
        BACKEND,

        /** The farm. */
        FARM,

        /** The version. */
        VERSION,

        /** The backendpool. */
        BACKENDPOOL,

        /** The rule. */
        RULE
    }

    /** The manager service id. */
    private final String id;

    /** The logger. */
    private final Logger log;

    /** The httpServerRequest. */
    private HttpServerRequest req = null;

    /** The server response. */
    private ServerResponse serverResponse = null;

    /**
     * Instantiates a new manager service.
     *
     * @param id the id
     * @param log the log
     */
    public ManagerService(String id, final Logger log) {
        this.id = id;
        this.log = log;
    }

    /**
     * Sets the httpServerRequest.
     *
     * @param req the httpServerRequest
     * @return this
     */
    public ManagerService setRequest(final HttpServerRequest req) {
        this.req = req;
        return this;
    }

    /**
     * Sets the serverResponse.
     *
     * @param resp the serverResponse
     * @return this
     */
    public ManagerService setResponse(final ServerResponse resp) {
        this.serverResponse = resp;
        return this;
    }

    /**
     * Check if uri is ok.
     *
     * @return true, if successful
     */
    public boolean checkUriOk() {
        String uriBase = "";
        try {
            if (req.params() != null) {
                uriBase =  req.params().contains("uriBase") ? req.params().get("uriBase") :
                    req.params().contains("param0") ? req.params().get("param0") : "";
                uriBase = java.net.URLDecoder.decode(uriBase, "UTF-8");
            } else {
                log.error("UriBase is null");
                endResponse(HttpCode.NOT_FOUND, "UriBase is null");
                return false;
            }
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
            endResponse(HttpCode.NOT_FOUND, e.getMessage());
            return false;
        }
        for (UriSupported uriEnum : EnumSet.allOf(UriSupported.class)) {
            if (uriBase.equals(uriEnum.toString().toLowerCase())) {
                return true;
            }
        }
        endResponse(HttpCode.NOT_FOUND, String.format("URI /%s not supported", uriBase));
        return false;
    }

    /**
     * Check if method is ok.
     *
     * @param methodId the method id
     * @return true, if successful
     */
    public boolean checkMethodOk(String methodId) {
        String method = req.method();
        if (!methodId.equalsIgnoreCase(method)) {
            endResponse(HttpCode.METHOD_NOT_ALLOWED, "Method Not Allowed");
            return false;
        }
        return true;
    }

    /**
     * Check if id is consistency.
     *
     * @param entityJson the entity json
     * @param idFromUri the id from uri
     * @return true, if successful
     */
    public boolean checkIdConsistency(JsonObject entityJson, String idFromUri) {
        String idFromJson = entityJson.getString(IJsonable.ID_FIELDNAME, "");
        if ("".equals(idFromJson) || "".equals(idFromUri) || !idFromJson.equals(idFromUri)) {
            endResponse(HttpCode.BAD_REQUEST,
                    String.format("IDs inconsistents: bodyId(%s) not equal uriId(%s)", idFromJson, idFromUri));
            return false;
        }
        return true;
    }

    /**
     * Gets the request id.
     *
     * @return the request id
     */
    public String getRequestId() {
        String idFromUri = "";
        try {
            idFromUri = req.params() != null && req.params().contains(IJsonable.ID_FIELDNAME) ?
                    java.net.URLDecoder.decode(req.params().get(IJsonable.ID_FIELDNAME), "UTF-8") : "";
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        return idFromUri;
    }


    /**
     * Check if id is present.
     *
     * @return true, if successful
     */
    public boolean checkIdPresent() {
        if ("".equals(getRequestId())) {
            endResponse(HttpCode.BAD_REQUEST, "ID absent");
            return false;
        }
        return true;
    }

    /**
     * End response.
     *
     * @param statusCode the status code
     * @param message the message
     * @return true, if successful
     */
    public boolean endResponse(int statusCode, String message) {
        if (statusCode < HttpCode.BAD_REQUEST) {
            log.info(message);
        } else {
            log.warn(message);
        }
        boolean isOk = true;
        try {
            serverResponse.setStatusCode(statusCode)
                .setMessage(HttpCode.getMessage(statusCode, true))
                .setId(id)
                .endResponse();
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            isOk = false;
        }
        return isOk;
    }

    /**
     * Status from message schema.
     *
     * @param message the message
     * @param uri the uri
     * @param registerLog the register log
     * @return http status code
     */
    public int statusFromMessageSchema(String message, String uri, boolean registerLog) {

        JsonObject json = new JsonObject(message);
        if ("{}".equals(json.encode())) {
            if (registerLog) {
                log.error(String.format("Json decode error: %s", message));
            }
            return HttpCode.BAD_REQUEST;
        }

        if (!json.containsField("version")) {
            if (registerLog) log.error(String.format("Version is mandatory: %s", message));
            return HttpCode.BAD_REQUEST;
        }

        if (!json.containsField(IJsonable.ID_FIELDNAME)) {
            if (registerLog) log.error(String.format("ID is mandatory: %s", message));
            return HttpCode.BAD_REQUEST;
        }
        return HttpCode.OK;
    }

    /**
     * Status from message schema.
     *
     * @param message the message
     * @param uri the uri
     * @return http status code
     */
    public int statusFromMessageSchema(String message, String uri) {
        return statusFromMessageSchema(message, uri, true);
    }

}
