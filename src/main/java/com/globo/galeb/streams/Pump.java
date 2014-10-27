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

    /** The drain handler. */
    private final Handler<Void> drainHandler = new Handler<Void>() {
        @Override
        public void handle(Void v) {
            try{
                rs.resume();
            } catch (RuntimeException e) {
                stop();
                handleException(e);
            }
        }
      };

    /** The data handler. */
    private final Handler<Buffer> dataHandler = new Handler<Buffer>() {
        @Override
        public void handle(Buffer buffer) {
            try {
                ws.write(buffer);
                handleWrite();
                pumped += buffer.length();
                if (ws.writeQueueFull()) {
                    rs.pause();
                    ws.drainHandler(drainHandler);
                }
            } catch (RuntimeException e) {
                stop();
                handleException(e);
            }
        }
    };

    /** The exception handler. */
    private Handler<Throwable> exceptionHandler;

    /** The write handler. */
    private Handler<Void> writeHandler;

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
        try {
            ws.drainHandler(null);
        } catch (RuntimeException e) {
            handleException(e);
        } finally {
            rs.dataHandler(null);
        }
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
     * Exception handler.
     *
     * @param exceptionHandler the exception handler
     * @return this
     */
    public Pump exceptionHandler(Handler<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    /**
     * Handle exception: invoqued when happens exceptions.
     *
     * @param throwable the throwable event
     */
    private void handleException(Throwable throwable) {
        if (exceptionHandler != null) {
            exceptionHandler.handle(throwable);
        }
    }

    /**
     * Write handler.
     *
     * @param writeHandler the write handler
     * @return this
     */
    public Pump writeHandler(Handler<Void> writeHandler) {
        this.writeHandler = writeHandler;
        return this;
    }

    /**
     * Handle write: invoqued when pump call WriteStream.write()
     */
    private void handleWrite() {
        Void voidParam = null;
        if (writeHandler != null) {
            writeHandler.handle(voidParam);
        }
    }
}
