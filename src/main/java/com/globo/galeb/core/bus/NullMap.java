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
package com.globo.galeb.core.bus;

import com.globo.galeb.core.entity.Entity;

/**
 * Class NullMap.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class NullMap extends MessageToMap<Entity> {

    /** The log message on err. */
    private String logMessageOnErr = "Farm is NULL or uriBase not supported";

    /** The log message on ok. */
    private String logMessageOk    = String.format("[%s] uriBase %s not supported", verticleId, uriBase);

    /**
     * Instantiates a new null map.
     */
    public NullMap() {
        super();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#add()
     */
    @Override
    public boolean add() {
        if (log!=null) {
            log.warn(logMessageOk);
        } else {
            System.err.println(logMessageOnErr);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#del()
     */
    @Override
    public boolean del() {
        if (log!=null) {
            log.warn(logMessageOk);
        } else {
            System.err.println(logMessageOnErr);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#reset()
     */
    @Override
    public boolean reset() {
        if (log!=null) {
            log.warn(logMessageOk);
        } else {
            System.err.println(logMessageOnErr);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#change()
     */
    @Override
    public boolean change() {
        if (log!=null) {
            log.warn(logMessageOk);
        } else {
            System.err.println(logMessageOnErr);
        }
        return false;
    }

}
