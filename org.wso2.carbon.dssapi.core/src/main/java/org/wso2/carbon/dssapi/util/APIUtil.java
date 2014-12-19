/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.dssapi.util;

import org.apache.axis2.Constants;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.dataservices.ui.beans.*;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class APIUtil {
    private static final String HTTP_PORT = "mgt.transport.http.port";
    private static final String HOST_NAME = "carbon.local.ip";

    /**
     * To get the API provider
     *
     * @param username username of the logged user
     * @return
     */
    private APIProvider getAPIProvider(String username) {
        try {

            return APIManagerFactory.getInstance().getAPIProvider(username);
        } catch (APIManagementException e) {
            e.printStackTrace();
        }
        return null;
    }
   /**
     * To add an API
     *
     * @param serviceId  service name of the service
     * @param username   username of the logged user
     * @param tenantName tenant of the logged user
     */
    public void addApi(String serviceId, String username, String tenantName,Data data,String version) {
                  APIProvider apiProvider;
           if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantName)) {
                apiProvider = getAPIProvider(username+"@"+MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            } else {
                 apiProvider = getAPIProvider(username + "@" + tenantName);
            }
        API api=createApiObject(serviceId,username,tenantName,data,version,apiProvider);
        if (api != null) {
                try {
                  apiProvider.addAPI(api);
                     } catch (APIManagementException e) {
                    e.printStackTrace();
                }
            }
    }

    /**
     * To create the model of the API
     *
     * @param apiProvider API Provider
     * @param apiContext  API Context
     * @param apiEndpoint Endpoint url
     * @param authType    Authentication type
     * @param identifier  API identifier
     * @return API model
     */
    private API createAPIModel(APIProvider apiProvider, String apiContext, String apiEndpoint, String authType, APIIdentifier identifier,Data data) {
        API api = null;
        try {
            api = new API(identifier);
            api.setContext(apiContext);
            api.setUriTemplates(getURITemplates(apiEndpoint, authType,data));
            api.setVisibility(APIConstants.API_GLOBAL_VISIBILITY);
            api.addAvailableTiers(apiProvider.getTiers());
            api.setEndpointSecured(false);
            api.setStatus(APIStatus.PUBLISHED);
            api.setTransports(Constants.TRANSPORT_HTTP + "," + Constants.TRANSPORT_HTTPS);
            api.setSubscriptionAvailability(APIConstants.SUBSCRIPTION_TO_ALL_TENANTS);
            api.setResponseCache(APIConstants.DISABLED);
            api.setImplementation("endpoint");
            String endpointConfig = "{\"production_endpoints\":{\"url\":\"" + apiEndpoint + "\",\"config\":null},\"endpoint_type\":\"http\"}";
            api.setEndpointConfig(endpointConfig);
             } catch (APIManagementException e) {
            e.printStackTrace();
        }
        return api;
    }

    /**
     * To get URI templates
     *
     * @param endpoint Endpoint URL
     * @param authType Authentication type
     * @return URI templates
     */
    private Set<URITemplate> getURITemplates(String endpoint, String authType,Data data) {
        //todo improve to add sub context paths for uri templates as well
        Set<URITemplate> uriTemplates = new LinkedHashSet<URITemplate>();
        ArrayList<Operation> operations=data.getOperations();
        ArrayList<Resource> resourceList=data.getResources();

           if (authType.equals(APIConstants.AUTH_NO_AUTHENTICATION)) {
               for (Resource resource:resourceList) {
                URITemplate template = new URITemplate();
                template.setAuthType(APIConstants.AUTH_NO_AUTHENTICATION);
                template.setHTTPVerb(resource.getMethod());
                template.setResourceURI(endpoint);
                template.setUriTemplate("/" + resource.getPath());
                uriTemplates.add(template);
            }
               for (Operation operation:operations) {
                   URITemplate template = new URITemplate();
                   template.setAuthType(APIConstants.AUTH_NO_AUTHENTICATION);
                   template.setHTTPVerb("POST");
                   template.setResourceURI(endpoint);
                   template.setUriTemplate("/" + operation.getName());
                   uriTemplates.add(template);
               }
        } else {
               for (Operation operation:operations) {
                   URITemplate template = new URITemplate();
                   template.setAuthType(APIConstants.AUTH_APPLICATION_OR_USER_LEVEL_TOKEN);
                   template.setHTTPVerb("POST");
                   template.setResourceURI(endpoint);
                   template.setUriTemplate("/"+operation.getName());
                   uriTemplates.add(template);
               }
            for (Resource resource:resourceList) {
                URITemplate template = new URITemplate();
                if (!"OPTIONS".equals(resource.getMethod())) {
                    template.setAuthType(APIConstants.AUTH_APPLICATION_OR_USER_LEVEL_TOKEN);
                } else {
                    template.setAuthType(APIConstants.AUTH_NO_AUTHENTICATION);
                }
                template.setHTTPVerb(resource.getMethod());
                template.setResourceURI(endpoint);
                template.setUriTemplate("/"+resource.getPath());
                uriTemplates.add(template);
            }
        }
        return uriTemplates;
    }

    /**
     * To make sure that the API is available for given service and to the given user of a given tenant
     *
     * @param serviceId    service name of the service
     * @param username     username of the logged user
     * @param tenantDomain tenant domain
     * @return availability of the API
     */
    public boolean apiAvailable(String serviceId, String username, String tenantDomain,String version) {
        boolean apiAvailable = false;

            APIProvider apiProvider;
            String provider;
          if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                provider = username;
                apiProvider = getAPIProvider(username);
          } else {
                provider = username + "-AT-" + tenantDomain;
                apiProvider = getAPIProvider(username + "@" + tenantDomain);
           }
            String apiVersion =version;
            String apiName = serviceId;

            APIIdentifier identifier = new APIIdentifier(provider, apiName, apiVersion);
            try {
                apiAvailable = apiProvider.checkIfAPIExists(identifier);
            } catch (APIManagementException e) {
                e.printStackTrace();
            }
        return apiAvailable;
    }
    /**
     * To make sure that the API having active subscriptions for given service
     * @param serviceId    service name of the service
     * @param username     username of the logged user
     * @param tenantDomain tenant domain
     * @return availability of the API
     */
    public long apiSubscriptions(String serviceId, String username, String tenantDomain,String version) {
        long subscriptionCount = 0;
            APIProvider apiProvider;
            String provider;
           if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                provider = username;
                apiProvider = getAPIProvider(username);
           } else {
               provider = username + "-AT-" + tenantDomain;
                apiProvider = getAPIProvider(username + "@" + tenantDomain);
            }
            String apiVersion = version;
            String apiName = serviceId;

            APIIdentifier identifier = new APIIdentifier(provider, apiName, apiVersion);
            try {
                subscriptionCount = apiProvider.getAPISubscriptionCountByAPI(identifier);
            } catch (APIManagementException e) {
                e.printStackTrace();
            }
        return subscriptionCount;
    }

    /**
     * To remove API availability form a given user in the given tenant domain
     *
     * @param serviceId    service name of the service
     * @param username     username of the logged user
     * @param tenantDomain tenant domain
     */
    public void removeApi(String serviceId, String username, String tenantDomain,String version) {
            APIProvider apiProvider;
            String provider;
            if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
               provider = username;
                apiProvider = getAPIProvider(username);
            } else {
               provider = username + "-AT-" + tenantDomain;
                apiProvider = getAPIProvider(username + "@" + tenantDomain);
            }
            String apiVersion = version;

            String apiName = serviceId;

            APIIdentifier identifier = new APIIdentifier(provider, apiName, apiVersion);
            try {
                if (apiProvider.checkIfAPIExists(identifier)) {
                    apiProvider.deleteAPI(identifier);
                }
            } catch (APIManagementException e) {
                e.printStackTrace();
            }
        }
    public List<API> getApi(String serviceId, String username, String tenantDomain){
        List<API> apiList = null;
                   APIProvider apiProvider;
            if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                apiProvider = getAPIProvider(username);
            } else {
                apiProvider = getAPIProvider(username + "@" + tenantDomain);
            }
        try{
            apiList= apiProvider.searchAPIs(serviceId, "", username);
        } catch (APIManagementException e) {
            e.printStackTrace();
        }
        return apiList;
    }
    /**
     * To update an API
     *
     * @param serviceId  service name of the service
     * @param username   username of the logged user
     * @param tenantName tenant of the logged user
     */
    public void updateApi(String serviceId, String username, String tenantName,Data data,String version) {
        APIProvider apiProvider;
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantName)) {
            apiProvider = getAPIProvider(username+"@"+MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        } else {
            apiProvider = getAPIProvider(username + "@" + tenantName);
        }
        API api=createApiObject(serviceId,username,tenantName,data,version,apiProvider);
        if (api != null) {
            try {
                apiProvider.updateAPI(api);
            } catch (APIManagementException e) {
                e.printStackTrace();
            }
        }
    }
    private API createApiObject(String ServiceId, String username, String tenantName,Data data,String version,APIProvider apiProvider){
        String providerName;
        String apiEndpoint;
        String apiContext;

        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantName)) {
            providerName = username;
            apiEndpoint = "http://" + System.getProperty(HOST_NAME) + ":" + System.getProperty(HTTP_PORT) + "/services/" + ServiceId+"-"+version ;
            apiContext = "/api/" + ServiceId;
         } else {
            providerName = username + "-AT-" + tenantName;
            apiEndpoint = "http://" + System.getProperty(HOST_NAME) + ":" + System.getProperty(HTTP_PORT) + "/services/t/" + tenantName + "/" + ServiceId+"-"+version;
            apiContext = "/api/t/" + tenantName + "/" + ServiceId;
        }

        String provider = providerName; //todo get correct provider(username) for tenants
        String apiVersion;
            apiVersion=version;
        String apiName = ServiceId;
        String authType = "Any";
        APIIdentifier identifier = new APIIdentifier(provider, apiName, apiVersion);
        return createAPIModel(apiProvider, apiContext, apiEndpoint, authType, identifier,data);
    }
}

