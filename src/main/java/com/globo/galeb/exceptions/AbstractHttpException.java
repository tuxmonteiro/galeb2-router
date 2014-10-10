package com.globo.galeb.exceptions;

public abstract class AbstractHttpException extends RuntimeException {

    private static final long serialVersionUID = 8815494516177855852L;

    private int httpCode;

    public AbstractHttpException(int httpCode) {
        this.httpCode = httpCode;
    }

    public int getHttpCode() {
        return httpCode;
    }
}

