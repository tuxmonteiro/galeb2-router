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
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.bus.IQueueService;
import com.globo.galeb.entity.impl.Farm;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.metrics.ICounter;
import com.globo.galeb.request.RouterRequest;

/**
 * Class RouterRequestHandler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class RouterRequestHandler implements Handler<HttpServerRequest> {

    /** The vertx. */
    private final Vertx vertx;

    /** The farm. */
    private final Farm farm;

    /** The counter. */
    private final ICounter counter;

    /** The log. */
    private final SafeLogger log;

    /** The queueService instance. */
    private final IQueueService queueService;

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(final HttpServerRequest sRequest) throws RuntimeException {
        Long requestTimeout = farm.getProperties().getLong(Farm.REQUEST_TIMEOUT_FIELDNAME, 5000L);

        sRequest.headers().remove(HttpHeaders.IF_MODIFIED_SINCE.toString());

        JsonObject conf = new JsonObject();
        conf.putNumber(Farm.REQUEST_TIMEOUT_FIELDNAME, requestTimeout);

        new RouterRequest(sRequest).setFarm(farm)
                                   .setQueueService(queueService)
                                   .setLog(log)
                                   .setCounter(counter)
                                   .setConf(conf)
                                   .setPlataform(vertx)
                                   .start();
    }

    /**
     * Instantiates a new router request handler.
     *
     * @param vertx the vertx
     * @param farm the farm
     * @param counter the counter
     * @param queueService the queue service
     * @param log the logger
     */
    public RouterRequestHandler(
            final Vertx vertx,
            final Farm farm,
            final ICounter counter,
            final IQueueService queueService,
            final SafeLogger log) {
        this.vertx = vertx;
        this.farm = farm;
        this.counter = counter;
        this.queueService = queueService;
        this.log = log;
    }

}
