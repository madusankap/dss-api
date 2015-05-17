/*
 *  Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.dssapi.org.wso2.carbon.dssapi.valve;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.core.APIManagerErrorConstants;
import org.wso2.carbon.apimgt.core.authenticate.APITokenValidator;
import org.wso2.carbon.apimgt.core.gateway.APITokenAuthenticator;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.interceptor.APIManagerInterceptorOps;
import org.wso2.carbon.apimgt.interceptor.UsageStatConfiguration;
import org.wso2.carbon.apimgt.interceptor.utils.APIManagetInterceptorUtils;
import org.wso2.carbon.apimgt.interceptor.valve.APIFaultException;
import org.wso2.carbon.apimgt.interceptor.valve.APIManagerInterceptorValve;
import org.wso2.carbon.apimgt.interceptor.valve.internal.DataHolder;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.dataservices.common.DBConstants;
import org.wso2.carbon.dataservices.core.DataServiceFault;
import org.wso2.carbon.dataservices.core.description.resource.Resource;
import org.wso2.carbon.dataservices.core.engine.DataService;
import org.wso2.carbon.dataservices.core.engine.ParamValue;
import org.wso2.carbon.dataservices.core.tools.DSTools;
import org.wso2.carbon.tomcat.ext.valves.CompositeValve;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DSSAPIValve extends APIManagerInterceptorValve {

    private static final Log log = LogFactory.getLog(DSSAPIValve.class);

    APITokenAuthenticator authenticator;

    public DSSAPIValve() {
        authenticator = new APITokenAuthenticator();

    }

    public void invoke(Request request, Response response, CompositeValve compositeValve) {
        String context1 = request.getCoyoteRequest().toString().replace("R( ", "");
        context1 = context1.substring(0, context1.lastIndexOf(")"));
        String context = null;
        String tenantDomain = MultitenantUtils.getTenantDomain(request);
        AxisConfiguration configurationContext = null;
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equalsIgnoreCase(tenantDomain)) {
            configurationContext =
                    TenantAxisUtils.getTenantAxisConfiguration(tenantDomain, DataHolder.getServerConfigContext());
        } else {
            configurationContext = DataHolder.getServerConfigContext().getAxisConfiguration();
        }
        if (context1.contains("api")) {
            context = context1.split("/[0-9][.][0-9][.][0-9]")[0];
        }

        if (context == null || context.equals("")) {
            //Invoke next valve in pipe.
            getNext().invoke(request, response, compositeValve);
            return;
        }
        boolean contextExist;
        Boolean contextValueInCache = null;
        if (APIUtil.getAPIContextCache().get(context) != null) {
            contextValueInCache = Boolean.parseBoolean(APIUtil.getAPIContextCache().get(context).toString());
        }
        if (contextValueInCache != null) {
            contextExist = contextValueInCache;
        } else {
            contextExist = ApiMgtDAO.isContextExist(context);
            APIUtil.getAPIContextCache().put(context, contextExist);
        }
        if (!contextExist) {
            getNext().invoke(request, response, compositeValve);
            return;
        }

        handleWSDLGetRequest(request, response, compositeValve, context);

        long requestTime = System.currentTimeMillis();
        APIManagerInterceptorOps interceptorOps = new APIManagerInterceptorOps();
        UsageStatConfiguration statConfiguration = new UsageStatConfiguration();
        if (contextExist) {
            //Use embedded API Management
            if (log.isDebugEnabled()) {
                log.debug("API Manager Interceptor Valve Got invoked!!");
            }

            String bearerToken = request.getHeader(APIConstants.OperationParameter.AUTH_PARAM_NAME);
            String accessToken = null;

            /* Authenticate*/
            try {
                if (bearerToken != null) {
                    accessToken = APIManagetInterceptorUtils.getBearerToken(bearerToken);
                }

                String apiVersion = null;
                Matcher matcher = Pattern.compile("[0-9][.][0-9][.][0-9]").matcher(context1);
                while (matcher.find()) {
                    apiVersion = matcher.group();
                }
                String domain = request.getHeader(APITokenValidator.getAPIManagerClientDomainHeader());
                String authLevel = authenticator.getResourceAuthenticationScheme(context,
                        apiVersion,
                        request.getRequestURI(),
                        request.getMethod());
                if (authLevel == APIConstants.NO_MATCHING_AUTH_SCHEME) {
                    APIManagetInterceptorUtils.handleNoMatchAuthSchemeCallForRestService(response,
                            request.getMethod(),
                            request.getRequestURI(),
                            apiVersion, context);
                    return;
                } else {
                    interceptorOps.doAuthenticate(context, apiVersion, accessToken, authLevel, domain);
                }
            } catch (APIManagementException e) {
                //ignore
            } catch (APIFaultException e) {/* If !isAuthorized APIFaultException is thrown*/
                APIManagetInterceptorUtils.handleAPIFaultForRestService(e, APIManagerErrorConstants.API_SECURITY_NS,
                        APIManagerErrorConstants
                                .API_SECURITY_NS_PREFIX,
                        response);
                return;
            }
            /* Throttle*/
            try {
                interceptorOps.doThrottle(request, accessToken);
            } catch (APIFaultException e) {
                APIManagetInterceptorUtils.handleAPIFaultForRestService(e,
                        APIManagerErrorConstants.API_THROTTLE_NS,
                        APIManagerErrorConstants
                                .API_THROTTLE_NS_PREFIX,
                        response);
                return;
            }
            /* Publish Statistic if enabled*/
            if (statConfiguration.isStatsPublishingEnabled()) {
                try {
                    interceptorOps.publishStatistics(request, requestTime, false);
                } catch (APIManagementException e) {
                    log.error("Error occured when publishing stats", e);
                }
            }
        }

        String serviceName = context.split("/api/")[1];
        String temp = context1.split("[0-9][.][0-9][.][0-9][/]")[1];
        String resourceName;
        String pathParam = null;
        if (temp.split("/").length > 1) {
            resourceName = temp.substring(0, temp.indexOf("/", 1));
            pathParam = temp.substring(temp.indexOf("/") + 1);
        } else {
            resourceName = temp;
        }

        try {
            AxisService axisService = configurationContext.getService(serviceName);
            DataService dataService =
                    (DataService) axisService.getParameter(DBConstants.DATA_SERVICE_OBJECT).getValue();
            Map<String, ParamValue> paramValueMap = new HashMap<String, ParamValue>();
            Enumeration<String> requestParameters = request.getParameterNames();
            boolean isOperation = true;
            boolean pathParamExist = false;
            if (!"PUT".equalsIgnoreCase(request.getMethod())) {
                while (requestParameters.hasMoreElements()) {
                    String requestKey = requestParameters.nextElement();
                    paramValueMap.put(requestKey, new ParamValue(request.getParameter(requestKey)));
                }
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
                String data = br.readLine();
                if (data != null) {
                    String[] formParameters = data.split("&");
                    for (String nameValue : formParameters) {
                        String[] nameValuePair = nameValue.split("=");
                        paramValueMap.put(nameValuePair[0], new ParamValue(nameValuePair[1]));
                    }
                }
            }

            Resource resource = null;
            if (pathParam != null) {
                pathParamExist = true;
                Iterator<Resource.ResourceID> resourceIDIterator = dataService.getResourceIds().iterator();
                while (resourceIDIterator.hasNext()) {
                    Resource.ResourceID resourceID = resourceIDIterator.next();
                    if (Pattern.matches(resourceName + "/[{]\\w+[}]", resourceID.getPath())) {
                        resource = dataService.getResource(resourceID);
                    }
                }
                Matcher paramNamePattern = Pattern.compile("[{]\\w+[}]").matcher(resource.getResourceId().getPath());
                paramNamePattern.find();
                String pathParamName = paramNamePattern.group().replace("{", "").replace("}", "");
                paramValueMap.put(pathParamName, new ParamValue(pathParam));
            } else {
                resource = dataService.getResource(new Resource.ResourceID(resourceName, request.getMethod()));

            }
            if (resource != null) {
                isOperation = false;
            }
            try {
                OMElement output;
                if (!isOperation) {
                    if (pathParamExist) {

                        output = DSTools.accessResource(dataService, resource.getResourceId().getPath(), paramValueMap,
                                request.getMethod());
                    } else {
                        output = DSTools.accessResource(dataService, resourceName, paramValueMap, request.getMethod());
                    }

                } else {
                    output = DSTools.invokeOperation(dataService, resourceName, paramValueMap);
                }
                if (output != null) {
                    output.serialize(response.getWriter());
                } else {
                    response.setStatus(HttpServletResponse.SC_ACCEPTED);
                    response.getWriter().write("");
                }
            } catch (IOException e) {
                log.error("couldn't create Writer object from response", e);
            }
        } catch (AxisFault axisFault) {
            log.error("couldn't create Configuration Context", axisFault);
        } catch (DataServiceFault dataServiceFault) {
            log.error("error occurred when accessing DataService", dataServiceFault);
        } catch (XMLStreamException e) {
            log.error("Couldn't Serialize Output", e);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Handle Responses
        if (contextExist && statConfiguration.isStatsPublishingEnabled()) {
            try {
                interceptorOps.publishStatistics(request, requestTime, true);
            } catch (APIManagementException e) {
                log.error("Error occured when publishing stats", e);
            }
        }

    }

    /**
     * When we do GET call for WSDL/WADL, we do not want to
     * authenticate/throttle the request.
     * <p/>
     * @param request
     * @param response
     * @param compositeValve
     * @param context
     */
    private void handleWSDLGetRequest(Request request, Response response,
                                      CompositeValve compositeValve, String context) {
        if (request.getMethod().equals(Constants.Configuration.HTTP_METHOD_GET)) {
            if (request.getRequestURI().matches(context + "/[^/]*/services")) {
                getNext().invoke(request, response, compositeValve);
                return;
            }
            Enumeration<String> params = request.getParameterNames();
            String paramName = null;
            while (params.hasMoreElements()) {
                paramName = params.nextElement();
                if (paramName.endsWith("wsdl") || paramName.endsWith("wadl")) {
                    getNext().invoke(request, response, compositeValve);
                    return;
                }
            }
        }
    }
}
