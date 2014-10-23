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
package com.globo.galeb.exceptions;

/**
 * Class AbstractHttpException.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public abstract class AbstractHttpException extends RuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 8815494516177855852L;

    /** The http code. */
    private final int httpCode;

    /**
     * Instantiates a new abstract http exception.
     *
     * @param httpCode the http code
     */
    public AbstractHttpException(int httpCode) {
        this.httpCode = httpCode;
    }

    /**
     * Gets the http code.
     *
     * @return the http code
     */
    public int getHttpCode() {
        return httpCode;
    }
}

