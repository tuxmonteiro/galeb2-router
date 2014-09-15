package com.globo.galeb.core;

import org.vertx.java.core.json.JsonObject;
import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpCode {

    public static final int Ok                  = HttpResponseStatus.OK.code(); // 200
    public static final int Accepted            = HttpResponseStatus.ACCEPTED.code(); // 202
    public static final int Found               = HttpResponseStatus.FOUND.code(); // 302
    public static final int TemporaryRedirect   = HttpResponseStatus.TEMPORARY_REDIRECT.code(); // 307
    public static final int BadRequest          = HttpResponseStatus.BAD_REQUEST.code(); // 400
    public static final int MethotNotAllowed    = HttpResponseStatus.METHOD_NOT_ALLOWED.code(); // 405
    public static final int InternalServerError = HttpResponseStatus.INTERNAL_SERVER_ERROR.code(); // 500
    public static final int BadGateway          = HttpResponseStatus.BAD_GATEWAY.code(); // 502
    public static final int GatewayTimeout      = HttpResponseStatus.GATEWAY_TIMEOUT.code(); // 504

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

}
