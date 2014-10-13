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

    private String logMessageOnErr = "Farm is NULL or uriBase not supported";
    private String logMessageOk    = String.format("[%s] uriBase %s not supported", verticleId, uriBase);

    public NullMap() {
        super();
    }

    @Override
    public boolean add() {
        if (log!=null) {
            log.warn(logMessageOk);
        } else {
            System.err.println(logMessageOnErr);
        }
        return super.add();
    }

    @Override
    public boolean del() {
        if (log!=null) {
            log.warn(logMessageOk);
        } else {
            System.err.println(logMessageOnErr);
        }
        return super.del();
    }

    @Override
    public boolean reset() {
        if (log!=null) {
            log.warn(logMessageOk);
        } else {
            System.err.println(logMessageOnErr);
        }
        return super.reset();
    }

    @Override
    public boolean change() {
        if (log!=null) {
            log.warn(logMessageOk);
        } else {
            System.err.println(logMessageOnErr);
        }
        return super.change();
    }

}
