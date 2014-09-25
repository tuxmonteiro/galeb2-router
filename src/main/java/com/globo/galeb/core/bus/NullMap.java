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
package com.globo.galeb.core.bus;

public class NullMap extends MessageToMap<IEventObserver> {

    public NullMap() {
        super();
    }

    @Override
    public boolean add() {
        if (log!=null) {
            log.warn(String.format("[%s] uriBase %s not supported", verticleId, uriBase));
        } else {
            System.err.println("Farm is NULL or uriBase not supported");
        }
        return super.add();
    }

    @Override
    public boolean del() {
        if (log!=null) {
            log.warn(String.format("[%s] uriBase %s not supported", verticleId, uriBase));
        } else {
            System.err.println("Farm is Null and uriBase not supported");
        }
        return super.del();
    }

    @Override
    public boolean reset() {
        if (log!=null) {
            log.warn(String.format("[%s] uriBase %s not supported", verticleId, uriBase));
        } else {
            System.err.println("Farm is Null and uriBase not supported");
        }
        return super.reset();
    }

    @Override
    public boolean change() {
        if (log!=null) {
            log.warn(String.format("[%s] uriBase %s not supported", verticleId, uriBase));
        } else {
            System.err.println("Farm is Null and uriBase not supported");
        }
        return super.change();
    }

}
