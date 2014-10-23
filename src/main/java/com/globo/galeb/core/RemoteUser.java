/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.globo.galeb.core;

import java.net.InetSocketAddress;

/**
 * Class RemoteUser.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class RemoteUser {

    /** The remote ip. */
    private final String remoteIP;

    /** The remote port. */
    private final Integer remotePort;

    /** The remote user id. */
    private String remoteUserId;

    /**
     * Instantiates a new remote user.
     *
     * @param remoteIP the remote ip
     * @param remotePort the remote port
     */
    public RemoteUser(String remoteIP, Integer remotePort) {
        this.remoteIP = remoteIP;
        this.remotePort = remotePort;
        this.remoteUserId = String.format("%s:%d", remoteIP, remotePort);
    }

    /**
     * Instantiates a new remote user.
     *
     * @param remoteAddress the remote address
     */
    public RemoteUser(InetSocketAddress remoteAddress) {
        this(remoteAddress.getAddress().getHostAddress(), remoteAddress.getPort());
    }

    /**
     * Gets the remote ip.
     *
     * @return the remote ip
     */
    public String getRemoteIP() {
        return remoteIP;
    }

    /**
     * Gets the remote port.
     *
     * @return the remote port
     */
    public Integer getRemotePort() {
        return remotePort;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return remoteUserId.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return remoteUserId;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
