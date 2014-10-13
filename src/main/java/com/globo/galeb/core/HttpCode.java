package com.globo.galeb.core;

import org.vertx.java.core.json.JsonObject;
import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpCode {

    public static final int OK                    = HttpResponseStatus.OK.code(); // 200
    public static final int ACCEPTED              = HttpResponseStatus.ACCEPTED.code(); // 202
    public static final int FOUND                 = HttpResponseStatus.FOUND.code(); // 302
    public static final int TEMPORARY_REDIRECT    = HttpResponseStatus.TEMPORARY_REDIRECT.code(); // 307
    public static final int BAD_REQUEST           = HttpResponseStatus.BAD_REQUEST.code(); // 400
    public static final int NOT_FOUND             = HttpResponseStatus.NOT_FOUND.code(); // 404
    public static final int METHOD_NOT_ALLOWED    = HttpResponseStatus.METHOD_NOT_ALLOWED.code(); // 405
    public static final int INTERNAL_SERVER_ERROR = HttpResponseStatus.INTERNAL_SERVER_ERROR.code(); // 500
    public static final int BAD_GATEWAY           = HttpResponseStatus.BAD_GATEWAY.code(); // 502
    public static final int SERVICE_UNAVAILABLE   = HttpResponseStatus.SERVICE_UNAVAILABLE.code(); // 503
    public static final int GATEWAY_TIMEOUT       = HttpResponseStatus.GATEWAY_TIMEOUT.code(); // 504

    private HttpCode() {
    }

    public static String getMessage(int statusCode) {
        return getMessage(statusCode, false);
    }

    public static String getMessage(int statusCode, boolean asJson){
        String message = HttpResponseStatus.valueOf(statusCode).reasonPhrase();
        if (asJson) {
            return new JsonObject().putString("status_message", message)
                        .encodePrettily();
        } else {
            return message;
        }
    }

    public static int codeFamily(int code) {
        return code/100;
    }

    public static boolean isServerError(int code) {
        return (codeFamily(code)==5);
    }

}
