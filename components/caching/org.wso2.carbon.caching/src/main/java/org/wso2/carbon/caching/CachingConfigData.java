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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;

/**
 * The class <code>CachingConfigData</code> is a container to hold caching configuration data.
 * This data is picked from the caching policy associated with a service.
 * Subsequently, an instance of <code>CachingConfigData</code> is used to capture the updates
 * on the configuration form the user interface.
 */
public class CachingConfigData {
    /**
     * The xmlIdentifier identifies the org.wso2.caching.digest.DigestGenerator
     * implementation class. An instance of the specified class is used to generate
     * the digest of the payload.
     */
    private String xmlIdentifier;

    /**
     * The expTime is the expiration time in milliseconds for cached messages.
     */
    private long expTime;

    /**
     * The maxCacheSize is the maximum possible number of cached messages in the system.
     * Once this limit is reached any new message will not be cached unless there exist
     * an expired cached message.
     */
    private int maxCacheSize;

    /**
     * The maxMsgSize is the maximum payload size in bytes which determines whether a message
     * should be cached or not. The reason behind this condition is due to the fact that digest
     * generation is expensive in terms of processor time. Therefore caching large messages will
     * reduce the advantage of caching.
     */
    private int maxMsgSize;

    /**
     * XML identifier accessor.
     *
     * @return XML identifier
     */
    public String getXmlIdentifier() {
        return xmlIdentifier;
    }

    /**
     * XML identifier mutator.
     *
     * @param xmlIdentifier XML identifier string
     */
    public void setXmlIdentifier(String xmlIdentifier) {
        this.xmlIdentifier = xmlIdentifier;
    }

    /**
     * Expiration time accessor.
     *
     * @return Expiration time
     */
    public long getExpTime() {
        return expTime;
    }

    /**
     * Expiration time mutator.
     *
     * @param expTime Expiration time
     */
    public void setExpTime(long expTime) {
        this.expTime = expTime;
    }

    /**
     * Maximum cache size accessor.
     *
     * @return Maximum cache size
     */
    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    /**
     * Maximum cache size mutator.
     *
     * @param maxCacheSize Maximum cache size
     */
    public void setMaxCacheSize(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * Maximum message size accessor.
     *
     * @return Maximum message size
     */
    public int getMaxMsgSize() {
        return maxMsgSize;
    }

    /**
     * Maximum message size mutator.
     *
     * @param maxMsgSize Maximum message size
     */
    public void setMaxMsgSize(int maxMsgSize) {
        this.maxMsgSize = maxMsgSize;
    }

    /**
     * Generates a <code>Policy</code> object based on the configuration data.
     *
     * @return a <code>Policy</code> object represeting the caching configuration data.
     */
    public Policy toPolicy() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace wsuNs = fac.createOMNamespace(
                CachingComponentConstants.WSU_NS, CachingComponentConstants.PREF_WSU);
        OMNamespace wspNs = fac.createOMNamespace(
                CachingComponentConstants.WSP_NS, CachingComponentConstants.PREF_WSP);
        OMNamespace wschNs = fac.createOMNamespace(
                CachingComponentConstants.WSCH_NS, CachingComponentConstants.PREF_WSCH);

        OMElement xmlIdentifierEle = fac.createOMElement(
                CachingComponentConstants.XML_IDENTIFIER, wschNs);
        xmlIdentifierEle.setText(xmlIdentifier);

        OMElement expTimeEle = fac.createOMElement(CachingComponentConstants.EXP_TIME, wschNs);
        expTimeEle.setText(String.valueOf(expTime));

        OMElement maxCacheSizeEle = fac.createOMElement(
                CachingComponentConstants.MAX_CACHE_SIZE, wschNs);
        maxCacheSizeEle.setText(String.valueOf(maxCacheSize));

        OMElement maxMsgSizeEle = fac.createOMElement(
                CachingComponentConstants.MAX_MSG_SIZE, wschNs);
        maxMsgSizeEle.setText(String.valueOf(maxMsgSize));

        OMElement allEle = fac.createOMElement("All", wspNs);
        allEle.addChild(xmlIdentifierEle);
        allEle.addChild(expTimeEle);
        allEle.addChild(maxCacheSizeEle);
        allEle.addChild(maxMsgSizeEle);

        OMElement policyEle = fac.createOMElement("Policy", wspNs);
        policyEle.addChild(allEle);

        OMElement cachingAssertionEle = fac.createOMElement("CachingAssertion", wschNs);
        cachingAssertionEle.addChild(policyEle);

        OMElement rootPolicyEle = fac.createOMElement("Policy", wspNs);
        rootPolicyEle.addAttribute(fac.createOMAttribute("Id", wsuNs, "WSO2CachingPolicy"));
        rootPolicyEle.addChild(cachingAssertionEle);

        // Creates a Policy object out of the OMElement
        return PolicyEngine.getPolicy(rootPolicyEle);
    }

}
