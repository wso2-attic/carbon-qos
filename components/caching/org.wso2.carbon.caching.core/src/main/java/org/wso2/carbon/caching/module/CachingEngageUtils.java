package org.wso2.carbon.caching.module;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.PolicySubject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.core.CacheConfiguration;
import org.wso2.carbon.caching.core.CachingConstants;
import org.wso2.carbon.caching.core.CachingException;
import org.wso2.carbon.caching.core.policy.CachingPolicyProcessor;

public class CachingEngageUtils {

    private static Log log = LogFactory.getLog(CachingEngageUtils.class);

    public static void enguage(AxisDescription axisDescription) throws AxisFault {
        CacheConfiguration cacheConfig;
        PolicySubject policySubject = axisDescription.getPolicySubject();

        if (policySubject != null) {
            try {
                cacheConfig = CachingPolicyProcessor.processCachingPolicy(policySubject);
            } catch (CachingException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to engage the caching module:" +
                              "Error in processing caching policy", e);
                }
                throw new AxisFault("Unable to engage the caching module:" +
                                    "Error in processing caching policy");
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Using the default initializer for the CacheConfiguration");
            }
            cacheConfig = new CacheConfiguration();
        }

        // if there is a caching policy, set the cache config if not inherited from the parent
        if (cacheConfig != null) {
            axisDescription.addParameter(CachingConstants.CACHE_CONFIGURATION, cacheConfig);
        }
    }
}
