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
package com.globo.galeb.list;

import java.util.ArrayList;

/**
 * Class UniqueArrayList: ArrayList with T unique elements
 *
 * @param <T> the generic type
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class UniqueArrayList<T> extends ArrayList<T> {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 250697580015525312L;

    /**
     * Instantiates a new unique array list.
     */
    public UniqueArrayList() {
        super();
    }

    /**
     * Instantiates a new unique array list.
     *
     * @param aList the a list
     */
    public UniqueArrayList(ArrayList<T> aList) {
        this();
        if (aList!=null) {
            for (T object: aList) {
                this.add(object);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    @Override
    public boolean add(T object) {
        if (object!=null && !contains(object)) {
            return super.add(object);
        }
        return false;
    }
}
