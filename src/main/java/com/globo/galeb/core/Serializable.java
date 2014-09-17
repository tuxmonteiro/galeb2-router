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

import org.vertx.java.core.json.JsonObject;

public abstract class Serializable implements IJsonable {

    protected String           id            = "";
    protected final Long       createdAt     = System.currentTimeMillis();
    protected Long             modifiedAt    = System.currentTimeMillis();
    protected final JsonObject properties    = new JsonObject();
    protected final JsonObject idObj         = new JsonObject();
    protected String           entityType    = this.getClass().getSimpleName().toLowerCase();

    public JsonObject getProperties() {
        return properties;
    }

    protected Serializable prepareJson() {
        idObj.putString(IJsonable.jsonIdFieldName, id);
        idObj.putObject(jsonLinksFieldName, new JsonObject()
            .putString(jsonLinksRelFieldName, "self")
            .putString(jsonLinksHrefFieldName, String.format("http://%s/%s/%s", Server.getHttpServerName(), entityType, id))
        );
        idObj.putNumber(IJsonable.jsonCreatedAtFieldName, createdAt);
        idObj.putNumber(IJsonable.jsonModifiedAtFieldName, modifiedAt);
        idObj.putObject(IJsonable.jsonPropertiesFieldName, properties);
        return this;
    }

    @Override
    public JsonObject toJson() {
        return idObj;
    }
}
