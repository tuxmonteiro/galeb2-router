package com.globo.galeb.exceptions;

import com.globo.galeb.core.HttpCode;

public class NotFoundException extends AbstractHttpException {

    private static final long serialVersionUID = -1148048070375338827L;

    public NotFoundException() {
        super(HttpCode.NotFound);
    }
}
