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
package com.globo.galeb.streams;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;
import org.vertx.java.core.streams.WriteStream;

import com.globo.galeb.scheduler.IScheduler;

/**
 * Class Pump.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 22, 2014.
 */
public class Pump {

    /** The ReadStream. */
    private final ReadStream<?> rs;

    /** The WriteStream. */
    private final WriteStream<?> ws;

    /** The number of bytes pumped by this pump. */
    private int pumped = 0;

    /** The scheduler. */
    private IScheduler scheduler = null;

    /** The drain handler. */
    private final Handler<Void> drainHandler = new Handler<Void>() {
        @Override
        public void handle(Void v) {
          rs.resume();
        }
      };

    /** The data handler. */
    private final Handler<Buffer> dataHandler = new Handler<Buffer>() {
        @Override
        public void handle(Buffer buffer) {
            if (scheduler!=null) {
                scheduler.cancel();
            }
            try {
                ws.write(buffer);
            } catch (RuntimeException e) {
                rs.dataHandler(null);
                return;
            }
            pumped += buffer.length();
            if (ws.writeQueueFull()) {
              rs.pause();
              ws.drainHandler(drainHandler);
            }
        }
    };

    /**
     * Instantiates a new pump.
     *
     * @param rs ReadStream
     * @param ws WriteStream
     */
    public Pump(ReadStream<?> rs, WriteStream<?> ws) {
        this.rs = rs;
        this.ws = ws;
    }

    /**
     * Instantiates a new pump.
     *
     * @param rs ReadStream
     * @param ws WriteStream
     * @param maxWriteQueueSize the max write queue size
     */
    public Pump(ReadStream<?> rs, WriteStream<?> ws, int maxWriteQueueSize) {
        this(rs, ws);
        this.ws.setWriteQueueMaxSize(maxWriteQueueSize);
    }

    /**
     * Start the Pump. The Pump can be started and stopped multiple times.
     *
     * @return this
     */
    public Pump start() {
      rs.dataHandler(dataHandler);
      return this;
    }

    /**
     * Stop the Pump. The Pump can be started and stopped multiple times.
     *
     * @return this
     */
    public Pump stop() {
      ws.drainHandler(null);
      rs.dataHandler(null);
      return this;
    }

    /**
     * Return the total number of bytes pumped by this pump.
     *
     * @return bytes pumped
     */
    public int bytesPumped() {
      return this.pumped;
    }

    /**
     * Sets the scheduler time out.
     *
     * @param scheduler the scheduler
     * @return this
     */
    public Pump setSchedulerTimeOut(final IScheduler scheduler) {
        this.scheduler = scheduler;
        return this;
    }
}
