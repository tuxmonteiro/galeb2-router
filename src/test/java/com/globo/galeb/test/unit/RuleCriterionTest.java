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

import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.impl.frontend.NullRule;
import com.globo.galeb.entity.impl.frontend.Rule;
import com.globo.galeb.entity.impl.frontend.Virtualhost;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.json.JsonObject;

public class RuleCriterionTest {

    private JsonObject virtualhostJson = new JsonObject().putString(IJsonable.ID_FIELDNAME, "test.localdomain");
    private Virtualhost virtualhost = new Virtualhost(virtualhostJson);
    private int numRules = 10;

    @Before
    public void setUp() throws Exception {

        virtualhost.start();

        for (Integer x=0; x<numRules; x++) {
            JsonObject ruleJson = new JsonObject().putString(IJsonable.ID_FIELDNAME, x.toString());
            if (x==5) {
                ruleJson.putObject(IJsonable.PROPERTIES_FIELDNAME, new JsonObject());
                ruleJson.getObject(IJsonable.PROPERTIES_FIELDNAME).putBoolean(Rule.DEFAULT_FIELDNAME, true);
            }
            Rule rule = new NullRule(ruleJson).setPriorityOrder(numRules-x);

            virtualhost.addEntity(rule);
        }
    }

    @Test
    public void checkRuleDefault() {
        Rule rule = virtualhost.getCriterion().thenGetResult();
        Rule expectedRule = virtualhost.getEntityById("5");
        assertThat(rule).isEqualTo(expectedRule);
    }

}
