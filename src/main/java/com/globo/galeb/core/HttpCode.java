package com.globo.galeb.core;

import org.vertx.java.core.json.JsonObject;
import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpCode {

    public static int Ok                  = HttpResponseStatus.OK.code(); // 200
    public static int TemporaryRedirect   = HttpResponseStatus.TEMPORARY_REDIRECT.code(); // 302
    public static int BadRequest          = HttpResponseStatus.BAD_REQUEST.code(); // 400
    public static int MethotNotAllowed    = HttpResponseStatus.METHOD_NOT_ALLOWED.code(); // 405
    public static int InternalServerError = HttpResponseStatus.INTERNAL_SERVER_ERROR.code(); // 500
    public static int BadGateway          = HttpResponseStatus.BAD_GATEWAY.code(); // 502
    public static int GatewayTimeout      = HttpResponseStatus.GATEWAY_TIMEOUT.code(); // 504

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
