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
package com.globo.galeb.test.integration.util;

import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;

public class Util {

    public static JsonObject safeExtractJson(String s) {
        JsonObject json = null;
        try {
            json = new JsonObject(s);
        } catch (DecodeException e) {
            System.out.printf("The string %s is not a well-formed Json", s);
        }
        return json;
    }
}
