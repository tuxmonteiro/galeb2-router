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

    public static final String ID_FIELDNAME          = "id";
    public static final String PARENT_ID_FIELDNAME   = "parentId";
    public static final String PROPERTIES_FIELDNAME  = "properties";

    public static final String STATUS_FIELDNAME      = "_status";
    public static final String CREATED_AT_FIELDNAME  = "_created_at";
    public static final String MODIFIED_AT_FIELDNAME = "_modified_at";
    public static final String LINKS_FIELDNAME       = "_links";
    public static final String LINKS_REL_FIELDNAME   = "rel";
    public static final String LINKS_HREF_FIELDNAME  = "href";

    public JsonObject toJson();

}
