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

import javax.xml.namespace.QName;

public class CachingComponentConstants {

    /** Default configurations for caching */

    /** Default XML identifier. */
    public static final String DEF_XML_IDENTIFIER = "org.wso2.carbon.caching.core.digest.DOMHASHGenerator";
    /** Default expiration time, 60ms. */
    public static final long DEF_EXP_TIME = 60000;
    /** Default maximum possible number of cached messages in the system. */
    public static final int DEF_MAX_CACHE_SIZE = 1000;
    /** Default maximum payload message size in bytes. */
    public static final int DEF_MAX_MSG_SIZE = 1000;

    /** Names */

    /** Registry instance name. */
    public static final String KEY_REGISTRY_INSTANCE = "WSO2Registry";
    /** Caching module name. */
    public static final String CACHING_MODULE = "wso2caching";
    /** Caching policy ID key. */
    public static final String CACHING_POLICY_ID = "cachingPolicyId";

    /** Namespaces */

    /** WS-Security Utility namespace. */
    public static final String WSU_NS =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    /** WS-Policy namespace. */
    public static final String WSP_NS = "http://schemas.xmlsoap.org/ws/2004/09/policy";
    /** WSO2 caching module namespace. */
    public static final String WSCH_NS = "http://www.wso2.org/ns/2007/06/commons/caching";
    /** ID key. */
    public static final String ID = "Id";

    /** Namespace prefixes */

    /** WS-Security Utility namespace prefix. */
    public static final String PREF_WSU = "wsu";
    /** WS-Policy namespace prefix. */
    public static final String PREF_WSP = "wsp";
    /** WSO2 caching module namespace prefix. */
    public static final String PREF_WSCH = "wsch";

    /** Identifier string keys */

    /** XML identifier. */
    public static final String XML_IDENTIFIER = "XMLIdentifier";
    /** Expiration time. */
    public static final String EXP_TIME = "ExpireTime";
    /** Maximum caching size. */
    public static final String MAX_CACHE_SIZE = "MaxCacheSize";
    /** Maximum message size. */
    public static final String MAX_MSG_SIZE = "MaxMessageSize";

    /** Caching assertion QName for the caching policy. */
    public static final QName CACHING_ASSERTION_QNAME
            = new QName(WSCH_NS, "CachingAssertion", PREF_WSCH);
    /** XMLIdentifier QName in the caching policy. */
    public static final QName XML_IDENTIFIER_QNAME
            = new QName(WSCH_NS, XML_IDENTIFIER, PREF_WSCH);
    /** Expire time QName for the cache in the caching policy. */
    public static final QName CACHE_EXPIRATION_TIME_QNAME
            = new QName(WSCH_NS, EXP_TIME, PREF_WSCH);

    /** Maximum cache size QName for the cache in the caching policy. */
    public static final QName MAX_CACHE_SIZE_QNAME
            = new QName(WSCH_NS, MAX_CACHE_SIZE, PREF_WSCH);

    /** Maximum message size QName for the cache in the caching policy. */
    public static final QName MAX_MESSAGE_SIZE_QNAME
            = new QName(WSCH_NS, MAX_MSG_SIZE, PREF_WSCH);
}
