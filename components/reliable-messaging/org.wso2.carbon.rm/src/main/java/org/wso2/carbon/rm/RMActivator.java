/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
package org.wso2.carbon.rm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;

import javax.xml.namespace.QName;

public class RMActivator implements BundleActivator {

    private static final Log log = LogFactory.getLog(RMActivator.class);

    private static String[] builders =
            new String[]{"org.apache.sandesha2.policy.builders.RMAssertionBuilder"};

    public void start(BundleContext context) throws Exception {
            Bundle rmBundle = context.getBundle();
            for (String buildeName : builders) {
                Class aClass = rmBundle.loadClass(buildeName.trim());
                AssertionBuilder builder = (AssertionBuilder) aClass.newInstance();
                QName[] knownElements = builder.getKnownElements();
                for (QName knownElement : knownElements) {
                    AssertionBuilderFactory.registerBuilder(knownElement, builder);
                }
            }

    }

    public void stop(BundleContext context) throws Exception {
    }
}
