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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.apache.axis2.client.Options" %>
<%@ page import="org.apache.axis2.client.ServiceClient" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.caching.stub.CachingAdminServiceStub" %>
<%@ page import="org.wso2.carbon.caching.stub.types.CachingConfigData" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="java.text.MessageFormat" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>



<jsp:include page="../dialog/display_messages.jsp"/>

<%
    // values for default caching policy
    final String DEF_XML_IDENTIFIER = "org.wso2.carbon.caching.core.digest.DOMHASHGenerator";
    final String DEF_EXP_TIME = "60000";
    final String DEF_MAX_CACHE_SIZE = "1000";
    final String DEF_MAX_MSG_SIZE = "1000";

    String BUNDLE = "org.wso2.carbon.caching.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
    String[] options = new String[]{"Yes", "No"};
    String[] optionsValues = new String[]{resourceBundle.getString("yes"), resourceBundle.getString("no")};
    int[] optionsOrder = null;

    String serviceName = request.getParameter("serviceName");
    String operationName = request.getParameter("opName");
    String param = "", header;
    boolean global = false;
    boolean operationLevel = false;

    if (request.getParameter("backURL") != null) {
        session.setAttribute("backURL", request.getParameter("backURL"));
    }

    if (serviceName == null) {
        global = true;
        header = resourceBundle.getString("global.caching.configuration");
    } else {
        if (operationName == null) {
            param = "serviceName=" + serviceName;
            header = MessageFormat.format(resourceBundle.getString("caching.configuration.for.0.service"), serviceName);
        } else {
            operationLevel = true;
            param = "serviceName=" + serviceName + "&opName=" + operationName;
            header = MessageFormat.format(resourceBundle.getString("caching.configuration.for.0.operation"), operationName, serviceName);
        }
    }

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

    String visibility = "display: none;";
    String op1;
    String op2;
    boolean cachingEnabled;
    CachingConfigData confData;

    try {
        if (global) {
            cachingEnabled = stub.isCachingGloballyEnabled();
            confData = stub.getGlobalCachingPolicy();
        } else if (operationLevel) {
            cachingEnabled = stub.isCachingEnabledForOperation(serviceName, operationName);
            confData = stub.getCachingPolicyForOperation(serviceName, operationName);
        } else {
            cachingEnabled = stub.isCachingEnabledForService(serviceName);
            confData = stub.getCachingPolicyForService(serviceName);
        }
        if (confData == null) {
            cachingEnabled = false;
        }
    } catch (Exception e) {
        CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
        %>
        <script type="text/javascript">
            location.href = "../admin/error.jsp";
        </script>
        <%
        return;
    }

    if (cachingEnabled) {
        optionsOrder = new int[]{0, 1};
        op1 = resourceBundle.getString("yes");
        op2 = resourceBundle.getString("no");
        visibility = "display: inline;";
    } else {
        optionsOrder = new int[]{1, 0};
        op1 = resourceBundle.getString("no");
        op2 = resourceBundle.getString("yes");
    }

    // set default values
    String xmlIdentifier = DEF_XML_IDENTIFIER;
    String expTime = DEF_EXP_TIME;
    String maxCacheSize = DEF_MAX_CACHE_SIZE;
    String maxMsgSize = DEF_MAX_MSG_SIZE;

    if (confData != null) {
        xmlIdentifier = confData.getXmlIdentifier();
        expTime = String.valueOf(confData.getExpTime());
        if (confData.getMaxCacheSize() != 0) {
            maxCacheSize = String.valueOf(confData.getMaxCacheSize());
        }
        if (confData.getMaxMsgSize() != 0) {
            maxMsgSize = String.valueOf(confData.getMaxMsgSize());
        }
    }
%>

<fmt:bundle basename="org.wso2.carbon.caching.ui.i18n.Resources">
<carbon:breadcrumb label="enable.caching"
                   resourceBundle="org.wso2.carbon.caching.ui.i18n.Resources"
                   topPage="false" request="<%=request%>"/>

<script type="text/javascript">
    function submitData() {
        if (document.configForm.hashField.value == "") {
            CARBON.showErrorDialog("<fmt:message key="hash.generator.field.can.t.be.empty"/>");
            return;
        }
        document.configForm.action = "update.jsp?<%= param%>";
        document.configForm.submit();
    }

    function resetData() {
        document.configForm.action = "index.jsp?<%= param%>";
        document.configForm.submit();
    }

    function clearAll() {
        document.configForm.hashField.value = "";
        document.configForm.timeoutField.value = "";
        document.configForm.cacheField.value = "";
        document.configForm.msgField.value = "";
    }

    function display() {
        var configDiv = document.getElementById("configDiv");
        var enableCombo = document.getElementById("enable");
        if (enableCombo.value == "<%=options[0]%>") {
            configDiv.style.display = "inline";
        } else {
            CARBON.showConfirmationDialog("<fmt:message key="disengage.caching.confirm"/>", function() {
                configDiv.style.display = "none";
                if (<%= cachingEnabled%>) {
                    submitData();
                }
            }, function() {
                enableCombo.value = "<fmt:message key="yes"/>";
            });
        }
    }

    function setDefaults() {
        document.configForm.hashField.value = "org.wso2.caching.digest.DOMHASHGenerator";
        document.configForm.timeoutField.value = "60000";
        document.configForm.cacheField.value = "1000";
        document.configForm.msgField.value = "1000";
    }

    function goBack() {
        location.href = "<%= (String) session.getAttribute("backURL")%>";
    }

</script>


<div id="middle">

    <h2><%= header%>
    </h2>

    <div id="workArea">

        <div id="formset">
            <form id="form1" name="configForm" method="post" action="">
                <table class="caching_style">
                    <tr>
                        <td>
                            <fmt:message key="enable.caching"/>?
                        </td>
                        <td>
                            <select id="enable" name="enable" onchange="display();">
                                <%
                                    for (int optionOder : optionsOrder) {
                                %>
                                <option value="<%=options[optionOder]%>"><%=optionsValues[optionOder]%>
                                </option>
                                <%
                                    }
                                %>
                            </select>
                        </td>
                    </tr>
                </table>
                <p>&nbsp;</p>

                <div id="configDiv" style="<%= visibility%>">
                    <table class="styledLeft">
                        <tr>
                            <td class="formRaw">
                                <table class="normal">
                                    <tr>
                                        <td><fmt:message key="hash.generator"/></td>
                                        <td>
                                            <input name="hashField" type="text"
                                                   value="<%= xmlIdentifier%>"
                                                   size="80"/>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td><fmt:message key="timeout"/> (ms)</td>
                                        <td>
                                            <input name="timeoutField" type="text"
                                                   value="<%= expTime%>"/>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td><fmt:message key="memory.cache.size"/>
                                        </td>
                                        <td>
                                            <input name="cacheField" type="text"
                                                   value="<%= maxCacheSize%>"/>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td><fmt:message key="maximum.message.size"/>
                                            (bytes)
                                        </td>
                                        <td>
                                            <input name="msgField" type="text"
                                                   value="<%= maxMsgSize%>"/>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>

                        <tr>
                            <td class="buttonRow">

                                <input type="button" class="button" id="Submit" name="Submit"
                                       value="<fmt:message key="finish"/>"
                                       onclick="submitData();"/>
                                <input type="button" class="button" id="Reset" name="Reset"
                                       value="<fmt:message key="reset"/>"
                                       onclick="resetData();"/>
                                <input type="button" class="button" id="Default" name="Default"
                                       value="<fmt:message key="default"/>"
                                       onclick="setDefaults();"/>
                                <input type="button" class="button" id="Clear" name="Clear"
                                       value="<fmt:message key="clear"/>"
                                       onclick="clearAll();"/>
                                <input type="button" class="button"
                                       onclick="goBack();return false;"
                                       value="<fmt:message key="cancel"/>"/>
                            </td>
                        </tr>

                    </table>
                </div>
            </form>
        </div>
    </div>
</div>

</fmt:bundle>