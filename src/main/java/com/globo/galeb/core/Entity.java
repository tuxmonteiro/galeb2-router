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
package com.globo.galeb.core;

import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;

public abstract class Entity implements IJsonable {

    protected JsonObject           staticConf    = new JsonObject();
    protected String               id            = "";
    protected final Long           createdAt     = System.currentTimeMillis();
    protected Long                 modifiedAt    = System.currentTimeMillis();
    protected final JsonObject     properties    = new JsonObject();
    protected final JsonObject     idObj         = new JsonObject();
    protected String               entityType    = this.getClass().getSimpleName().toLowerCase();

    public JsonObject getProperties() {
        return properties;
    }

    protected Entity prepareJson() {
        idObj.putString(IJsonable.ID_FIELDNAME, id);
        idObj.putObject(LINKS_FIELDNAME, new SafeJsonObject()
            .putString(LINKS_REL_FIELDNAME, "self")
            .putString(LINKS_HREF_FIELDNAME, String.format("http://%s/%s/%s", Server.getHttpServerName(), entityType, id))
        );
        idObj.putString(STATUS_FIELDNAME, "created");
        idObj.putNumber(IJsonable.CREATED_AT_FIELDNAME, createdAt);
        idObj.putNumber(IJsonable.MODIFIED_AT_FIELDNAME, modifiedAt);
        idObj.putObject(IJsonable.PROPERTIES_FIELDNAME, properties);
        return this;
    }

    public Entity setStaticConf(String staticConf) {
        if (!"".equals(staticConf)) {
            try {
                this.staticConf = new JsonObject(staticConf);
            } catch (DecodeException ignore) {}
        }
        return this;
    }

    @Override
    public JsonObject toJson() {
        return idObj;
    }
}
