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
package com.globo.galeb.scheduler.impl;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;

import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.ISchedulerHandler;


/**
 * Class VertxDelayScheduler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 22, 2014.
 */
public class VertxDelayScheduler implements IScheduler {

    /** The vertx. */
    private final Vertx vertx;

    /** The scheduler handler. */
    private ISchedulerHandler schedulerHandler = null;

    /** The period (ms). */
    private Long period = -1L;

    /** The Scheduler id. */
    private Long id = 0L;

    /**
     * Instantiates a new vertx delay scheduler.
     *
     * @param vertx the vertx
     */
    public VertxDelayScheduler(Vertx vertx) {
        this.vertx = vertx;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.scheduler.IScheduler#start()
     */
    @Override
    public IScheduler start() {
        if (schedulerHandler!=null && period != -1L) {
            id = vertx.setTimer(period, new Handler<Long>() {
                @Override
                public void handle(Long event) {
                    schedulerHandler.handle();
                }
            });
        }
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.scheduler.IScheduler#setHandler(com.globo.galeb.scheduler.ISchedulerHandler)
     */
    @Override
    public IScheduler setHandler(ISchedulerHandler schedulerHandler) {
        this.schedulerHandler = schedulerHandler;
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.scheduler.IScheduler#setPeriod(java.lang.Long)
     */
    @Override
    public IScheduler setPeriod(Long period) {
        this.period = period;
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.scheduler.IScheduler#cancel()
     */
    @Override
    public IScheduler cancel() {
        if (id!=0L) {
            vertx.cancelTimer(id);
            id = 0L;
        }
        return this;
    }


}
