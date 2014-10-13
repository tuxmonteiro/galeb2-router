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

import java.net.InetSocketAddress;

public class RemoteUser {

    private final String remoteIP;
    private final Integer remotePort;
    private String remoteUserId;

    public RemoteUser(String remoteIP, Integer remotePort) {
        this.remoteIP = remoteIP;
        this.remotePort = remotePort;
        this.remoteUserId = String.format("%s:%d", remoteIP, remotePort);
    }

    public RemoteUser(InetSocketAddress remoteAddress) {
        this(remoteAddress.getAddress().getHostAddress(), remoteAddress.getPort());
    }

    public String getRemoteIP() {
        return remoteIP;
    }

    public Integer getRemotePort() {
        return remotePort;
    }

    @Override
    public int hashCode() {
        return remoteUserId.hashCode();
    }

    @Override
    public String toString() {
        return remoteUserId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RemoteUser other = (RemoteUser) obj;
        if (remoteUserId == null) {
            if (other.remoteUserId != null) return false;
        } else {
            if (!remoteUserId.equalsIgnoreCase(other.remoteUserId)) return false;
        }
        return true;
    }
}
