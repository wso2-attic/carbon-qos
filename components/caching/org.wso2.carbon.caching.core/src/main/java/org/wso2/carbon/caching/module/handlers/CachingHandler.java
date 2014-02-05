/*
 * Copyright (c) 2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.caching.module.handlers;

import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.core.CachingException;

public abstract class CachingHandler extends AbstractHandler {

    /**
     * Log object for logging purposes
     */
    protected static final Log log = LogFactory.getLog(CachingHandler.class);

    protected void handleException(String message) throws AxisFault {
        log.error(message);
        throw new AxisFault(message, new CachingException(message));
    }

    protected void handleException(String message, Throwable cause) throws AxisFault {
        log.error(message + " : " + cause.getMessage());
        throw new AxisFault(message, cause);
    }
}
