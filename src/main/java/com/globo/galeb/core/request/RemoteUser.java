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
package com.globo.galeb.core.request;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Class RemoteUser.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class RemoteUser {

    /** The remote ip. */
    private InetAddress remoteIP = null;

    /** The remote port. */
    private Integer remotePort = 0;

    /** The remote user id. */
    private String remoteUserId;

    /**
     * Instantiates a new remote user.
     */
    public RemoteUser() {
        this("127.0.0.1", 0);
    }

    /**
     * Instantiates a new remote user.
     *
     * @param remoteIP the remote ip
     * @param remotePort the remote port
     */
    public RemoteUser(String ip, int port) {
        this(new InetSocketAddress(ip, port));
    }

    /**
     * Instantiates a new remote user.
     *
     * @param remoteAddress the remote address
     */
    public RemoteUser(InetSocketAddress remoteAddress) {
        this.remoteIP = remoteAddress.getAddress();
        this.remotePort = remoteAddress.getPort();
        this.remoteUserId = String.format("%s:%d", remoteIP, remotePort);
    }

    /**
     * Gets the remote ip.
     *
     * @return the remote ip
     */
    public String getRemoteIP() {
        return remoteIP.getHostAddress();
    }

    /**
     * Gets the remote port.
     *
     * @return the remote port
     */
    public Integer getRemotePort() {
        return remotePort;
    }

    /**
     * Convert IP Address to long.
     *
     * @param ipAddress the ip address
     * @return the long
     */
    private Long ipToLong(byte[] ipAddress) {
        return
            ((ipAddress [0] & 0xFFl) << (3*8)) +
            ((ipAddress [1] & 0xFFl) << (2*8)) +
            ((ipAddress [2] & 0xFFl) << (1*8)) +
            (ipAddress [3] &  0xFFl);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (ipToLong(remoteIP.getAddress()).intValue()*100000)+remotePort;
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
