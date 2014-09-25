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
package com.globo.galeb.verticles;

import static com.globo.galeb.core.Constants.*;

import org.vertx.java.platform.Verticle;
import com.globo.galeb.core.SafeJsonObject;

public class Starter extends Verticle{

    @Override
    public void start() {
        final SafeJsonObject conf = new SafeJsonObject(container.config());
        final SafeJsonObject confRouter = new SafeJsonObject(conf.getObject(CONF_ROOT_ROUTER, new SafeJsonObject("{}")));
        final SafeJsonObject confRouteManager = new SafeJsonObject(conf.getObject(CONF_ROOT_ROUTEMANAGER, new SafeJsonObject("{}")));
        confRouteManager.putObject(CONF_STARTER_CONF, conf);
        final SafeJsonObject confHealthManager = new SafeJsonObject(conf.getObject(CONF_ROOT_HEALTHMANAGER, new SafeJsonObject("{}")));
        final SafeJsonObject confStatsd;
        if (conf.containsField(CONF_ROOT_STATSD)) {
            confStatsd = new SafeJsonObject(conf.getObject(CONF_ROOT_STATSD, new SafeJsonObject("{}")));
            container.deployVerticle(StatsdVerticle.class.getName(), confStatsd, confStatsd.getInteger(CONF_INSTANCES, 1));
            confRouter.putBoolean(CONF_STATSD_ENABLE, true);
            confRouter.putString(CONF_STATSD_HOST, confStatsd.getString(CONF_HOST, "localhost"));
            confRouter.putString(CONF_STATSD_PREFIX, confStatsd.getString(CONF_PREFIX, "stats"));
            confRouter.putNumber(CONF_STATSD_PORT, confStatsd.getInteger(CONF_PORT, 8125));

            confRouteManager.putBoolean(CONF_STATSD_ENABLE, true);
            confRouteManager.putString(CONF_STATSD_HOST, confStatsd.getString(CONF_HOST, "localhost"));
            confRouteManager.putString(CONF_STATSD_PREFIX, confStatsd.getString(CONF_PREFIX, "stats"));
            confRouteManager.putNumber(CONF_STATSD_PORT, confStatsd.getInteger(CONF_PORT, 8125));
        }

        int numCpuCores = Runtime.getRuntime().availableProcessors();
        container.deployVerticle(RouterVerticle.class.getName(), confRouter, confRouter.getInteger(CONF_INSTANCES, numCpuCores));
        container.deployVerticle(RouteManagerVerticle.class.getName(), confRouteManager, confRouteManager.getInteger(CONF_INSTANCES, 1));
        container.deployVerticle(HealthManagerVerticle.class.getName(), confHealthManager, confHealthManager.getInteger(CONF_INSTANCES, 1));
    }
}
