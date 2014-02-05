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
package org.wso2.carbon.rm.service;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.storage.jdbc.PersistentStorageManager;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.core.Resources;
import org.wso2.carbon.core.persistence.PersistenceException;
import org.wso2.carbon.core.persistence.PersistenceFactory;
import org.wso2.carbon.core.persistence.PersistenceUtils;
import org.wso2.carbon.core.persistence.file.ServiceGroupFilePersistenceManager;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.utils.Transaction;

import javax.xml.namespace.QName;
import java.util.List;

public class RMAdminService extends AbstractAdmin {

    private static final Log log = LogFactory.getLog(RMAdminService.class);

    private static final String RM_POLICY_ID = "RMPolicy";
    private PersistenceFactory persistenceFactory;
    private ServiceGroupFilePersistenceManager serviceGroupFilePM;

    private Registry registry;

    public RMAdminService() throws Exception {
        persistenceFactory = PersistenceFactory.getInstance(getAxisConfig());
        serviceGroupFilePM = persistenceFactory.getServiceGroupFilePM();

        registry = getConfigSystemRegistry();
    }

    public boolean isRMEnabled(String serviceName) throws AxisFault {
        AxisConfiguration axisConfiguration = getAxisConfig();
        AxisService axisServce = axisConfiguration.getServiceForActivation(serviceName);
        AxisModule sandeshaModule = axisConfiguration.getModule("sandesha2");
        return axisServce.isEngaged(sandeshaModule);
    }

    public void enableRM(String serviceName) throws AxisFault {
        AxisConfiguration axisConfiguration = getAxisConfig();
        AxisService axisService = axisConfiguration.getServiceForActivation(serviceName);
        String serviceGroupId = axisService.getAxisServiceGroup().getServiceGroupName();
        AxisModule sandesahModule = axisConfiguration.getModule("sandesha2");

        String serviceXPath = PersistenceUtils.getResourcePath(axisService);
        // engage at registry
        try {
            boolean transactionStarted = serviceGroupFilePM.isTransactionStarted(serviceGroupId);
            if (!transactionStarted) {
                serviceGroupFilePM.beginTransaction(serviceGroupId);
            }

            // Check if an association exist between servicePath and moduleResourcePath.
            List associations = serviceGroupFilePM.getAll(serviceGroupId, serviceXPath +
                    "/" + Resources.ModuleProperties.MODULE_XML_TAG +
                    PersistenceUtils.getXPathAttrPredicate(
                            Resources.ModuleProperties.TYPE,
                            Resources.Associations.ENGAGED_MODULES));
            boolean associationExist = false;
            String version = sandesahModule.getVersion().toString();
            if (sandesahModule.getVersion() == null) {
                version = Resources.ModuleProperties.UNDEFINED;
            }
            for (Object node : associations) {
                OMElement association = (OMElement) node;
                if (association.getAttributeValue(new QName(Resources.NAME)).equals(
                        sandesahModule.getName()) &&
                        association.getAttributeValue(new QName(Resources.VERSION)).equals(version)) {
                    associationExist = true;
                    break;
                }
            }

            //if RM is not found, add a new association
            if (!associationExist) {
                serviceGroupFilePM.put(serviceGroupId,
                        PersistenceUtils.createModule(sandesahModule.getName(), version,
                                Resources.Associations.ENGAGED_MODULES),
                        serviceXPath);
            }

            if (!transactionStarted) {
                serviceGroupFilePM.commitTransaction(serviceGroupId);
            }

/*            Association[] associations = registry.getAssociations(servicePath,
                    RegistryResources.Associations.ENGAGED_MODULES);
            boolean associationExist = false;
            for (Association association : associations) {
                if (association.getDestinationPath().equals(getModuleResourcePath(sandesahModule))) {
                    associationExist = true;
                    break;
                }
            }

            //if throttling is not found, add a new association
            if (!associationExist) {
                registry.addAssociation(servicePath, getModuleResourcePath(sandesahModule),
                        RegistryResources.Associations.ENGAGED_MODULES);
            }*/
        }/* catch (RegistryException e) {
            log.error("Error occured in engaging throttlin at registry", e);
            throw new AxisFault("Can not save to the registry");
        }*/ catch (PersistenceException e) {
            log.error("Error occured persisting RM", e);
            throw new AxisFault("Cannot persist RM metadata");
        }
        axisService.engageModule(sandesahModule);

    }

    public void disableRM(String serviceName) throws AxisFault {
        AxisConfiguration axisConfiguration = getAxisConfig();
        AxisService axisService = axisConfiguration.getServiceForActivation(serviceName);
        String serviceGroupId = axisService.getAxisServiceGroup().getServiceGroupName();
        AxisModule sandesahModule = axisConfiguration.getModule("sandesha2");

        String servicePath = RegistryResources.SERVICE_GROUPS
                + axisService.getAxisServiceGroup().getServiceGroupName()
                + RegistryResources.SERVICES + serviceName;

        ServiceGroupFilePersistenceManager sfpm = persistenceFactory.getServiceGroupFilePM();

        try {
            boolean transactionStarted = sfpm.isTransactionStarted(serviceGroupId);
            if (!transactionStarted) {
                sfpm.beginTransaction(serviceGroupId);
            }

            sfpm.delete(serviceGroupId, PersistenceUtils.getResourcePath(axisService) +
                    "/" + Resources.ModuleProperties.MODULE_XML_TAG +
                    PersistenceUtils.getXPathAttrPredicate(
                            Resources.NAME, sandesahModule.getName()) +
                    PersistenceUtils.getXPathAttrPredicate(
                            Resources.ModuleProperties.TYPE,
                            Resources.Associations.ENGAGED_MODULES));

            if (!transactionStarted) {
                sfpm.commitTransaction(serviceGroupId);
            }

        } catch (PersistenceException e) {
            log.error("Error ocurred in disengaging the module ", e);
            throw new AxisFault("Error ocurred in disengaging the module ");
        }

/*
        Registry registry = getConfigSystemRegistry();
        try {
            registry.removeAssociation(servicePath, getModuleResourcePath(sandesahModule),
                    RegistryResources.Associations.ENGAGED_MODULES);
            String policyPath = servicePath + RegistryResources.POLICIES + RM_POLICY_ID;
            if (registry.resourceExists(policyPath)) {
                registry.delete(policyPath);
            }

        } catch (RegistryException e) {
            log.error("Error ocurred in disengaging the module ", e);
            throw new AxisFault("Error ocurred in disengaging the module ");
        }
*/

        axisService.disengageModule(sandesahModule);
    }

    public void setParameters(String serviceName, RMParameterBean parameters) throws AxisFault {
        AxisConfiguration axisConfiguration = getAxisConfig();
        AxisService axisService = axisConfiguration.getServiceForActivation(serviceName);
        if (parameters != null) {
            Parameter sandeshaPolicyBeanParameter = axisService.getParameter(
                    Sandesha2Constants.SANDESHA_PROPERTY_BEAN);

            SandeshaPolicyBean sandeshaPolicyBean;
            if (sandeshaPolicyBeanParameter != null) {
                sandeshaPolicyBean = (SandeshaPolicyBean) sandeshaPolicyBeanParameter.getValue();
                if (sandeshaPolicyBean.getParent() == null) {
                    sandeshaPolicyBean = new SandeshaPolicyBean();
                    sandeshaPolicyBean.setParent(
                            (SandeshaPolicyBean) sandeshaPolicyBeanParameter.getValue());
                    axisService.addParameter(Sandesha2Constants.SANDESHA_PROPERTY_BEAN,
                            sandeshaPolicyBean);
                }
            } else {
                sandeshaPolicyBean = new SandeshaPolicyBean();
                axisService.addParameter(Sandesha2Constants.SANDESHA_PROPERTY_BEAN,
                        sandeshaPolicyBean);
            }

            sandeshaPolicyBean.setInactiveTimeoutInterval(
                    parameters.getInactivityTimeoutInterval(),
                    parameters.getInactivityTimeoutMeasure());

            sandeshaPolicyBean.setSequenceRemovalTimeoutInterval(
                    parameters.getSequenceRemovalTimeoutInterval(),
                    parameters.getSequenceRemovalTimeoutMeasure());
            sandeshaPolicyBean.setSequenceRemovalTimeoutMeasure(
                    parameters.getSequenceRemovalTimeoutMeasure());

            sandeshaPolicyBean.setAcknowledgementInterval(parameters.getAcknowledgementInterval());
            sandeshaPolicyBean.setRetransmissionInterval(parameters.getRetransmissionInterval());
            sandeshaPolicyBean.setExponentialBackoff(parameters.isExponentialBackoff());
            sandeshaPolicyBean.setMaximumRetransmissionCount(
                    parameters.getMaximumRetransmissionCount());

//            String serviceResourcePath = RegistryResources.SERVICE_GROUPS
//                + axisService.getAxisServiceGroup().getServiceGroupName()
//                + RegistryResources.SERVICES + serviceName;
            updatePolicy(axisService, sandeshaPolicyBean);
        }
    }

    private void updatePolicy(AxisService axisService, SandeshaPolicyBean sandeshaPolicyBean)
            throws AxisFault {
//        Registry registry = getConfigSystemRegistry();
        Policy sandeshaPolicy = new Policy();
        sandeshaPolicy.setId(RM_POLICY_ID);
        sandeshaPolicy.setName(RM_POLICY_ID);
        sandeshaPolicy.addPolicyComponent(sandeshaPolicyBean);

        String serviceGroupId = axisService.getAxisServiceGroup().getServiceGroupName();
        boolean isProxyService = PersistenceUtils.isProxyService(axisService);

        try {
            //to registry
            boolean registryTransactionStarted = true;
            registryTransactionStarted = Transaction.isStarted();
            if (isProxyService && !registryTransactionStarted) {
                registry.beginTransaction();
            }
            if (isProxyService) {
                String policyType = "" + PolicyInclude.AXIS_SERVICE_POLICY;
                String servicePath = PersistenceUtils.getRegistryResourcePath(axisService);
                persistenceFactory.getServicePM().persistPolicyToRegistry(sandeshaPolicy, policyType, servicePath);
            }

            //to file
            OMFactory omFactory = OMAbstractFactory.getOMFactory();
            OMElement policyWrapperEle = omFactory.createOMElement(Resources.POLICY, null);
            policyWrapperEle.addAttribute(Resources.ServiceProperties.POLICY_TYPE,
                    String.valueOf(PolicyInclude.AXIS_SERVICE_POLICY), null);

            OMElement idElement = omFactory.createOMElement(Resources.ServiceProperties.POLICY_UUID, null);
            idElement.setText(sandeshaPolicy.getId());
            policyWrapperEle.addChild(idElement);

            OMElement policyEleToPersist = PersistenceUtils.createPolicyElement(sandeshaPolicy);
            policyWrapperEle.addChild(policyEleToPersist);

            String serviceXPath = PersistenceUtils.getResourcePath(axisService);
            boolean transactionStarted = serviceGroupFilePM.isTransactionStarted(serviceGroupId);
            if (!transactionStarted) {
                serviceGroupFilePM.beginTransaction(serviceGroupId);
            }

            //check if "policies" section exists otherwise create, and delete the existing policy if exists
            if (!serviceGroupFilePM.elementExists(serviceGroupId, serviceXPath + "/" + Resources.POLICIES)) {
                serviceGroupFilePM.put(serviceGroupId,
                        omFactory.createOMElement(Resources.POLICIES, null), serviceXPath);
            } else {
                //you must manually delete the existing policy before adding new one.
                String pathToPolicy = serviceXPath + "/" + Resources.POLICIES +
                        "/" + Resources.POLICY +
                        PersistenceUtils.getXPathTextPredicate(
                                Resources.ServiceProperties.POLICY_UUID,
                                sandeshaPolicy.getId());
                if (serviceGroupFilePM.elementExists(serviceGroupId, pathToPolicy)) {
                    serviceGroupFilePM.delete(serviceGroupId, pathToPolicy);
                }
            }

            //put the policy
            serviceGroupFilePM.put(serviceGroupId, policyWrapperEle, serviceXPath + "/" + Resources.POLICIES);

            if (!serviceGroupFilePM.elementExists(serviceGroupId, serviceXPath +
                    PersistenceUtils.getXPathTextPredicate(
                            Resources.ServiceProperties.POLICY_UUID, sandeshaPolicy.getId()))) {
                serviceGroupFilePM.put(serviceGroupId, idElement.cloneOMElement(), serviceXPath);
            }

            if (!transactionStarted) {
                serviceGroupFilePM.commitTransaction(serviceGroupId);
            }
            if (isProxyService && !registryTransactionStarted) {
                registry.commitTransaction();
            }
        } catch (PersistenceException e) {
            log.error("Problem when setting parameter values", e);
            serviceGroupFilePM.rollbackTransaction(serviceGroupId);
            if (isProxyService) {
                try {
                    registry.rollbackTransaction();
                } catch (RegistryException re) {
                    log.error(e.getMessage(), e);
                }
            }
            throw new AxisFault("Problem when setting parameter values");
        } catch (Exception e) {
            log.error("Problem when setting parameter values", e);
            serviceGroupFilePM.rollbackTransaction(serviceGroupId);
            if (isProxyService) {
                try {
                    registry.rollbackTransaction();
                } catch (RegistryException re) {
                    log.error(e.getMessage(), e);
                }
            }
            throw new AxisFault("Problem when setting parameter values");
        }

        /*try {
            String resourcePath = serviceResourcePath + RegistryResources.POLICIES +
                    RM_POLICY_ID;

            Resource policyResource;
            if (registry.resourceExists(resourcePath)) {
                policyResource = registry.get(resourcePath);
            } else {
                policyResource = registry.newResource();
                policyResource.setProperty(RegistryResources.ServiceProperties.POLICY_TYPE,
                        String.valueOf(PolicyInclude.SERVICE_POLICY));
                policyResource.setProperty(RegistryResources.ServiceProperties.POLICY_UUID,
                        RM_POLICY_ID);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(baos);
            sandeshaPolicy.serialize(writer);
            writer.flush();
            policyResource.setContent(baos.toString());
            registry.put(resourcePath, policyResource);

        } catch (RegistryException e) {
            throw new AxisFault("Problem when setting parameter values");
        } catch (XMLStreamException e) {
            throw new AxisFault("Problem when setting parameter values");
        }*/
    }

    public RMParameterBean getParameters(String serviceName) throws AxisFault {
        AxisConfiguration axisConfiguration = getAxisConfig();
        AxisService axisService = axisConfiguration.getServiceForActivation(serviceName);
        RMParameterBean rmParameterBean = new RMParameterBean();

        Parameter sandeshaPolicyBeanParameter = axisService.getParameter(
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
            rmParameterBean.setRetransmissionInterval(
                    sandeshaPolicyBean.getRetransmissionInterval());
            rmParameterBean.setExponentialBackoff(
                    sandeshaPolicyBean.isExponentialBackoff());
            rmParameterBean.setMaximumRetransmissionCount(
                    sandeshaPolicyBean.getMaximumRetransmissionCount());
        }

        return rmParameterBean;
    }

/*  //commented out since this is unused
    private String getModuleResourcePath(AxisModule axisModule) {
        String moduleName = axisModule.getName();
        String moduleVersion = axisModule.getVersion().toString();
        if (moduleVersion == null || moduleVersion.length() == 0) {
            moduleVersion = "SNAPSHOT";
        }
        return RegistryResources.MODULES + moduleName + "/" + moduleVersion;
    }
*/

    public void ConfigurePermenentStorage(String connectionString,
                                          String driver,
                                          String userName,
                                          String password) throws AxisFault {

        ConfigurationContext configurationContext = getConfigContext();
        AxisConfiguration axisConfiguration = configurationContext.getAxisConfiguration();

        ModuleConfiguration moduleConfiguration = new ModuleConfiguration("sandesha2", null);
        moduleConfiguration.addParameter(new Parameter("db.connectionstring", connectionString));
        moduleConfiguration.addParameter(new Parameter("db.driver", driver));
        moduleConfiguration.addParameter(new Parameter("db.user", userName));
        moduleConfiguration.addParameter(new Parameter("db.password", password));

        axisConfiguration.addModuleConfig(moduleConfiguration);
        AxisModule sandeshaModule = axisConfiguration.getModule("sandesha2");

        PersistentStorageManager persistentStorageManager =
                new PersistentStorageManager(configurationContext);
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
}
