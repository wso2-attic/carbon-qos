<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 ~
 ~ WSO2 Inc. licenses this file to you under the Apache License,
 ~ Version 2.0 (the "License"); you may not use this file except
 ~ in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~    http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 -->
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.apache.axis2.client.Options" %>
<%@ page import="org.apache.axis2.client.ServiceClient" %>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="java.rmi.RemoteException" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.rm.stub.service.RMAdminServiceStub"%>
<%@ page import="org.wso2.carbon.rm.stub.service.xsd.RMParameterBean"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
    <link href="../styles/main.css" rel="stylesheet" type="text/css" media="all"/>
<title>Untitled Document</title>
</head>

<body>
<%
    String BUNDLE = "org.wso2.carbon.rm.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

    String serviceName = request.getParameter("serviceName");

    ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
            .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    //Server URL which is defined in the server.xml
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(),
            session) + "RMAdminService.RMAdminServiceHttpsSoap12Endpoint";

    RMAdminServiceStub stub = new RMAdminServiceStub(configContext, serverURL);

    String cookie = (String)session.getAttribute(org.wso2.carbon.utils.ServerConstants.ADMIN_SERVICE_COOKIE);
    ServiceClient client = stub._getServiceClient();
    Options option = client.getOptions();
    option.setManageSession(true);
    option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

    String inactivityTimeoutMeasure = request.getParameter("inactivityTimeoutMeasure");
    long inactivityTimeoutInterval = Long.parseLong(request.getParameter("inactivityTimeoutInterval"));

    String sequenceRemovalTimeoutMeasure = request.getParameter("sequenceRemovalTimeoutMeasure");
    long sequenceRemovalTimeoutInterval = Long.parseLong(request.getParameter("sequenceRemovalTimeoutInterval"));

    long acknowledgementInterval = Long.parseLong(request.getParameter("acknowledgementInterval"));

    long retransmissionInterval = Long.parseLong(request.getParameter("retransmissionInterval"));

    boolean exponentialBackoff = Boolean.parseBoolean(request.getParameter("exponentialBackoff"));

    int maximumRetransmissionCount = Integer.parseInt(request.getParameter("maximumRetransmissionCount"));

    boolean isRMEngaged = Boolean.parseBoolean(request.getParameter("isRMEngaged"));



    RMParameterBean rmParameterBean = new RMParameterBean();
    rmParameterBean.setInactivityTimeoutMeasure(inactivityTimeoutMeasure);
    rmParameterBean.setInactivityTimeoutInterval(inactivityTimeoutInterval);


    rmParameterBean.setSequenceRemovalTimeoutMeasure(sequenceRemovalTimeoutMeasure);
    rmParameterBean.setSequenceRemovalTimeoutInterval(sequenceRemovalTimeoutInterval);

    rmParameterBean.setAcknowledgementInterval(acknowledgementInterval);
    rmParameterBean.setRetransmissionInterval(retransmissionInterval);
    rmParameterBean.setExponentialBackoff(exponentialBackoff);
    rmParameterBean.setMaximumRetransmissionCount(maximumRetransmissionCount);

    try {
        String msg;
        if (isRMEngaged){
            // TODO: Use a single service call to get this done
            stub.setParameters(serviceName, rmParameterBean);
            stub.enableRM(serviceName);
            msg = resourceBundle.getString("successfully.applied.configuration");
        } else {
            stub.disableRM(serviceName);
            msg = resourceBundle.getString("successfully.disable.configuration");
        }

        CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.INFO, request);
    } catch (NumberFormatException e) {
        String msg = resourceBundle.getString("numbers.format.error");
        CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.WARNING, request);
    } catch (RemoteException e) {
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
      location.href = '<%=backURL%>';
</script>
</body>
</html>
