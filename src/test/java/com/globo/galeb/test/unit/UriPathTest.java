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
package com.globo.galeb.test.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import com.globo.galeb.criteria.ICriterion.CriterionAction;
import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.impl.frontend.Rule;
import com.globo.galeb.entity.impl.frontend.UriPath;
import com.globo.galeb.entity.impl.frontend.Virtualhost;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.rulereturn.HttpCode;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class UriPathTest {

    private JsonObject virtualhostJson = new JsonObject().putString(IJsonable.ID_FIELDNAME, "test.localdomain");
    private Virtualhost virtualhost = new Virtualhost(virtualhostJson);
    private int numRules = 10;
    private HttpServerRequest httpServerRequest = null;

    @Before
    public void setUp() throws Exception {

        virtualhost.setLogger(new SafeLogger()).start();

        for (Integer x=0; x<numRules; x++) {

            JsonObject ruleJson = new JsonObject().putString(IJsonable.ID_FIELDNAME, x.toString());
            ruleJson.putObject(IJsonable.PROPERTIES_FIELDNAME, new JsonObject());
            ruleJson.getObject(IJsonable.PROPERTIES_FIELDNAME)
                        .putString(Rule.RETURNTYPE_FIELDNAME, HttpCode.class.getSimpleName())
                        .putString(Rule.MATCH_FIELDNAME, "/"+x.toString());
            Rule rule = new UriPath(ruleJson).setPriorityOrder(numRules-x);

            virtualhost.addEntity(rule);
        }
        httpServerRequest = mock(HttpServerRequest.class);
        try {
            when(httpServerRequest.absoluteURI()).thenReturn(new URI("null://x/6"));
        } catch (URISyntaxException e) {}
        when(httpServerRequest.headers()).thenReturn(new CaseInsensitiveMultiMap());
        when(httpServerRequest.params()).thenReturn(new CaseInsensitiveMultiMap());
        when(httpServerRequest.remoteAddress()).thenReturn(InetSocketAddress.createUnresolved("0.0.0.0", 0));

    }

    @Test
    public void checkMatch() {
        Rule rule = virtualhost.getCriterion().when(httpServerRequest).action(CriterionAction.RESET_REQUIRED).thenGetResult();
        Rule expectedRule = virtualhost.getEntityById("6");
        assertThat(rule).isEqualTo(expectedRule);
    }

    @Test
    public void checkNotMatch() {
        Rule rule = virtualhost.getCriterion().when(httpServerRequest).action(CriterionAction.RESET_REQUIRED).thenGetResult();
        Rule expectedRule = virtualhost.getEntityById("7");
        assertThat(rule).isNotEqualTo(expectedRule);
    }
}
