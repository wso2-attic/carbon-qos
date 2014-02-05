package org.wso2.carbon.caching.core.util;/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.core.CachableResponse;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;

public class CachingUtils {
    private static Log log = LogFactory.getLog(CachingUtils.class.getName());

    public static final String CACHING_CACHE_MANAGER = "caching.cache.manager";

    public static Cache<String,CachableResponse> getCacheForService(String serviceName){
        // acquiring  cache manager.
        Cache<String,CachableResponse> cache;
        CacheManager cacheManager = Caching.getCacheManagerFactory().getCacheManager(CACHING_CACHE_MANAGER);
        if (cacheManager != null) {
            cache = cacheManager.getCache(serviceName);
        } else {
            cache = Caching.getCacheManager().getCache(serviceName);
        }
        if (log.isDebugEnabled()) {
            log.debug("created org.wso2.carbon.caching.core cache : " + cache);
        }
        return cache;
    }

}
