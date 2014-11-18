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
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.core.ServerResponse;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.bus.NullQueueService;
import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.impl.NullScheduler;


/**
 * Class ClientRequestExceptionHandler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class ClientRequestExceptionHandler implements Handler<Throwable> {

    /** The serverResponse instance. */
    private ServerResponse sResponse = null;

    /** the scheduler instance */
    private IScheduler scheduler = new NullScheduler();

    /** The header host of request. */
    private String headerHost;

    /** The backend id. */
    private String backendId;

    /** The queue service. */
    private IQueueService queueService = new NullQueueService();

    /** The log. */
    private Logger log = null;

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(Throwable event) {
        scheduler.cancel();
        queueService.publishBackendFail(backendId.toString());
        log.error(String.format("ClientRequestExceptionHandler: %s", event.getMessage()));
        sResponse.setHeaderHost(headerHost).setBackendId(backendId)
            .showErrorAndClose(event);
    }

    public ClientRequestExceptionHandler setsResponse(final ServerResponse sResponse) {
        this.sResponse = sResponse;
        return this;
    }

    public ClientRequestExceptionHandler setScheduler(final IScheduler scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    public ClientRequestExceptionHandler setHeaderHost(String headerHost) {
        this.headerHost = headerHost;
        return this;
    }

    public ClientRequestExceptionHandler setBackendId(String backendId) {
        this.backendId = backendId;
        return this;
    }

    public ClientRequestExceptionHandler setQueueService(final IQueueService queueService) {
        this.queueService = queueService;
        return this;
    }

    public ClientRequestExceptionHandler setLog(final Logger log) {
        this.log = log;
        return this;
    }



}

