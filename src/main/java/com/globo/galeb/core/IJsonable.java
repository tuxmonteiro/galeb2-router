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

public interface IJsonable {

    public static final String jsonIdFieldName         = "id";
    public static final String jsonPropertiesFieldName = "properties";

    public static final String jsonCreatedAtFieldName  = "_created_at";
    public static final String jsonModifiedAtFieldName = "_modified_at";
    public static final String jsonLinksFieldName      = "_links";
    public static final String jsonLinksRelFieldName   = "_rel";
    public static final String jsonLinksHrefFieldName  = "_href";

    public JsonObject toJson();

}
