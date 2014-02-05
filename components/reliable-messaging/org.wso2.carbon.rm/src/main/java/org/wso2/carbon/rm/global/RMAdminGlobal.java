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
package org.wso2.carbon.rm.global;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.description.PolicyInclude;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.neethi.Policy;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.storage.jdbc.PersistentStorageManager;
import org.jaxen.JaxenException;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.core.Resources;
import org.wso2.carbon.core.persistence.ModulePersistenceManager;
import org.wso2.carbon.core.persistence.PersistenceException;
import org.wso2.carbon.core.persistence.PersistenceFactory;
import org.wso2.carbon.core.persistence.PersistenceUtils;
import org.wso2.carbon.core.persistence.file.ModuleFilePersistenceManager;


public class RMAdminGlobal extends AbstractAdmin {

    public void setParameters(RMParameterBean parameters) throws AxisFault {


        ConfigurationContext configurationContext = getConfigContext();
        AxisConfiguration axisConfiguration = configurationContext.getAxisConfiguration();
        AxisModule sandeshaModule = axisConfiguration.getModule("sandesha2");

        // persisting parameters
        String moduleVersion = sandeshaModule.getVersion().toString();
        if (moduleVersion == null) {
            moduleVersion = Resources.ModuleProperties.UNDEFINED;
        }
        String moduleResourcePath = PersistenceUtils.getResourcePath(sandeshaModule);
//        String moduleResourcePath = Resources.ModuleProperties + "sandesha2/" + moduleVersion;

        updateParameter(moduleResourcePath, "db.connectionstring", parameters.getConnectionString(),
                sandeshaModule);
        updateParameter(moduleResourcePath, "db.driver", parameters.getDriverName(), sandeshaModule);
        updateParameter(moduleResourcePath, "db.user", parameters.getUserName(), sandeshaModule);
        updateParameter(moduleResourcePath, "db.password", parameters.getPassword(), sandeshaModule);

        // getting the policy bean to update
        SandeshaPolicyBean sandeshaPolicyBean
                = (SandeshaPolicyBean) axisConfiguration.getParameter(
                Sandesha2Constants.SANDESHA_PROPERTY_BEAN).getValue();

        sandeshaPolicyBean.setInactiveTimeoutInterval(
                parameters.getInactivityTimeoutInterval(), parameters.getInactivityTimeoutMeasure());

        sandeshaPolicyBean.setSequenceRemovalTimeoutInterval(
                parameters.getSequenceRemovalTimeoutInterval(),
                parameters.getSequenceRemovalTimeoutMeasure());
        sandeshaPolicyBean.setSequenceRemovalTimeoutMeasure(parameters.getSequenceRemovalTimeoutMeasure());

        sandeshaPolicyBean.setAcknowledgementInterval(parameters.getAcknowledgementInterval());
        sandeshaPolicyBean.setRetransmissionInterval(parameters.getRetransmissionInterval());
        sandeshaPolicyBean.setExponentialBackoff(parameters.isExponentialBackoff());
        sandeshaPolicyBean.setMaximumRetransmissionCount(parameters.getMaximumRetransmissionCount());

        // if it has set the storage as permenent the we need to initiate storage and put to the
        // configuration context.
        if ((parameters.getStorageManager() != null)
                && (parameters.getStorageManager().equals(Sandesha2Constants.PERMANENT_STORAGE_MANAGER))) {
            sandeshaPolicyBean.setPermanentStorageManagerClass(PersistentStorageManager.class.getName());
            // creating the persistace storage.
            PersistentStorageManager persistentStorageManager = new PersistentStorageManager(configurationContext);
            persistentStorageManager.initStorage(sandeshaModule);

            Parameter permenentStorage = axisConfiguration.getParameter(
                    Sandesha2Constants.PERMANENT_STORAGE_MANAGER);
            if (permenentStorage != null) {
                axisConfiguration.removeParameter(permenentStorage);
            }
            // change the permenetent storage manager.
            axisConfiguration.addParameter(Sandesha2Constants.PERMANENT_STORAGE_MANAGER,
                    persistentStorageManager);

        }

        // this parameter should load at the module init.
        Parameter storageManagerParameter = axisConfiguration.getParameter(
                Sandesha2Constants.STORAGE_MANAGER_PARAMETER);
        if (storageManagerParameter != null) {
            axisConfiguration.removeParameter(storageManagerParameter);
        }
        axisConfiguration.addParameter(Sandesha2Constants.STORAGE_MANAGER_PARAMETER,
                parameters.getStorageManager());
        MessageContext.getCurrentMessageContext().getConfigurationContext().
                setProperty("storageManagerInstance", null);

        updateParameter(moduleResourcePath, Sandesha2Constants.STORAGE_MANAGER_PARAMETER,
                parameters.getStorageManager(), sandeshaModule);

        updatePolicy(moduleResourcePath, sandeshaModule, sandeshaPolicyBean);

    }

    public RMParameterBean getParameters() throws AxisFault {

        AxisConfiguration axisConfiguration = getAxisConfig();
        AxisModule sandeshaModule = axisConfiguration.getModule("sandesha2");
        RMParameterBean rmParameterBean = new RMParameterBean();

        rmParameterBean.setConnectionString(getParameterValue("db.connectionstring", sandeshaModule));
        rmParameterBean.setDriverName(getParameterValue("db.driver", sandeshaModule));
        rmParameterBean.setPassword(getParameterValue("db.password", sandeshaModule));
        rmParameterBean.setUserName(getParameterValue("db.user", sandeshaModule));

        Parameter sandeshaPolicyBeanParameter = axisConfiguration.getParameter(
                Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
        if (sandeshaPolicyBeanParameter != null) {
            SandeshaPolicyBean sandeshaPolicyBean =
                    (SandeshaPolicyBean) sandeshaPolicyBeanParameter.getValue();

            // sandesha policy bean stored them in miliseconds so we make them seconds to display users.
            rmParameterBean.setInactivityTimeoutInterval(
                    sandeshaPolicyBean.getInactivityTimeoutInterval() / 1000);
            rmParameterBean.setInactivityTimeoutMeasure("seconds");

            rmParameterBean.setSequenceRemovalTimeoutInterval(
                    sandeshaPolicyBean.getSequenceRemovalTimeoutInterval() / 1000);
            rmParameterBean.setSequenceRemovalTimeoutMeasure("seconds");

            rmParameterBean.setAcknowledgementInterval(
                    sandeshaPolicyBean.getAcknowledgementInterval());
            rmParameterBean.setRetransmissionInterval(sandeshaPolicyBean.getRetransmissionInterval());
            rmParameterBean.setExponentialBackoff(sandeshaPolicyBean.isExponentialBackoff());
            rmParameterBean.setMaximumRetransmissionCount(
                    sandeshaPolicyBean.getMaximumRetransmissionCount());
        }

        if (axisConfiguration.getParameter(Sandesha2Constants.STORAGE_MANAGER_PARAMETER) != null) {
            rmParameterBean.setStorageManager(
                    (String) axisConfiguration.getParameter(
                            Sandesha2Constants.STORAGE_MANAGER_PARAMETER).getValue());
        }

        return rmParameterBean;

    }

    private String getParameterValue(String parameterName, ParameterInclude parameterInclude) {
        String parameterValue = null;
        if (parameterInclude.getParameter(parameterName) != null) {
            parameterValue = (String) parameterInclude.getParameter(parameterName).getValue();
        }
        return parameterValue;
    }

    private void updateParameter(String moduleResourcePath,
                                 String paramName,
                                 String paramValue,
                                 AxisModule axisModule) throws AxisFault {

        if (axisModule.getParameter(paramName) != null) {
            Parameter param = axisModule.getParameter(paramName);
            param.setValue(paramValue);
        } else {
            axisModule.addParameter(new Parameter(paramName, paramValue));
        }

        PersistenceFactory persistenceFactory = PersistenceFactory.getInstance(getAxisConfig());
        ModulePersistenceManager mpm = persistenceFactory.getModulePM();

        Parameter parameter = new Parameter(paramName, paramValue);
        try {
            mpm.updateParameter(axisModule.getName(), parameter, moduleResourcePath);


        } catch (PersistenceException e) {
            throw new AxisFault("Problem when setting parameter values", e);
        } catch (Exception e) {
            throw new AxisFault("Problem when setting parameter values", e);
        }

        /*Registry registry = getConfigSystemRegistry();
        try {
            String resourcePath = moduleResourcePath + RegistryResources.PARAMETERS + paramName;
            Resource paramResource = null;
            if (registry.resourceExists(resourcePath)) {
                paramResource = registry.get(resourcePath);
                paramResource.setContent("<parameter name=\"" + paramName + "\" locked=\"false\">" + paramValue + "</parameter>");
                registry.put(resourcePath, paramResource);
            } else {
                paramResource = registry.newResource();
                paramResource.setContent("<parameter name=\"" + paramName + "\" locked=\"false\">" + paramValue + "</parameter>");
                paramResource.addProperty(RegistryResources.NAME, paramName);
                registry.put(resourcePath, paramResource);
            }

        } catch (RegistryException e) {
            throw new AxisFault("Problem when setting parameter values");
        }*/
    }

    private void updatePolicy(String moduleResourcePath,
                              AxisModule sandeshaModule,
                              SandeshaPolicyBean sandeshaPolicyBean) throws AxisFault {

//        Registry registry = getConfigSystemRegistry();
        Policy sandeshaPolicy = new Policy();
        sandeshaPolicy.setId("RMPolicy");
        sandeshaPolicy.setName("RMPolicy");
        sandeshaPolicy.addPolicyComponent(sandeshaPolicyBean);

        try {
            PersistenceFactory pf = PersistenceFactory.getInstance(getAxisConfig());
            pf.getModulePM().persistModulePolicy(sandeshaModule.getName(),
                    sandeshaModule.getVersion().toString(), sandeshaPolicy, sandeshaPolicy.getId(),
                    String.valueOf(PolicyInclude.AXIS_MODULE_POLICY), PersistenceUtils.getResourcePath(sandeshaModule));
        } catch (PersistenceException e) {
            throw new AxisFault("Problem when persisting updated policy");
        } catch (JaxenException e) {
            throw new AxisFault("Problem when persisting updated policy");
        } catch (Exception e) {
            throw new AxisFault("Problem when persisting updated policy");
        }
    }


}
