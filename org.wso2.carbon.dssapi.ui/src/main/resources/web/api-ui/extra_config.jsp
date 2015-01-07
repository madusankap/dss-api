<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.dssapi.model.xsd.LifeCycleEventDao" %>
<%@ page import="org.wso2.carbon.dssapi.ui.APIPublisherClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.apimgt.api.model.LifeCycleEvent" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<!--
~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
    String serviceName = request.getParameter("serviceName");
    String apiVersion;
    boolean APIAvailability = false;
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    APIPublisherClient client = null;
    try {
        client = new APIPublisherClient(cookie, backendServerURL, configContext);
        APIAvailability = client.isAPIAvailable(serviceName);
    } catch (Exception e) {
        response.setStatus(500);
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
    }
%>
<fmt:bundle basename="org.wso2.carbon.dssapi.ui.i18n.Resources">
    <table class="styledLeft" id="apiOperationsTable" style="margin-left: 0px; margin-top :15px" width="100%">
        <thead>
        <tr>
            <th colspan="2" align="left"><fmt:message key="api.management"/></th>
        </tr>
        </thead>
        <%
            if (APIAvailability) {
                apiVersion = client.getCurrentApiVersion(serviceName);
        %>
        <tr class="tableOddRow">
            <td><fmt:message key="published.date"/> :
                <%=client.getPublishedDate(serviceName, apiVersion)%>
            </td>
            <td><fmt:message key="update.date"/> :
                <%=client.getUpdatedDate(serviceName, apiVersion)%>
            </td>
        </tr>
        <tr class="tableEvenRow">
            <td colspan="2"><input type="button" value="Update API" onclick="updateAPI()"/> <input type="button"
                                                                                        value="Unpublish API"
                                                                                        onclick="unpublishAPI()"/></td>
        </tr>
        <tr class="tableOddRow">
            <td colspan="2" style="padding-top: 5px;padding-bottom: 5px">
                <strong><fmt:message key="publish.history"/></strong>
                <%
                    LifeCycleEventDao[] cycleEventDaos = client.getLifeCycleEvents(serviceName, apiVersion);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a");
                    for(LifeCycleEventDao lifeCycleEventDao : cycleEventDaos) {
                        Date date = lifeCycleEventDao.getDate();
                        String user = lifeCycleEventDao.getUserId();
                %>
                <div style="padding-top: 5px; margin-left: -3px">
                    <span style="background-image: url('../api-ui/images/info.png'); background-repeat: no-repeat;background-size: 18px 16px;padding-left: 18px"><%=dateFormat.format(date)%>,<%=timeFormat.format(date)%></span>
                    <span style="background-image: url('../api-ui/images/user.png');background-repeat: no-repeat;background-size: 16px 16px;padding-left: 18px"><a href="../..//publisher/user?uname=<%=user%>"><%=user%></a></span>
                    <%
                        if (lifeCycleEventDao.getOldStatus() != "") {
                    %>
                    <span>State changed from <strong><%=lifeCycleEventDao.getOldStatus().toLowerCase()%></strong> to </span>
                    <%
                        }
                    %>
                    <span><strong><%=lifeCycleEventDao.getNewStatus().toLowerCase()%></strong></span>
                </div>
                <%
                    }
                %>
                <br/>
            </td>
        </tr>
        <tr class="tableEvenRow">

        </tr>

        <%
        } else {
        %>
        <tr class="tableOddRow">
            <td>API Name : <input type="text" value="<%=serviceName%>" disabled style="width:80%" name="apiName"/></td>
            <td>Version : <input type="text" style="width:80%" name="apiVersion" id="apiVersion"/></td>
        </tr>
        <tr class="tableEvenRow">
            <td colspan="2"><input type="button" value="Publish as an API" onclick="publishAPI()"></td>
        </tr>
        <%
            }
        %>
    </table>

    <script type="text/javascript">
        jQuery.noConflict();
        function changeState(active) {
            try {
                var url = '../api-ui/change_state.jsp?serviceName=<%=serviceName%>&isPublishRequest=' + active;
                jQuery.ajax({
                    url: url,
                    async: false,
                    type: "GET",
                    cache: false,
                    success: function () {
                        <%
                            //String successMessage = "API change request sent and currently processing. Please reload the page after a few seconds.";
                            //CarbonUIMessage.sendCarbonUIMessage(successMessage,CarbonUIMessage.INFO,request,response,"https://10.100.5.179:9443/carbon/service-mgt/service_info.jsp?serviceName="+serviceName);
                         %>
                        //var successMessage = "API change request sent and currently processing. Please reload the page after a few seconds.";
                        //alert(successMessage);
                        location.reload(true);
                    }
                });
            } catch (exception) {
                alert(exception);
            }
        }
    </script>

    <script type="text/javascript">
        jQuery.noConflict();
        function publishAPI() {
           // alert('Publish API');
            try {
                var version = document.getElementById("apiVersion").value;
                //alert(version);
                var url = '../api-ui/api_publish.jsp?serviceName=<%=serviceName%>&version=' + version;
                jQuery.ajax({
                    url: url,
                    async: false,
                    type: "GET",
                    cache: false,
                    success: function () {
                        <%
                            //String successMessage = "API change request sent and currently processing. Please reload the page after a few seconds.";
                            //CarbonUIMessage.sendCarbonUIMessage(successMessage,CarbonUIMessage.INFO,request,response,"https://10.100.5.179:9443/carbon/service-mgt/service_info.jsp?serviceName="+serviceName);
                         %>
                        //var successMessage = "API change request sent and currently being processing. Please reload the page after a few seconds.";
                        //alert(successMessage);
                        location.reload(true);
                    }
                });
            } catch (exception) {
                alert(exception);
            }
        }
    </script>

    <script type="text/javascript">
        jQuery.noConflict();
        function unpublishAPI() {
            //alert('Unpublish API');
            try {
                var url = '../api-ui/api_unpublish.jsp?serviceName=<%=serviceName%>';
                jQuery.ajax({
                    url: url,
                    async: false,
                    type: "GET",
                    cache: false,
                    success: function () {
                        <%
                            //String successMessage = "API change request sent and currently processing. Please reload the page after a few seconds.";
                            //CarbonUIMessage.sendCarbonUIMessage(successMessage,CarbonUIMessage.INFO,request,response,"https://10.100.5.179:9443/carbon/service-mgt/service_info.jsp?serviceName="+serviceName);
                         %>
                        //var successMessage = "API change request sent and currently being processing. Please reload the page after a few seconds.";
                        //alert(successMessage);
                        location.reload(true);
                    }
                });
            } catch (exception) {
                alert(exception);
            }
        }
    </script>

    <script type="text/javascript">
        jQuery.noConflict();
        function updateAPI() {
           // alert('Update API');
            try {
                var url = '../api-ui/api_update.jsp?serviceName=<%=serviceName%>';
                jQuery.ajax({
                    url: url,
                    async: false,
                    type: "GET",
                    cache: false,
                    success: function () {
                        <%
                            //String successMessage = "API change request sent and currently processing. Please reload the page after a few seconds.";
                            //CarbonUIMessage.sendCarbonUIMessage(successMessage,CarbonUIMessage.INFO,request,response,"https://10.100.5.179:9443/carbon/service-mgt/service_info.jsp?serviceName="+serviceName);
                         %>
                       // var successMessage = "API change request sent and currently being processing. Please reload the page after a few seconds.";
                        //alert(successMessage);
                        location.reload(true);
                    }
                });
            } catch (exception) {
                alert(exception);
            }
        }
    </script>
</fmt:bundle>