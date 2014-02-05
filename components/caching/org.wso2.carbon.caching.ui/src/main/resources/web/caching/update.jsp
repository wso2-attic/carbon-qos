<!--
/*
* Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
-->

<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.apache.axis2.client.Options" %>
<%@ page import="org.apache.axis2.client.ServiceClient" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.caching.stub.types.CachingConfigData" %>
<%@ page import="org.wso2.carbon.caching.stub.CachingAdminServiceStub" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.caching.stub.CachingComponentExceptionException" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<jsp:include page="../dialog/display_messages.jsp"/>

<%
    String BUNDLE = "org.wso2.carbon.caching.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

    //Obtaining the client-side ConfigurationContext instance.
    ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
            .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    //Server URL which is defined in the server.xml
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(),
            session) + "CachingAdminService";
    CachingAdminServiceStub stub = new CachingAdminServiceStub(configContext, serverURL);

    String cookie = (String) session.getAttribute(org.wso2.carbon.utils.ServerConstants.ADMIN_SERVICE_COOKIE);
    ServiceClient client = stub._getServiceClient();
    Options option = client.getOptions();
    option.setManageSession(true);
    option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

    String serviceName = request.getParameter("serviceName");
    String operationName = request.getParameter("opName");
    boolean global = false;
    boolean operationLevel = false;
    boolean engagedAtHigherLevel = false;
    if (serviceName == null) {
        global = true;
    } else if (serviceName != null && operationName != null) {
        operationLevel = true;
    }

    try {
        CachingConfigData confData = new CachingConfigData();
        if (request.getParameter("enable").equals("Yes")) {
            confData.setXmlIdentifier(request.getParameter("hashField"));
            String temp = request.getParameter("timeoutField");
            if (temp != null && temp.length() != 0) {
                confData.setExpTime(Long.parseLong(temp));
            }
            temp = request.getParameter("cacheField");
            if (temp != null && temp.length() != 0) {
                confData.setMaxCacheSize(Integer.parseInt(temp));
            }
            temp = request.getParameter("msgField");
            if (temp != null && temp.length() != 0) {
                confData.setMaxMsgSize(Integer.parseInt(temp));
            }
            if (global) {
                stub.globallyEngageCaching(confData);
            } else if (operationLevel) {
                engagedAtHigherLevel = stub.engageCachingForOperation(serviceName, operationName, confData);
            } else {
                stub.engageCachingForService(serviceName, confData);
            }
        } else {
            if (global) {
                stub.disengageGlobalCaching();
            } else if (operationLevel) {
                engagedAtHigherLevel = stub.disengageCachingForOperation(serviceName, operationName);
            } else {
                stub.disengageCachingForService(serviceName);
            }
        }

        String msg;
        if (engagedAtHigherLevel) {
            msg = resourceBundle.getString("already.applied.at.service.level");
            CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.WARNING, request);
        } else {
            msg = resourceBundle.getString("successfully.applied.configuration");
            CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.INFO, request);
        }

    } catch (NumberFormatException e) {
        String msg = resourceBundle.getString("numbers.format.error");
        CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.WARNING, request);
    } catch (CachingComponentExceptionException e) {
        CarbonUIMessage.sendCarbonUIMessage(resourceBundle.getString("error.communicating.with.back.end"), CarbonUIMessage.ERROR, request, e);
%>
<script type="text/javascript">
    location.href = "../admin/error.jsp";
</script>
<%
        return;
    }

    String backURL = (String) session.getAttribute("backURL");
    session.removeAttribute("backURL");
%>

<script type="text/javascript">
    location.href = "<%=backURL%>";
</script>