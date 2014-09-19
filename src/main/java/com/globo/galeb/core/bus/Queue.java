/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
package com.globo.galeb.core.bus;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.logging.Logger;
import com.globo.galeb.core.SafeJsonObject;

public class Queue {

    public enum ACTION {

        ADD         ("route.add"),
        DEL         ("route.del"),
        SET_VERSION ("route.version");

        private String queue;
        private ACTION(final String queue) {
            this.queue = queue;
        }
        @Override
        public String toString() {
            return queue;
        }
    }

    public final EventBus eb;
    private final Logger log;

    public Queue(final EventBus eb, final Logger log) {
        this.eb=eb;
        this.log=log;
    }

    public void queueToAdd(SafeJsonObject json, final String uri) {
        putMessageToQueue(json, ACTION.ADD, uri);
    }

    public void queueToDel(SafeJsonObject json, final String uri) {
        putMessageToQueue(json, ACTION.DEL, uri);
    }

    public void queueToChange(SafeJsonObject json, final String uri) {
        // putMessageToQueue(json, ACTION.CHANGE, uri);
        log.warn(String.format("%s: Change not implemented", this.toString()));
    }

    private void putMessageToQueue(SafeJsonObject json, ACTION action, final String uri) {
        Long version = 0L;

        try {
            version = json.getLong("version");
        } catch (DecodeException e) {
            if (log!=null) {
                log.error(e.getMessage());
            } else {
                System.err.println(e.getMessage());
            }
            return;
        }
        json.removeField("version");

        String parentId = json.getString(MessageBus.parentIdFieldName, "");

        MessageBus messageBus = new MessageBus()
                                    .setUri(uri)
                                    .setEntity(json.encode());

        if (!"".equals(parentId)) {
            messageBus.setParentId(parentId);
        }

        String message = messageBus.make().toString();

        publish(message, action);
        publish(String.format("%d", version), ACTION.SET_VERSION);

    }

    private void publish(String message, final ACTION action) {
        if (eb!=null) {
            eb.publish(action.toString(), message);
        } else {
            System.out.println(String.format("publish at %s queue: %s", action.toString(), message));
        }
        String debugMessage = String.format("Sending %s to %s",message, action.toString());
        if (log!=null) {
            log.debug(debugMessage);
        } else {
            System.out.println(debugMessage);
        }
    }

}
