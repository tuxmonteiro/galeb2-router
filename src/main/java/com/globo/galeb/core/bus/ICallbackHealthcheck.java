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

import java.io.UnsupportedEncodingException;

/**
 * Interface ICallbackHealthcheck.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public interface ICallbackHealthcheck {

    /**
     * Move backend.
     *
     * @param backend the backend
     * @param status the status
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public void moveBackend(String backend, boolean status) throws UnsupportedEncodingException;

}
