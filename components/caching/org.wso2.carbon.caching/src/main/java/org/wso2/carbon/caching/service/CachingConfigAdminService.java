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

package org.wso2.carbon.caching.service;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.PolicyInclude;
import org.apache.axis2.description.PolicySubject;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.wso2.carbon.caching.CachingComponentConstants;
import org.wso2.carbon.caching.CachingComponentException;
import org.wso2.carbon.caching.CachingConfigData;
import org.wso2.carbon.caching.CachingPolicyUtils;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.core.Resources;
import org.wso2.carbon.core.persistence.*;
import org.wso2.carbon.core.persistence.file.*;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.utils.Transaction;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * The <code>CachingConfigAdminService</code> class provides service methods to configure the
 * caching module for a given service.
 */
public class CachingConfigAdminService extends AbstractAdmin {

    /**
     * The logger object for this class.
     */
    private static final Log log = LogFactory.getLog(CachingConfigAdminService.class);

    private static final String defaultCachingPolicyId = "WSO2CachingPolicy";

    private PersistenceFactory persistenceFactory;

    private ServicePersistenceManager servicePM;

    private ServiceGroupFilePersistenceManager serviceGroupFilePM;

    private ModuleFilePersistenceManager ModuleFilePM;

    private Registry configRegistry;

    /**
     * Reference to Axis configuration.
     */
    private AxisConfiguration axisConfig = null;

    /**
     * The admin provides the implementations of the caching configuration options.
     */
    private CachingPolicyUtils cachingPolicyUtils;

    private static final String GLOBALLY_ENGAGED_PARAM_NAME = "globallyEngaged";

    private static final String GLOBALLY_ENGAGED_CUSTOM = "globallyEngagedCustom";

    private static final String ADMIN_SERVICE_PARAM_NAME = "adminService";

    private static final String HIDDEN_SERVICE_PARAM_NAME = "hiddenService";

    /**
     * Creates a new instance of the <code>CachingConfigAdminService</code>.
     *
     * @throws Exception ex
     */
    public CachingConfigAdminService() throws Exception {
        axisConfig = getAxisConfig();
        persistenceFactory = PersistenceFactory.getInstance(axisConfig);
        servicePM = persistenceFactory.getServicePM();
        serviceGroupFilePM = persistenceFactory.getServiceGroupFilePM();
        ModuleFilePM = persistenceFactory.getModuleFilePM();
        cachingPolicyUtils = new CachingPolicyUtils();

        configRegistry = getConfigSystemRegistry();
    }

    /**
     * Enables caching for the given service using the given configuration.
     *
     * @param serviceName the name of the service to which caching should be enabled.
     * @param confData    the caching configuration to be used.
     * @throws CachingComponentException if engaging of caching is unsuccessful.
     */
    public void engageCachingForService(String serviceName, CachingConfigData confData)
            throws CachingComponentException {
        if (log.isDebugEnabled()) {
            log.debug("Enabling caching for the service: " + serviceName);
        }

        //get AxisService from service name
        AxisService axisService = this.retrieveAxisService(serviceName);
        String serviceGroupId = axisService.getAxisServiceGroup().getServiceGroupName();

        //create service path in configRegistry
        String serviceXPath = PersistenceUtils.getResourcePath(axisService);

        try {
            this.enableCaching(serviceGroupId, axisService, confData, serviceXPath);
        } catch (AxisFault af) {
            throw new CachingComponentException("errorEngagingModuleToService",
                    new String[]{serviceName}, af, log);
        }

        if (log.isDebugEnabled()) {
            log.debug("Engaged caching for the Axis service: " + serviceName);
        }
    }

    /**
     * Enables caching for the given service using the given configuration.
     *
     * @param serviceName   the name of the service to which caching should be enabled.
     * @param confData      the caching configuration to be used.
     * @param operationName name of the operation
     * @return true if already engaged caching at the service level, else false
     * @throws CachingComponentException if engaging of caching is unsuccessful.
     */
    public boolean engageCachingForOperation(String serviceName, String
            operationName, CachingConfigData confData) throws CachingComponentException {
        if (log.isDebugEnabled()) {
            log.debug("Enabling caching for the operation: " + operationName
                    + " of service : " + serviceName);
        }

        //get AxisService from service name
        AxisService axisService = this.retrieveAxisService(serviceName);
        String serviceGroupId = axisService.getAxisServiceGroup().getServiceGroupName();


        // Retrieves caching module.
        AxisModule cachingModule = axisConfig.getModule(CachingComponentConstants.CACHING_MODULE);

        if (axisService.isEngaged(cachingModule)) {
            return true;
        }

        //get AxisOperation
        AxisOperation operation = axisService.getOperation(new QName(operationName));

        //create operation path in configRegistry
        String operationXPath = PersistenceUtils.getResourcePath(operation);

        try {
            this.enableCaching(serviceGroupId, operation, confData, operationXPath);
        } catch (AxisFault af) {
            throw new CachingComponentException("errorEngagingModuleToOperation",
                    new String[]{operationName}, af, log);
        }

        if (log.isDebugEnabled()) {
            log.debug("Engaged caching for the Axis operation: " + serviceName
                    + " of service : " + serviceName);
        }
        return false;
    }

    /**
     * Engages caching for service or operation
     *
     * @param serviceGroupId  SG
     * @param description    - AxisService or AxisOperation
     * @param confData       - incomming config data
     * @param engagementPath - service path or operation path
     * @throws CachingComponentException - errors
     * @throws AxisFault                 - errors on AxisDescription
     */
    private void enableCaching(String serviceGroupId, AxisDescription description, CachingConfigData confData,
                               String engagementPath)
            throws CachingComponentException, AxisFault {

        // Retrieves caching module.
        AxisModule cachingModule = axisConfig.getModule(CachingComponentConstants.CACHING_MODULE);

        try {
            boolean transactionStarted = serviceGroupFilePM.isTransactionStarted(serviceGroupId);
            if (!transactionStarted) {
                serviceGroupFilePM.beginTransaction(serviceGroupId);
            }
            boolean registryTransactionStarted = Transaction.isStarted();
            if (!registryTransactionStarted) {
                configRegistry.beginTransaction();
            }
            // Checks if an association exists between engagementPath and moduleResourcePath.
            List associations = serviceGroupFilePM.getAll(serviceGroupId, engagementPath +
                    "/" + Resources.ModuleProperties.MODULE_XML_TAG +
                    PersistenceUtils.getXPathAttrPredicate(
                            Resources.ModuleProperties.TYPE, Resources.Associations.ENGAGED_MODULES));
            boolean associationExist = false;
            String version = cachingModule.getVersion().toString();
            if (cachingModule.getVersion() == null) {
                version = Resources.ModuleProperties.UNDEFINED;
            }
            for (Object node : associations) {
                OMElement association = (OMElement) node;
                if (association.getAttributeValue(new QName(Resources.NAME)).equals(cachingModule.getName()) &&
                        association.getAttributeValue(new QName(Resources.VERSION)).equals(version)) {
                    associationExist = true;
                    break;
                }
            }

            // If no association exist between engagementPath and moduleResourcePath then
            // add new association between them.
            if (!associationExist) {
                serviceGroupFilePM.put(serviceGroupId,
                        PersistenceUtils.createModule(
                                cachingModule.getName(), version, Resources.Associations.ENGAGED_MODULES),
                        engagementPath);
            }

            // Gets a Policy object representing the configuration data.
            Policy policy = confData.toPolicy();

            //Add new policy to the description
            this.handleNewPolicyAddition(policy, description.getPolicySubject(), confData);

            // Save the policy
            try {
                //to registry if a proxy
                boolean isProxyService = false;
                String registryEngagementPath = "";
                if (description instanceof AxisService) {
                    AxisService service = (AxisService) description;
                    isProxyService = PersistenceUtils.isProxyService((AxisService) description);
                    registryEngagementPath = RegistryResources.SERVICE_GROUPS
                            + service.getAxisServiceGroup().getServiceGroupName()
                            + RegistryResources.SERVICES + service.getName();
                } else if (description instanceof AxisOperation) {
                    AxisService service = ((AxisOperation) description).getAxisService();
                    isProxyService = PersistenceUtils.isProxyService(service);
                    registryEngagementPath = RegistryResources.SERVICE_GROUPS
                            + service.getAxisServiceGroup().getServiceGroupName()
                            + RegistryResources.SERVICES + service.getName() +
                            RegistryResources.OPERATIONS + ((AxisOperation)description).getName().getLocalPart();

                } else {
                    log.debug("Proxy services - AxisDescription is neither an instance of AxisService nor AxisOperation.");
                }

                if(isProxyService) {
                    String policyType = "" + PolicyInclude.AXIS_SERVICE_POLICY;
                    String servicePath = registryEngagementPath;
                    if (description instanceof AxisOperation) {
                        servicePath = registryEngagementPath.substring(0, registryEngagementPath
                                .indexOf(RegistryResources.OPERATIONS));
                        policyType = "" + PolicyInclude.AXIS_OPERATION_POLICY;
                    }
                    persistenceFactory.getServicePM().persistPolicyToRegistry(policy, policyType, servicePath);
                }

                //persist policy to file
                String policyType = "" + PolicyInclude.AXIS_SERVICE_POLICY;
                String policyPath = engagementPath;
                if (description instanceof AxisOperation) {
                    policyPath = engagementPath.substring(0, engagementPath     //the service xpath - /service[@name="xxx"]
                            .indexOf("/" + Resources.OPERATION));               //todo make sure this returns the service path.
                    policyType = "" + PolicyInclude.AXIS_OPERATION_POLICY;
                }

                servicePM.persistServicePolicy( serviceGroupId, policy, policy.getId(), policyType,
                        policyPath, engagementPath );

/*
                OMFactory omFactory = OMAbstractFactory.getOMFactory();
                OMElement policyWrapperElement = omFactory.createOMElement(Resources.POLICY, null);


                policyWrapperElement.addAttribute(Resources.ServiceProperties.POLICY_TYPE, policyType, null);

                OMElement idElement = omFactory.createOMElement(Resources.ServiceProperties.POLICY_UUID, null);
                idElement.setText("" + policy.getId());
                policyWrapperElement.addChild(idElement);

                OMElement policyElementToPersist = PersistenceUtils.createPolicyElement(policy);
                policyWrapperElement.addChild(policyElementToPersist);

                if (!serviceGroupFilePM.elementExists(serviceGroupId, policyPath + "/" + Resources.POLICIES)) {
                    serviceGroupFilePM.put(serviceGroupId,
                            omFactory.createOMElement(Resources.POLICIES, null), policyPath);
                } else {
                    //you must manually delete the existing policy before adding new one.
                    String pathToPolicy = policyPath + "/" + Resources.POLICIES +
                            "/" + Resources.POLICY +
                            PersistenceUtils.getXPathTextPredicate(
                                    Resources.ServiceProperties.POLICY_UUID, policy.getId());
                    if (serviceGroupFilePM.elementExists(serviceGroupId, pathToPolicy)) {
                        serviceGroupFilePM.delete(serviceGroupId, pathToPolicy);
                    }
                }
                serviceGroupFilePM.put(serviceGroupId, policyWrapperElement, policyPath +
                        "/" + Resources.POLICIES);

                if (!serviceGroupFilePM.elementExists(serviceGroupId, policyPath +
                        PersistenceUtils.getXPathTextPredicate(
                                Resources.ServiceProperties.POLICY_UUID, policy.getId()))) {
                    serviceGroupFilePM.put(serviceGroupId, idElement.cloneOMElement(), policyPath);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Caching policy is saved in file system");
                }
*/

                description.engageModule(cachingModule);
                if (!transactionStarted) {
                    serviceGroupFilePM.commitTransaction(serviceGroupId);
                }

                if (!registryTransactionStarted) {
                    configRegistry.commitTransaction();
                }
            } catch (Exception e) {
                String msg = "Error persisting caching policy in file system.";
                log.error(msg, e);
                serviceGroupFilePM.rollbackTransaction(serviceGroupId);
                configRegistry.rollbackTransaction();
                throw new PersistenceException(msg, e);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            serviceGroupFilePM.rollbackTransaction(serviceGroupId);
            try { configRegistry.rollbackTransaction(); } catch (RegistryException re) { log.error(re.getMessage(), re); }
            throw new CachingComponentException("errorSavingPolicy", e, log);
        }

        // TODO - At the moment there is no notification mechanism to notify that the policy used by the particular
        // service has changed. So all we can do is to engage the module each time after doing everything with policy.
    }

    public void globallyEngageCaching(CachingConfigData confData) throws AxisFault,
            CachingComponentException {
        // Retrieves caching module.
        AxisModule cachingModule = axisConfig.getModule(CachingComponentConstants.CACHING_MODULE);
        // Engage caching only if it is not engaged already.
        String globalPath = PersistenceUtils.getResourcePath(cachingModule);

        try {
            ModuleFilePM.beginTransaction(cachingModule.getName());
            if (ModuleFilePM.elementExists(cachingModule.getName(), globalPath)) {
                OMElement element = (OMElement) ModuleFilePM.get(cachingModule.getName(), globalPath);
                if (!Boolean.parseBoolean(element
                        .getAttributeValue(new QName(GLOBALLY_ENGAGED_CUSTOM)))) {
                    element.addAttribute(GLOBALLY_ENGAGED_CUSTOM, Boolean.TRUE.toString(), null);
                    ModuleFilePM.setMetaFileModification(cachingModule.getName());
                }
            } else {
                OMFactory omFactory = OMAbstractFactory.getOMFactory();
                OMElement moduleElement = omFactory.createOMElement(Resources.VERSION, null);
                if (cachingModule.getVersion() != null) {
                    moduleElement.addAttribute(Resources.ModuleProperties.VERSION_ID, cachingModule.getVersion().toString(), null);
                } else {
                    moduleElement.addAttribute(Resources.ModuleProperties.VERSION_ID, Resources.ModuleProperties.UNDEFINED, null);
                }

                moduleElement.addAttribute(GLOBALLY_ENGAGED_CUSTOM, Boolean.TRUE.toString(), null);
                ModuleFilePM.put(cachingModule.getName(), moduleElement, Resources.ModuleProperties.ROOT_XPATH);
            }

            // Gets a Policy object representing the configuration data.
            Policy policy = confData.toPolicy();

            // Add new policy to module
            this.handleNewPolicyAddition(policy, cachingModule.getPolicySubject(), confData);

            // Save the policy.
            persistenceFactory.getModulePM().persistModulePolicy(cachingModule.getName(), cachingModule.getVersion().toString(),
                    policy, policy.getId(), "" + PolicyInclude.AXIS_MODULE_POLICY, globalPath);

            //no need to persist to registry since modules are not proxy services in any case.
            //todo but what if proxy services looks for the policy in registry when the module is globally engaged
            //Apparently ProxyObserver takes care of this. Need more testing. - kasung

            cachingModule.addParameter(new Parameter(GLOBALLY_ENGAGED_PARAM_NAME, "true"));

            //engage the module for every service which is not an admin service
            for (AxisService service : this.axisConfig.getServices().values()) {
                String adminParamValue =
                        (String) service.getParent().getParameterValue(ADMIN_SERVICE_PARAM_NAME);
                String hiddenParamValue =
                        (String) service.getParent().getParameterValue(HIDDEN_SERVICE_PARAM_NAME);

                //avoid admin and hidden services
                if ((adminParamValue != null && adminParamValue.length() != 0 &&
                        Boolean.parseBoolean(adminParamValue.trim())) ||
                        (hiddenParamValue != null && hiddenParamValue.length() != 0 &&
                                Boolean.parseBoolean(hiddenParamValue.trim())) ||
                        service.isClientSide() ) {
                    continue;
                }
                this.engageCachingForService(service.getName(), confData);
            }
            ModuleFilePM.commitTransaction(cachingModule.getName());
        } catch (Exception e) {
            ModuleFilePM.rollbackTransaction(cachingModule.getName());
            log.error("Error occurred in globally engaging caching", e);
            throw new CachingComponentException("errorEngagingModuleAtRegistry", log);
        }
    }

    /**
     * Disables caching from the given service
     *
     * @param serviceName the name of the service from which caching should be disabled
     * @throws CachingComponentException if disengaging caching is unsuccessful
     */
    public void disengageCachingForService(String serviceName) throws CachingComponentException {
        if (log.isDebugEnabled()) {
            log.debug("Disabling caching for the service: " + serviceName);
        }

        // Retrieves the AxisService instance corresponding to the serviceName.
        AxisService axisService = retrieveAxisService(serviceName);
        String serviceGroupId = axisService.getAxisServiceGroup().getServiceGroupName();

        String serviceXPath = PersistenceUtils.getResourcePath(axisService);
        boolean isProxyService = PersistenceUtils.isProxyService(axisService);
        String registryCachingPolicyPath =
                PersistenceUtils.getRegistryResourcePath(axisService) + RegistryResources.POLICIES +
                defaultCachingPolicyId ;

        try {
            this.disableCaching(serviceGroupId, axisService, serviceXPath, isProxyService, registryCachingPolicyPath);
        } catch (AxisFault af) {
            throw new CachingComponentException("errorDisablingCaching",
                    new String[]{serviceName}, af, log);
        }

        if (log.isDebugEnabled()) {
            log.debug("Disengaged caching for the Axis service: " + serviceName);
        }
    }

    /**
     * Disables caching from the given operation
     *
     * @param serviceName   the name of the service from which caching should be disabled
     * @param operationName the name of the operation
     * @return true if already engaged caching at the service level, else false
     * @throws CachingComponentException if disengaging caching is unsuccessful
     */
    public boolean disengageCachingForOperation(String serviceName, String
            operationName) throws CachingComponentException {
        if (log.isDebugEnabled()) {
            log.debug("Disabling caching for the operation: " + operationName +
                    "service: " + serviceName);
        }

        // Retrieves the AxisService instance corresponding to the serviceName.
        AxisService axisService = retrieveAxisService(serviceName);
        String serviceGroupId = axisService.getAxisServiceGroup().getServiceGroupName();
        boolean isProxyService = PersistenceUtils.isProxyService(axisService);
        String registryCachingPolicyPath =
                PersistenceUtils.getRegistryResourcePath(axisService) + RegistryResources.POLICIES +
                        defaultCachingPolicyId ;

        // Retrieves caching module.
        AxisModule cachingModule = axisConfig.getModule(CachingComponentConstants.CACHING_MODULE);

        if (axisService.isEngaged(cachingModule)) {
            return true;
        }

        //get AxisOperation
        AxisOperation operation = axisService.getOperation(new QName(operationName));
        String operationXPath = PersistenceUtils.getResourcePath(operation);

        try {
            this.disableCaching(serviceGroupId, operation, operationXPath, isProxyService, registryCachingPolicyPath);
        } catch (AxisFault af) {
            throw new CachingComponentException("errorDisablingCaching",
                    new String[]{serviceName + "operation : " + operationName}, af, log);
        }

        if (log.isDebugEnabled()) {
            log.debug("Disengaged caching for the Axis operation: " + operationName +
                    "service: " + serviceName);
        }
        return false;
    }

    /**
     * disengages caching from description
     *
     * @param serviceGroupId sg name
     * @param description    - AxisService or AxisOperation
     * @param engagementPath - service path or operation path
     * @throws CachingComponentException - error
     * @throws AxisFault                 - error on AxisDescription
     */
    private void disableCaching(String serviceGroupId, AxisDescription description, String engagementPath,
                                boolean isProxyService, String registryCachingPolicyPath)
            throws CachingComponentException, AxisFault {
        // Removes caching from both the configRegistry and the description
        try {
            AxisModule cachingModule = axisConfig.getModule(CachingComponentConstants.CACHING_MODULE);

            boolean isTransactionStarted = serviceGroupFilePM.isTransactionStarted(serviceGroupId);
            if (!isTransactionStarted) {
                serviceGroupFilePM.beginTransaction(serviceGroupId);
            }
            // Removes the association from the file system.
            serviceGroupFilePM.delete(serviceGroupId, engagementPath +
                    "/" + Resources.ModuleProperties.MODULE_XML_TAG +
                    PersistenceUtils.getXPathAttrPredicate(Resources.NAME, cachingModule.getName()) +
                    PersistenceUtils.getXPathAttrPredicate(Resources.ModuleProperties.TYPE,
                            Resources.Associations.ENGAGED_MODULES));

            if (isProxyService) {
                boolean registryTransactionStarted = Transaction.isStarted();
                if (!registryTransactionStarted) {
                    configRegistry.beginTransaction();
                }

                if (configRegistry.resourceExists(registryCachingPolicyPath)) {
                    configRegistry.delete(registryCachingPolicyPath);
                } else {
                    log.warn("Could not delete the Caching policy because it " +
                            "does not exist at " + registryCachingPolicyPath);
                }

                if (!registryTransactionStarted) {
                    configRegistry.commitTransaction();
                }
            }

            // Disengage from description
            description.disengageModule(cachingModule);

            if (!isTransactionStarted) {
                serviceGroupFilePM.commitTransaction(serviceGroupId);
            }
        } catch (Exception e) {
            serviceGroupFilePM.rollbackTransaction(serviceGroupId);
            log.error("errorDisablingAtRegistry", e);
            throw new CachingComponentException("errorDisablingAtRegistry", e, log);
        }
    }

    public void disengageGlobalCaching() throws CachingComponentException {
        if (log.isDebugEnabled()) {
            log.debug("Disengaging globally engaged caching");
        }
        //get the caching module from the current axis config
        AxisModule module = this.axisConfig
                .getModule(CachingComponentConstants.CACHING_MODULE);

        //disengage the caching module
        try {
            boolean isTransactionStarted = ModuleFilePM.isTransactionStarted(module.getName());
            if (!isTransactionStarted) {
                ModuleFilePM.beginTransaction(module.getName());
            }

            String modulePath = PersistenceUtils.getResourcePath(module);
            if (ModuleFilePM.elementExists(module.getName(), modulePath)) {
                OMElement element = (OMElement) ModuleFilePM.get(module.getName(), modulePath);
                if (!Boolean.parseBoolean(element
                        .getAttributeValue(new QName(GLOBALLY_ENGAGED_CUSTOM)))) {
                    element.addAttribute(GLOBALLY_ENGAGED_CUSTOM, Boolean.FALSE.toString(), null);
                    ModuleFilePM.setMetaFileModification(module.getName());
                }
            }

            Parameter param = module.getParameter(GLOBALLY_ENGAGED_PARAM_NAME);
            if (param != null) {
                module.removeParameter(module.getParameter(GLOBALLY_ENGAGED_PARAM_NAME));
            }

            //disengage throttling from all the services which are not admin services
            for (AxisService service : this.axisConfig.getServices().values()) {
                String adminParamValue =
                        (String) service.getParent().getParameterValue(ADMIN_SERVICE_PARAM_NAME);
                String hiddenParamValue =
                        (String) service.getParent().getParameterValue(HIDDEN_SERVICE_PARAM_NAME);

                if ((adminParamValue != null && adminParamValue.length() != 0 &&
                        Boolean.parseBoolean(adminParamValue.trim())) ||
                        (hiddenParamValue != null && hiddenParamValue.length() != 0 &&
                                Boolean.parseBoolean(hiddenParamValue.trim()))) {
                    continue;
                }
                this.disengageCachingForService(service.getName());
            }
            if (!isTransactionStarted) {
                ModuleFilePM.commitTransaction(module.getName());
            }
        } catch (PersistenceException e) {
            ModuleFilePM.rollbackTransaction(module.getName());
            log.error("Error occured while removing global caching from configRegistry", e);
            throw new CachingComponentException("errorDisablingAtRegistry", log);
        } catch (AxisFault e) {
            ModuleFilePM.rollbackTransaction(module.getName());
            log.error("Error occured while disengaging module from AxisService", e);
            throw new CachingComponentException("errorDisablingCaching", log);
        }
    }

    /**
     * Retrieves the caching configuration associated with the given service
     *
     * @param serviceName the name of the service from which caching configuration
     *                    should be retrieved
     * @return <code>CachingConfigData</code> instance containing the configuration details
     * @throws CachingComponentException if retrieving configuration is unsuccessful
     */
    public CachingConfigData getCachingPolicyForService(String serviceName)
            throws CachingComponentException {
        // Retrieves the AxisService instance corresponding to the serviceName.
        AxisService service = retrieveAxisService(serviceName);

        Collection policyComponents = service.getPolicySubject().getAttachedPolicyComponents();
        return getCachingConfig(policyComponents);
    }

    /**
     * Retrieves the caching configuration associated with the given service
     *
     * @param serviceName   the name of the service from which caching configuration
     *                      should be retrieved
     * @param operationName name of the operation
     * @return <code>CachingConfigData</code> instance containing the configuration details
     * @throws CachingComponentException if retrieving configuration is unsuccessful
     */
    public CachingConfigData getCachingPolicyForOperation(String serviceName, String operationName)
            throws CachingComponentException {
        // Retrieves the AxisService instance corresponding to the serviceName.
        AxisService service = retrieveAxisService(serviceName);
        AxisOperation operation = service.getOperation(new QName(operationName));

        AxisModule module = this.axisConfig.getModule(CachingComponentConstants.CACHING_MODULE);

        Policy[] arr = null;
        Collection policyComponents = null;

        if (service.isEngaged(module)) {
            policyComponents = service.getPolicySubject().getAttachedPolicyComponents();
            arr = cachingPolicyUtils.retrieveCachingAssertionAndPolicy(policyComponents);
        }
        if (arr == null) {
            policyComponents = operation.getPolicySubject().getAttachedPolicyComponents();
        }
        return getCachingConfig(policyComponents);
    }


    /**
     * Returns the current global configuration
     *
     * @return CachingConfigData
     * @throws CachingComponentException - on error
     */
    public CachingConfigData getGlobalCachingPolicy() throws CachingComponentException {
        AxisModule module = this.axisConfig
                .getModule(CachingComponentConstants.CACHING_MODULE);

        Collection policyComponents = module.getPolicySubject().getAttachedPolicyComponents();
        return getCachingConfig(policyComponents);
    }


    /**
     * Handles the policy addition to given policy subject
     *
     * @param builtPolicy   - policy built from new configs
     * @param policySubject - service or operation level policy subject
     * @param confData      - new config data
     */
    private void handleNewPolicyAddition(Policy builtPolicy, PolicySubject
            policySubject, CachingConfigData confData) {
        Collection policyComponents = policySubject.getAttachedPolicyComponents();
        if (policyComponents == null) {

            // No policy components found. So we add the new caching policy directly.
            policySubject.attachPolicy(builtPolicy);
            if (log.isDebugEnabled()) {
                log.debug("Used the new policy configuration as no " +
                        "existing policy components were found");
            }
        } else {
            Policy[] arr = cachingPolicyUtils.retrieveCachingAssertionAndPolicy(policyComponents);
            if (arr == null) {

                // No caching assertion found. So we add the new caching policy directly.
                policySubject.attachPolicy(builtPolicy);
                if (log.isDebugEnabled()) {
                    log.debug("Used the new policy configuration as no existing " +
                            "caching assertion was found");
                }
            } else {

                // Caching assertion found in service policy. So we update it with the given
                // configuration values.
                cachingPolicyUtils.updateCachingAssertion(arr[0], confData);
                policySubject.updatePolicy(arr[1]);
                if (log.isDebugEnabled()) {
                    log.debug("The existing caching policy is updated with the new configuration data");
                }
            }
        }
    }

    private CachingConfigData getCachingConfig(Collection policyComponents) {
        Policy[] arr;

        // Checks whether the service is already associated with any caching policy.
        if (policyComponents != null &&
                (arr = cachingPolicyUtils.retrieveCachingAssertionAndPolicy(policyComponents)) != null) {
            if (log.isDebugEnabled()) {
                log.debug("Returns the configuration data generated from the exisiting caching policy");
            }
            return cachingPolicyUtils.generateConfigurationFromPolicy(arr[0]);
        }
        return null;
    }

    /**
     * Returns true if the given eservice is engaged with caching
     *
     * @param serviceName the name of the service which is to be checked for the availability of
     *                    caching
     * @return <cdoe>true</code> if the service for the given <code>serviceName</code> is engaged
     *         with caching and else <code>false</code>
     * @throws CachingComponentException if retrieving of the availability of caching is unsuccessful
     */
    public boolean isCachingEnabledForService(String serviceName) throws CachingComponentException {
        AxisService service = retrieveAxisService(serviceName);
        AxisModule module = axisConfig.getModule(CachingComponentConstants.CACHING_MODULE);
        return service.isEngaged(module);
    }

    /**
     * Returns true if the given eservice is engaged with caching
     *
     * @param serviceName   the name of the service which is to be checked for the availability of
     *                      caching
     * @param operationName name of the operation
     * @return <cdoe>true</code> if the service for the given <code>serviceName</code> is engaged
     *         with caching and else <code>false</code>
     * @throws CachingComponentException if retrieving of the availability of caching is unsuccessful
     */
    public boolean isCachingEnabledForOperation(String serviceName, String operationName)
            throws CachingComponentException {
        AxisService service = retrieveAxisService(serviceName);
        AxisOperation operation = service.getOperation(new QName(operationName));
        AxisModule module = axisConfig.getModule(CachingComponentConstants.CACHING_MODULE);
        return operation.isEngaged(module) || service.isEngaged(module);
    }

    public boolean isCachingGloballyEnabled() throws CachingComponentException {
        AxisModule module = axisConfig.getModule(CachingComponentConstants.CACHING_MODULE);
        Parameter param = module.getParameter(GLOBALLY_ENGAGED_PARAM_NAME);
        if (param != null) {
            String globallyEngaged = (String) param.getValue();
            if (globallyEngaged != null && globallyEngaged.length() != 0) {
                return Boolean.parseBoolean(globallyEngaged.trim());
            }
        }
        return false;
    }

    /**
     * Retrieves the <code>AxisService</code> instance for the given <code>serviceName</code>.
     *
     * @param serviceName the name of the axis service to be retrieved
     * @return the <code>AxisService</code> instance for the given <code>serviceName</code>.
     * @throws CachingComponentException if the retrieval is unsuccessful
     */
    private AxisService retrieveAxisService(String serviceName) throws CachingComponentException {
        if (log.isDebugEnabled()) {
            log.debug("Retrieving Axis service: " + serviceName);
        }
        AxisService axisService = axisConfig.getServiceForActivation(serviceName);
        if (axisService == null) {
            throw new CachingComponentException("noSuchService",
                    new String[]{serviceName}, log);
        }
        return axisService;
    }

    /**
     * Resource path of the caching module including its version.
     *
     * @param axisModule AxisModule
     * @return module loation
     */
    private String getModuleResourcePath(AxisModule axisModule) {
        String moduleName = axisModule.getName();
        String moduleVersion = axisModule.getVersion().toString();
        if (moduleVersion == null || moduleVersion.length() == 0) {
            moduleVersion = "SNAPSHOT";
        }
        return RegistryResources.MODULES + moduleName + "/" + moduleVersion;
    }
}
