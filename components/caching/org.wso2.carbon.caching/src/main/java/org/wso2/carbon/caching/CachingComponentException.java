/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.caching;

import org.apache.commons.logging.Log;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Exception class that represents exceptions related to caching component.
 */
public class CachingComponentException extends Exception{

    /**
     * Resource bundle that holds the caching errors package.
     */
    private static ResourceBundle resources;

    private static String msg;

    static {
        try {
            resources = ResourceBundle.getBundle("org.wso2.carbon.caching.errors");
        } catch (MissingResourceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Creates a new <code>CachingComponentException</code> instance.
     *
     * @param errorCode a key field in the <code>errors.properties</code>.            
     * @param args the set of arguments which will be put into the error message.
     * @param log the <code>Log</code> object for the originating class.
     */
    public CachingComponentException (String errorCode, Object[] args, Log log) {
        super(msg = getMessage(errorCode, args));
        log.error(msg);
    }

    /**
     * Creates a new <code>CachingComponentException</code> instance.
     *
     * @param errorCode a key field in the <code>errors.properties</code>.
     * @param log the <code>Log</code> object for the originating class.
     */
    public CachingComponentException (String errorCode, Log log) {
        this(errorCode, (Object[]) null, log);
    }

    /**
     * Creates a new <code>CachingComponentException</code> instance.
     *
     * @param errorCode a key field in the <code>errors.properties</code>.
     * @param args the set of arguments which will be put into the error message.
     * @param e the original exception.
     * @param log the <code>Log</code> object for the originating class.
     */
    public CachingComponentException (String errorCode, Object[] args, Throwable e, Log log) {
        super(msg = getMessage(errorCode, args), e);
        log.error(msg);
    }

    /**
     * Creates a new <code>CachingComponentException</code> instance.
     *
     * @param errorCode a key field in the <code>errors.properties</code>.
     * @param e the original exception
     * @param log the <code>Log</code> object for the originating class.
     */
    public CachingComponentException (String errorCode, Throwable e, Log log) {
        this(errorCode, null, e, log);
    }

    /**
     * get the error message from resource bundle.
     *
     * @return the message translated from the property (message) file.
     */
    protected static String getMessage (String errorCode, Object[] args) {
        String msg;
        try {
            msg = MessageFormat.format(resources.getString(errorCode), args);
        } catch (MissingResourceException e) {
            throw new RuntimeException("Undefined '" + errorCode + "' resource property");
        }
        return msg;
    }
}
