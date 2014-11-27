package com.globo.galeb.handlers;

import com.globo.galeb.exceptions.GatewayTimeoutException;
import com.globo.galeb.scheduler.ISchedulerHandler;
import com.globo.galeb.server.ServerResponse;

/**
 * Class GatewayTimeoutTaskHandler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class GatewayTimeoutTaskHandler implements ISchedulerHandler {

    /** The serverResponse. */
    private final ServerResponse sResponse;

    /** The backend id. */
    private final String backendId;

    /**
     * Instantiates a new gateway timeout task handler.
     *
     * @param sResponse the serverResponse instance
     * @param headerHost the header host
     * @param backendId the backend id
     */
    public GatewayTimeoutTaskHandler(final ServerResponse sResponse, String backendId) {
        this.sResponse = sResponse;
        this.backendId = backendId;
    }

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle() {
        sResponse.setBackendId(backendId).showErrorAndClose(new GatewayTimeoutException());
    }

}
