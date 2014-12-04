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
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.bus.IQueueService;
import com.globo.galeb.bus.NullQueueService;
import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.impl.NullScheduler;
import com.globo.galeb.server.ServerResponse;


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

    /** The backend (json format). */
    private JsonObject backend;

    /** The queue service. */
    private IQueueService queueService = new NullQueueService();

    /** The log. */
    private SafeLogger log = null;

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(Throwable event) {
        scheduler.cancel();
        queueService.publishBackendFail(backend);
        if (log==null) {
            log = new SafeLogger();
        }
        log.error(String.format("ClientRequestExceptionHandler: %s", event.getMessage()));
        sResponse.setBackendId(backend.getString(IJsonable.ID_FIELDNAME)).showErrorAndClose(event);
    }

    public ClientRequestExceptionHandler setsResponse(final ServerResponse sResponse) {
        this.sResponse = sResponse;
        return this;
    }

    public ClientRequestExceptionHandler setScheduler(final IScheduler scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    public ClientRequestExceptionHandler setBackendJson(JsonObject json) {
        this.backend = json;
        return this;
    }

    public ClientRequestExceptionHandler setQueueService(final IQueueService queueService) {
        this.queueService = queueService;
        return this;
    }

    public ClientRequestExceptionHandler setLog(final SafeLogger log) {
        this.log = log;
        return this;
    }

}

