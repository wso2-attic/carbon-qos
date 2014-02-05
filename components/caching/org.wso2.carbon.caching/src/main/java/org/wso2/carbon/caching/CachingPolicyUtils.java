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
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.All;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.builders.xml.XmlPrimtiveAssertion;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * The class <code>CachingPolicyUtils</code> provides methods to retrieve information from the available caching policy
 * and to persist any modifications.
 */
public class CachingPolicyUtils {
    public static final Log log = LogFactory.getLog(CachingComponentConstants.class);
//    private Registry configRegistry;

    public CachingPolicyUtils() {
    }

    /**
     * Generates a <code>CachingConfigData</code> instance from the given
     * <code>cachingPolicyComponent</code>.
     *
     * @param cachingPolicyComponent the caching policy component.
     * @return the generated <code>CachingConfigData</code> instance.
     */
    public CachingConfigData generateConfigurationFromPolicy(Policy cachingPolicyComponent) {
        CachingConfigData confData = new CachingConfigData();
        XmlPrimtiveAssertion[] assertionArr = retrieveConfigPrimitiveAssertions(
                cachingPolicyComponent);
        confData.setXmlIdentifier(assertionArr[0].getValue().getText());
        confData.setExpTime(Long.parseLong(assertionArr[1].getValue().getText()));
        confData.setMaxCacheSize(Integer.parseInt(assertionArr[2].getValue().getText()));
        confData.setMaxMsgSize(Integer.parseInt(assertionArr[3].getValue().getText()));
        return confData;
    }

    /**
     * Retrieves the four primitive caching assertions from the given caching policy component.
     *
     * @param cachingPolicyComponent the caching policy component.
     * @return an <code>XmlPrimtiveAssertion</code> array containing the four caching assertions.
     */
    public XmlPrimtiveAssertion[] retrieveConfigPrimitiveAssertions(
            Policy cachingPolicyComponent) {
        XmlPrimtiveAssertion[] assertionArr = new XmlPrimtiveAssertion[4];
        XmlPrimtiveAssertion primitiveAssertion;
        QName assertionName;
        for (Object policyObject : cachingPolicyComponent.getPolicyComponents()) {
            if (policyObject instanceof Policy) {
                for (Object allObject : ((Policy) policyObject).getPolicyComponents()) {
                    if (allObject instanceof All) {
                        for (Object assertionObject : ((All) allObject).getPolicyComponents()) {
                            if (assertionObject instanceof XmlPrimtiveAssertion) {
                                primitiveAssertion = (XmlPrimtiveAssertion) assertionObject;
                                if ((assertionName = primitiveAssertion.getName()).equals(
                                        CachingComponentConstants.XML_IDENTIFIER_QNAME)) {
                                    assertionArr[0] = primitiveAssertion;
                                } else if (assertionName.equals(
                                        CachingComponentConstants.CACHE_EXPIRATION_TIME_QNAME)) {
                                    assertionArr[1] = primitiveAssertion;
                                } else if (assertionName.equals(
                                        CachingComponentConstants.MAX_CACHE_SIZE_QNAME)) {
                                    assertionArr[2] = primitiveAssertion;
                                } else if (assertionName.equals(
                                        CachingComponentConstants.MAX_MESSAGE_SIZE_QNAME)) {
                                    assertionArr[3] = primitiveAssertion;
                                }
                            }
                        }
                    }
                }
            }
        }
        return assertionArr;
    }

    /**
     * Updates the values of the four caching assertions based on the given configuration data.
     *
     * @param cachingPolicyComponent the caching policy component.
     * @param confData               the <code>CachingConfigData</code> instance with new configuration data
     */
    public void updateCachingAssertion(Policy cachingPolicyComponent,
                                       CachingConfigData confData) {

        XmlPrimtiveAssertion[] assertionArr =
                retrieveConfigPrimitiveAssertions(cachingPolicyComponent);
        assertionArr[0].getValue().setText(confData.getXmlIdentifier());
        assertionArr[1].getValue().setText(String.valueOf(confData.getExpTime()));
        assertionArr[2].getValue().setText(String.valueOf(confData.getMaxCacheSize()));
        assertionArr[3].getValue().setText(String.valueOf(confData.getMaxMsgSize()));
    }

    /**
     * Retrieves the caching assertion and caching policy components.
     *
     * @param policyComponents the set of policy components associated with the service.
     * @return the caching assertion and caching policy components as a <code>Policy</code> array.
     */
    public Policy[] retrieveCachingAssertionAndPolicy(Collection policyComponents) {

        for (Object policyComponent : policyComponents) {
            if (policyComponent instanceof Policy) {
                Policy parentPolicy = (Policy) policyComponent;
                for (Iterator iterator = parentPolicy.getAlternatives();
                     iterator.hasNext();) {
                    Object object = iterator.next();
                    if (object instanceof List) {
                        List list = (List) object;
                        for (Object assertObj : list) {
                            if (assertObj instanceof XmlPrimtiveAssertion) {
                                XmlPrimtiveAssertion primitiveAssertion = (XmlPrimtiveAssertion)
                                        assertObj;
                                if (primitiveAssertion.getName().equals(CachingComponentConstants
                                        .CACHING_ASSERTION_QNAME)) {
                                    return new Policy[]{PolicyEngine
                                            .getPolicy(primitiveAssertion.getValue()), parentPolicy};
                                }

                            }
                        }
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Returns null as no caching assertion was found");
        }
        return null;
    }
}
