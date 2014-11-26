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
package com.globo.galeb.logger;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import org.vertx.java.core.logging.Logger;

/**
 * Class SafeLogger.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 18, 2014.
 */
public class SafeLogger {

    /**
     * Enum LogLevel.
     *
     * @author See AUTHORS file.
     * @version 1.0.0, Nov 18, 2014.
     */
    public enum LogLevel {

        /** The fatal level. */
        FATAL,

        /** The error level. */
        ERROR,

        /** The warn level. */
        WARN,

        /** The info level. */
        INFO,

        /** The debug level. */
        DEBUG,

        /** The trace level. */
        TRACE,

        /** The undef level. */
        UNDEF
    }

    /** The logger. */
    private static Logger log = null;

    /** The default log level. */
    private LogLevel level = LogLevel.UNDEF;

    /**
     * Sets the logger.
     *
     * @param log the log
     * @return the safe logger
     */
    public SafeLogger setLogger(final Object alog) {
        if (alog instanceof Logger && log == null) {
            log=(Logger)alog;
        }
        return this;
    }

    /**
     * Gets the logger.
     *
     * @return the logger
     */
    public Logger getLogger() {
        return log;
    }

    /**
     * Log println.
     *
     * @param logLevel the log level
     * @param message the message
     * @param t throwable event
     */
    private void logPrintln(LogLevel logLevel, final Object message, final Throwable t) {
        if (logLevel==null) logLevel = LogLevel.UNDEF;
        System.err.println(String.format("[%s] %s", logLevel.toString(), message));
        if (t!=null) {
            System.err.println(String.format("[%s] %s", logLevel.toString(), getStackTrace(t)));
        }
    }

    /**
     * Fatal log.
     *
     * @param message the message
     */
    public void fatal(final Object message) {
        level = LogLevel.FATAL;
        if (log==null) {
            logPrintln(level, message, null);
            return;
        }
        log.fatal(message);
    }

    /**
     * Fatal log.
     *
     * @param message the message
     * @param t throwable event
     */
    public void fatal(final Object message, final Throwable t) {
        level = LogLevel.FATAL;
        if (log==null) {
            logPrintln(level, message, t);
            return;
        }
        log.fatal(message, t);
    }

    /**
     * Error log.
     *
     * @param message the message
     */
    public void error(final Object message) {
        level = LogLevel.ERROR;
        if (log==null) {
            logPrintln(level, message, null);
            return;
        }
        log.error(message);
    }

    /**
     * Error log.
     *
     * @param message the message
     * @param t throwable event
     */
    public void error(final Object message, final Throwable t) {
        level = LogLevel.ERROR;
        if (log==null) {
            logPrintln(level, message, t);
            return;
        }
        log.error(message, t);
    }

    /**
     * Warn log.
     *
     * @param message the message
     */
    public void warn(final Object message) {
        level = LogLevel.WARN;
        if (log==null) {
            logPrintln(level, message, null);
            return;
        }
        log.warn(message);
    }

    /**
     * Warn log.
     *
     * @param message the message
     * @param t throwable event
     */
    public void warn(final Object message, final Throwable t) {
        level = LogLevel.WARN;
        if (log==null) {
            logPrintln(level, message, t);
            return;
        }
        log.warn(message, t);
    }

    /**
     * Info log.
     *
     * @param message the message
     */
    public void info(final Object message) {
        level = LogLevel.INFO;
        if (log==null) {
            logPrintln(level, message, null);
            return;
        }
        log.info(message);
    }

    /**
     * Info log.
     *
     * @param message the message
     * @param t throwable event
     */
    public void info(final Object message, final Throwable t) {
        level = LogLevel.INFO;
        if (log==null) {
            logPrintln(level, message, t);
            return;
        }
        log.info(message, t);
    }

    /**
     * Debug log.
     *
     * @param message the message
     */
    public void debug(final Object message) {
        level = LogLevel.DEBUG;
        if (log==null) {
            logPrintln(level, message, null);
            return;
        }
        log.debug(message);
    }

    /**
     * Debug log.
     *
     * @param message the message
     * @param t throwable event
     */
    public void debug(final Object message, final Throwable t) {
        level = LogLevel.DEBUG;
        if (log==null) {
            logPrintln(level, message, t);
            return;
        }
        log.debug(message, t);
    }

    /**
     * Trace log.
     *
     * @param message the message
     */
    public void trace(final Object message) {
        level = LogLevel.TRACE;
        if (log==null) {
            logPrintln(level, message, null);
            return;
        }
        log.trace(message);
    }

    /**
     * Trace log.
     *
     * @param message the message
     * @param t throwable event
     */
    public void trace(final Object message, final Throwable t) {
        level = LogLevel.TRACE;
        if (log==null) {
            logPrintln(level, message, t);
            return;
        }
        log.trace(message, t);
    }

    /**
     * Gets the last log level.
     *
     * @return the last log level
     */
    public LogLevel getLastLogLevel() {
        return level;
    }

}
