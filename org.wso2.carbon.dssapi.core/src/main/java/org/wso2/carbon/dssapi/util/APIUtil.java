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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.dataservices.core.DBUtils;
import org.wso2.carbon.dataservices.ui.beans.Data;
import org.wso2.carbon.dataservices.ui.beans.Operation;
import org.wso2.carbon.dataservices.ui.beans.Resource;
import org.wso2.carbon.dssapi.model.LifeCycleEventDao;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class APIUtil {
    private static final String HTTP_PORT = "mgt.transport.http.port";
    private static final String HOST_NAME = "carbon.local.ip";
    private static final String APPLICATION_XML="_application.xml";
    private static final Log log = LogFactory.getLog(APIUtil.class);
    /**
     * To get the API provider
     *
     * @param username username of the logged user
     * @return
     */
    private APIProvider getAPIProvider(String username) {
        try {
            if (log.isDebugEnabled()){
                log.debug("Create APIProvider from username:"+username);
            }
            return APIManagerFactory.getInstance().getAPIProvider(username);
        } catch (APIManagementException e) {
           log.error("Failed to Create APIProvider for"+username,e);
        }
        return null;
    }

    /**
     * To add an API
     *
     * @param serviceId  service name of the service
     * @param username   username of the logged user
     * @param tenantName tenant of the logged user
     * @param data       data service object
     * @param version    version of the api
     */
    public void addApi(String serviceId, String username, String tenantName, Data data, String version) {
        APIProvider apiProvider;
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantName)) {
            apiProvider = getAPIProvider(username);
        } else {
            apiProvider = getAPIProvider(username + "@" + tenantName);
        }
        API api = createApiObject(serviceId, username, tenantName, data, version, apiProvider);

        if (api != null) {
            try {
                apiProvider.addAPI(api);
                String DSSRepositoryPath;
                int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

                if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
                    DSSRepositoryPath = CarbonUtils.getCarbonRepository() + "/dataservices";
                } else {
                    DSSRepositoryPath = CarbonUtils.getCarbonTenantsDirPath() + "/" + tenantId + "/dataservices";
                }
                try {
                    String applicationXmlPath = DSSRepositoryPath + "/" + serviceId +APPLICATION_XML;
                    File file = new File(applicationXmlPath);
                    if (!file.exists()) {
                        XMLStreamWriter xmlStreamWriter = DBUtils.getXMLOutputFactory().createXMLStreamWriter(new FileOutputStream(file));
                        xmlStreamWriter.writeStartDocument();
                        xmlStreamWriter.writeStartElement("api");
                        xmlStreamWriter.writeStartElement("managedApi");
                        xmlStreamWriter.writeCharacters("true");
                        xmlStreamWriter.writeEndElement();
                        xmlStreamWriter.writeStartElement("version");
                        xmlStreamWriter.writeCharacters(version);
                        xmlStreamWriter.writeEndElement();
                        xmlStreamWriter.writeEndElement();
                        xmlStreamWriter.writeEndDocument();
                        xmlStreamWriter.flush();
                        xmlStreamWriter.close();
                        if(log.isDebugEnabled()){
                            log.debug("API created successfully for "+serviceId+" Service");
                        }
                    }
                } catch (FileNotFoundException e) {
                    log.error("couldn't found path :"+DSSRepositoryPath+" application xml file for " + serviceId + "Service", e);
                } catch (XMLStreamException e) {
                    log.error("couldn't write application xml file for " + serviceId + "Service", e);
                }

            } catch (APIManagementException e) {
                log.error("couldn't Create API for "+serviceId+"Service",e);
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
     * @param data        data service object
     * @return API model
     */
    private API createAPIModel(APIProvider apiProvider, String apiContext, String apiEndpoint, String authType, APIIdentifier identifier, Data data) {
        API api = null;
        try {
            api = new API(identifier);
            api.setContext(apiContext);
            api.setUriTemplates(getURITemplates(apiEndpoint, authType, data));
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
            if(log.isDebugEnabled()){
                log.debug("API Object Created for API:"+identifier.getApiName()+"version:"+identifier.getVersion());
            }
        } catch (APIManagementException e) {
            log.error("couldn't get tiers for provider:"+identifier.getProviderName(),e);
        }
        return api;
    }

    /**
     * To get URI templates
     *
     * @param endpoint Endpoint URL
     * @param authType Authentication type
     * @param data     data service object
     * @return URI templates
     */
    private Set<URITemplate> getURITemplates(String endpoint, String authType, Data data) {
        //todo improve to add sub context paths for uri templates as well
        Set<URITemplate> uriTemplates = new LinkedHashSet<URITemplate>();
        ArrayList<Operation> operations = data.getOperations();
        ArrayList<Resource> resourceList = data.getResources();

        if (authType.equals(APIConstants.AUTH_NO_AUTHENTICATION)) {
            for (Resource resource : resourceList) {
                URITemplate template = new URITemplate();
                template.setAuthType(APIConstants.AUTH_NO_AUTHENTICATION);
                template.setHTTPVerb(resource.getMethod());
                template.setResourceURI(endpoint);
                template.setUriTemplate("/" + resource.getPath());
                uriTemplates.add(template);
            }
            for (Operation operation : operations) {
                URITemplate template = new URITemplate();
                template.setAuthType(APIConstants.AUTH_NO_AUTHENTICATION);
                template.setHTTPVerb("POST");
                template.setResourceURI(endpoint);
                template.setUriTemplate("/" + operation.getName());
                uriTemplates.add(template);
            }
        } else {
            for (Operation operation : operations) {
                URITemplate template = new URITemplate();
                template.setAuthType(APIConstants.AUTH_APPLICATION_OR_USER_LEVEL_TOKEN);
                template.setHTTPVerb("POST");
                template.setResourceURI(endpoint);
                template.setUriTemplate("/" + operation.getName());
                uriTemplates.add(template);
            }
            for (Resource resource : resourceList) {
                URITemplate template = new URITemplate();
                if (!"OPTIONS".equals(resource.getMethod())) {
                    template.setAuthType(APIConstants.AUTH_APPLICATION_OR_USER_LEVEL_TOKEN);
                } else {
                    template.setAuthType(APIConstants.AUTH_NO_AUTHENTICATION);
                }
                template.setHTTPVerb(resource.getMethod());
                template.setResourceURI(endpoint);
                template.setUriTemplate("/" + resource.getPath());
                uriTemplates.add(template);
            }
        }
        return uriTemplates;
    }

    /**
     * To make sure that the API is available for given service and to the given user of a given tenant
     *
     * @param serviceId service name of the service
     * @param tenantId  tenant domain
     * @return availability of the API
     */
    public boolean apiAvailable(String serviceId,int tenantId) {
        boolean apiAvailable = false;
        String DSSRepositoryPath;
        if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
            DSSRepositoryPath = CarbonUtils.getCarbonRepository() + "/dataservices";
        } else {
            DSSRepositoryPath = CarbonUtils.getCarbonTenantsDirPath() + "/" + tenantId + "/dataservices";
        }
        try {
            String applicationXmlPath = DSSRepositoryPath + "/" + serviceId + APPLICATION_XML;
            File file = new File(applicationXmlPath);
            if (file.exists()) {
                XMLStreamReader parser = DBUtils.getXMLInputFactory().createXMLStreamReader(
                        new FileInputStream(file));
                StAXOMBuilder builder = new StAXOMBuilder(parser);
                OMElement documentElement = builder.getDocumentElement();
                if (documentElement.getLocalName().equals("managedApi") && "true".equals(documentElement.getText())) {
                    apiAvailable = true;
                }

            }
        } catch (FileNotFoundException e) {
            log.error("application.xml file couldn't be found on path:"+DSSRepositoryPath,e);
        } catch (XMLStreamException e) {
            log.error("couldn't read application xml file",e);
        }
        return apiAvailable;
    }

    /**
     * To make sure that the API having active subscriptions for given service
     *
     * @param serviceId    service name of the service
     * @param username     username of the logged user
     * @param tenantDomain tenant domain
     * @return availability of the API
     */
    public long apiSubscriptions(String serviceId, String username, String tenantDomain, String version) {
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
           log.error("error getting subscription count for API:"+apiName+"for version:"+version,e);
        }
        return subscriptionCount;
    }

    /**
     * To remove API availability form a given user in the given tenant domain
     *
     * @param serviceId    service name of the service
     * @param username     username of the logged user
     * @param tenantDomain tenant domain
     * @param version      version of the api
     */
    public boolean removeApi(String serviceId, String username, String tenantDomain, String version) {
        boolean status = false;
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
            String DSSRepositoryPath;
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

            if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
                DSSRepositoryPath = CarbonUtils.getCarbonRepository() + "/dataservices";
            } else {
                DSSRepositoryPath = CarbonUtils.getCarbonTenantsDirPath() + "/" + tenantId + "/dataservices";
            }
            String applicationXmlPath = DSSRepositoryPath + "/" + serviceId + "_application.xml";
            File file = new File(applicationXmlPath);
            if (file.exists()) {
                if (file.delete()) {
                    status = true;
                }
            }
        } catch (APIManagementException e) {
           log.error("couldn't remove API"+apiName+"version:"+version,e);
        }
        return status;
    }

    /**
     * To get the list of APIs
     *
     * @param serviceId    service name of the service
     * @param username     username of the logged user
     * @param tenantDomain tenant domain
     * @return api list according to the given parameters
     */
    public List<API> getApi(String serviceId, String username, String tenantDomain) {
        List<API> apiList = null;
        APIProvider apiProvider;
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
            apiProvider = getAPIProvider(username);
        } else {
            apiProvider = getAPIProvider(username + "@" + tenantDomain);
        }
        try {
            apiList = apiProvider.searchAPIs(serviceId, "default", null);
        } catch (APIManagementException e) {
            log.error("couldn't find api for Service:"+serviceId,e);
        }
        return apiList;
    }

    /**
     * To update an API
     *
     * @param serviceId  service name of the service
     * @param username   username of the logged user
     * @param tenantName tenant of the logged user
     * @param version    version of the api
     */
    public void updateApi(String serviceId, String username, String tenantName, Data data, String version) {
        APIProvider apiProvider;
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantName)) {
            apiProvider = getAPIProvider(username + "@");
        } else {
            apiProvider = getAPIProvider(username + "@" + tenantName);
        }
        API api = createApiObject(serviceId, username, tenantName, data, version, apiProvider);
        if (api != null) {
            try {
                apiProvider.updateAPI(api);
            } catch (APIManagementException e) {
               log.error("error while updating api:"+serviceId+"for version:"+version,e);
            }
        }
    }

    /**
     * To update an API
     *
     * @param serviceId  service name of the service
     * @param username   username of the logged user
     * @param tenantName tenant of the logged user
     * @param version    version of the api
     */
    public LifeCycleEventDao[] getLifeCycleEventList(String serviceId, String username, String tenantName, String version) {
        APIProvider apiProvider;
        List<LifeCycleEvent> lifeCycleEventList;
        List<LifeCycleEventDao> lifeCycleEventDaoList = new ArrayList<LifeCycleEventDao>();
        String provider;
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantName)) {
            apiProvider = getAPIProvider(username);
            provider = username;
        } else {
            provider = username + "-AT-" + tenantName;
            apiProvider = getAPIProvider(username + "@" + tenantName);
        }
        APIIdentifier apiIdentifier = new APIIdentifier(provider, serviceId, version);
        if (apiIdentifier != null) {
            try {
                lifeCycleEventList = apiProvider.getLifeCycleEvents(apiIdentifier);
                if (lifeCycleEventList.size() > 0) {

                    for (LifeCycleEvent lifeCycleEvent : lifeCycleEventList) {
                        LifeCycleEventDao lifeCycleEventDao;

                        if ((lifeCycleEvent.getOldStatus() != null)) {
                            lifeCycleEventDao = new LifeCycleEventDao(apiIdentifier, lifeCycleEvent.getOldStatus().name(), lifeCycleEvent.getNewStatus().name(), lifeCycleEvent.getUserId(), lifeCycleEvent.getDate());
                        } else {
                            lifeCycleEventDao = new LifeCycleEventDao(apiIdentifier, "", lifeCycleEvent.getNewStatus().name(), lifeCycleEvent.getUserId(), lifeCycleEvent.getDate());
                        }
                        lifeCycleEventDaoList.add(lifeCycleEventDao);
                    }
                }
            } catch (APIManagementException e) {
                log.error("error while getting lifecycle history api:"+serviceId+"for version:"+version,e);
            }
        }
        return lifeCycleEventDaoList.toArray(new LifeCycleEventDao[lifeCycleEventDaoList.size()]);
    }

    /**
     * To create API model
     * @param serviceId service name of the service
     * @param username username of the logged user
     * @param tenantName tenant of the logged user
     * @param data data service object
     * @param version version of the api
     * @param apiProvider API Provider
     * @return created api model
     */
    private API createApiObject(String serviceId, String username, String tenantName, Data data, String version, APIProvider apiProvider) {
        String providerName;
        String apiEndpoint;
        String apiContext;

        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantName)) {
            providerName = username;
            apiEndpoint = "http://" + System.getProperty(HOST_NAME) + ":" + System.getProperty(HTTP_PORT) + "/services/" + serviceId;
            apiContext = "/api/" + serviceId;
        } else {
            providerName = username + "-AT-" + tenantName;
            apiEndpoint = "http://" + System.getProperty(HOST_NAME) + ":" + System.getProperty(HTTP_PORT) + "/services/t/" + tenantName + "/" + serviceId;
            apiContext = "/api/t/" + tenantName + "/" + serviceId;
        }

        String provider = providerName;
        String apiVersion;
        apiVersion = version;
        String apiName = serviceId;
        String authType = "Any";
        APIIdentifier identifier = new APIIdentifier(provider, apiName, apiVersion);
        return createAPIModel(apiProvider, apiContext, apiEndpoint, authType, identifier, data);
    }
}

