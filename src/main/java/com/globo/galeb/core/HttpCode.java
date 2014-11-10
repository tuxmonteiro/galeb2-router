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
package com.globo.galeb.core;

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.rules.IRuleReturn;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Class HttpCode.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class HttpCode implements IRuleReturn {

    /** The Constant OK. */
    public static final int OK                    = HttpResponseStatus.OK.code(); // 200

    /** The Constant ACCEPTED. */
    public static final int ACCEPTED              = HttpResponseStatus.ACCEPTED.code(); // 202

    /** The Constant FOUND. */
    public static final int FOUND                 = HttpResponseStatus.FOUND.code(); // 302

    /** The Constant TEMPORARY_REDIRECT. */
    public static final int TEMPORARY_REDIRECT    = HttpResponseStatus.TEMPORARY_REDIRECT.code(); // 307

    /** The Constant BAD_REQUEST. */
    public static final int BAD_REQUEST           = HttpResponseStatus.BAD_REQUEST.code(); // 400

    /** The Constant NOT_FOUND. */
    public static final int NOT_FOUND             = HttpResponseStatus.NOT_FOUND.code(); // 404

    /** The Constant METHOD_NOT_ALLOWED. */
    public static final int METHOD_NOT_ALLOWED    = HttpResponseStatus.METHOD_NOT_ALLOWED.code(); // 405

    /** The Constant INTERNAL_SERVER_ERROR. */
    public static final int INTERNAL_SERVER_ERROR = HttpResponseStatus.INTERNAL_SERVER_ERROR.code(); // 500

    /** The Constant BAD_GATEWAY. */
    public static final int BAD_GATEWAY           = HttpResponseStatus.BAD_GATEWAY.code(); // 502

    /** The Constant SERVICE_UNAVAILABLE. */
    public static final int SERVICE_UNAVAILABLE   = HttpResponseStatus.SERVICE_UNAVAILABLE.code(); // 503

    /** The Constant GATEWAY_TIMEOUT. */
    public static final int GATEWAY_TIMEOUT       = HttpResponseStatus.GATEWAY_TIMEOUT.code(); // 504

    /** The http code. */
    private Integer httpCode = OK;

    /**
     * Instantiates a new http code.
     */
    public HttpCode(int httpCode) {
        this.httpCode = httpCode;
    }

    /**
     * Gets the message.
     *
     * @param statusCode the status code
     * @return the message
     */
    public static String getMessage(int statusCode) {
        return getMessage(statusCode, false);
    }

    /**
     * Gets the message.
     *
     * @param statusCode the status code
     * @param asJson the as json
     * @return the message
     */
    public static String getMessage(int statusCode, boolean asJson){
        String message = HttpResponseStatus.valueOf(statusCode).reasonPhrase();
        if (asJson) {
            return new JsonObject().putString("status_message", message)
                        .encodePrettily();
        } else {
            return message;
        }
    }

    /**
     * Code family.
     *
     * @param code the code
     * @return the int
     */
    public static int codeFamily(int code) {
        return code/100;
    }

    /**
     * Checks if is server error.
     *
     * @param code the code
     * @return true, if is server error
     */
    public static boolean isServerError(int code) {
        return (codeFamily(code)==5);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.rules.IRuleReturn#getReturnType()
     */
    @Override
    public String getReturnType() {
        return HttpCode.class.getSimpleName();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.rules.IRuleReturn#getReturnId()
     */
    @Override
    public String getReturnId() {
        return this.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return httpCode.toString();
    }

}
