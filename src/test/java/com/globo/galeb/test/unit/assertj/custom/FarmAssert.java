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
package com.globo.galeb.test.unit.assertj.custom;

import com.globo.galeb.core.entity.impl.Farm;

import org.assertj.core.api.AbstractAssert;

public class FarmAssert extends AbstractAssert<FarmAssert, Farm> {

    protected FarmAssert(Farm actual) {
        super(actual, FarmAssert.class);
    }

    public static FarmAssert assertThat(Farm actual) {
        return new FarmAssert(actual);
    }

    public FarmAssert hasVirtualhostsSize(Integer size) {
        isNotNull();
        if (actual.getEntities().size() != size) {
            failWithMessage("Expected size to be <%s> but was <%s>", size, actual.getEntities().size());
        }
        return this;
    }

    public FarmAssert hasBackendsSize(Integer size) {
        isNotNull();
        if (actual.getBackends().size() != size) {
            failWithMessage("Expected size to be <%s> but was <%s>", size, actual.getBackends().size());
        }
        return this;
    }

    public FarmAssert hasProperty(String property) {
        isNotNull();
        if (!actual.getProperties().containsField(property)) {
            failWithMessage("Farm haven't the %s property", property);
        }
        return this;
    }

    public FarmAssert haventProperty(String property) {
        isNotNull();
        if (!actual.getProperties().containsField(property)) {
            failWithMessage("Farm has the %s property", property);
        }
        return this;
    }
}
