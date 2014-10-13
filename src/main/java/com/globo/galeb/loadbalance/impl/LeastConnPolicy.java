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
package com.globo.galeb.loadbalance.impl;

import java.util.List;

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.loadbalance.ILoadBalancePolicy;

public class LeastConnPolicy implements ILoadBalancePolicy {

    private LeastConnectionsFinder leastConnectionsFinder = null;
    private long lastReset = System.currentTimeMillis();

    @Override
    public Backend getChoice(final List<Backend> backends, final RequestData requestData) {

        JsonObject properties = requestData.getProperties();
        long timeout = properties.getLong(ILoadBalancePolicy.CACHE_TIMEOUT_FIELDNAME, 2000L);
        boolean transientState = properties.getBoolean(Virtualhost.TRANSIENT_STATE_FIELDNAME, false);

        long now = System.currentTimeMillis();

        if (leastConnectionsFinder == null) {
            transientState = false;
            lastReset = now;
            leastConnectionsFinder = new LeastConnectionsFinder(backends);
        }

        if (transientState) {
            leastConnectionsFinder.rebuild(backends);
            lastReset = now;
        } else if ((lastReset + timeout) < now) {
            leastConnectionsFinder.update();
            lastReset = now;
        }

        return leastConnectionsFinder.get();
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public String toString() {
        return LeastConnPolicy.class.getSimpleName();
    }

}
