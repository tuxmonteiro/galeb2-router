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

import static org.junit.Assert.*;

import org.junit.Test;

import com.globo.galeb.core.bus.NullMap;

public class NullMapTest {

    private NullMap nullMap = new NullMap();

    @Test
    public void addReturnFalse() {
        assertFalse(nullMap.add());
    }

    @Test
    public void delReturnFalse() {
        assertFalse(nullMap.del());
    }

    @Test
    public void resetReturnFalse() {
        assertFalse(nullMap.reset());
    }

    @Test
    public void changeReturnFalse() {
        assertFalse(nullMap.change());
    }

}
