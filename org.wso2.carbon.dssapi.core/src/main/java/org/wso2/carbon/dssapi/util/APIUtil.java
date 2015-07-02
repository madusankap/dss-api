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

package org.wso2.carbon.dssapi.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.dataservices.core.admin.DataServiceAdmin;
import org.wso2.carbon.dataservices.ui.beans.*;
import org.wso2.carbon.dssapi.core.DSSAPIException;
import org.wso2.carbon.dssapi.model.Application;
import org.wso2.carbon.dssapi.model.LifeCycleEventDao;
import org.wso2.carbon.dssapi.observer.DataHolder;
import org.wso2.carbon.service.mgt.ServiceAdmin;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * APIUtil method has all the util methods to help dss api
 */
@SuppressWarnings("unchecked")
public class APIUtil {
    private static final String HTTP_PORT = "mgt.transport.http.port";
    private static final String HOST_NAME = "carbon.local.ip";
    public static final String APPLICATION_XML = "_application.xml";
    private static final Log log = LogFactory.getLog(APIUtil.class);

    /**
     * To get the API provider
     *
     * @param username username of the logged user
     * @return APIProvider object according to user
     */
    private static APIProvider getAPIProvider(String username) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Create APIProvider from username:" + username);
            }
            return APIManagerFactory.getInstance().getAPIProvider(username);
        } catch (APIManagementException e) {
            log.error("Failed to Create APIProvider for" + username, e);
        }
        return null;
    }

    /**
     * To add an API
     *
     * @param serviceId service name of the service
     * @param username  username of the logged user
     * @param data      data service object
     * @param version   version of the api
     */
    public static void addApi(String serviceId, String username, Data data, String version) {
        APIProvider apiProvider;
        apiProvider = getAPIProvider(username);
        Map<String, JSONArray> resourceMap = new LinkedHashMap<String, JSONArray>();
        API api = createApiObject(serviceId, username, data, version, apiProvider, resourceMap);
        JSONObject swagger12Json = new JSONObject();
        if (api != null) {

            try {
                apiProvider.addAPI(api);
                updateSwagger12Definition(api, apiProvider, swagger12Json, resourceMap);
                api.setStatus(APIStatus.CREATED);
                apiProvider.changeAPIStatus(api, APIStatus.PUBLISHED, username, false);
            } catch (APIManagementException e) {
                log.error("couldn't Create API for " + serviceId + "Service", e);
            }
            String dssRepositoryPath;
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

            if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
                dssRepositoryPath = CarbonUtils.getCarbonRepository() + "/dataservices";
            } else {
                dssRepositoryPath = CarbonUtils.getCarbonTenantsDirPath() + "/" + tenantId + "/dataservices";
            }
            try {
                String deployedTime = new ServiceAdmin(DataHolder.getConfigurationContext().getAxisConfiguration())
                        .getServiceData(serviceId).getServiceDeployedTime();
                String applicationXmlPath = dssRepositoryPath + "/" + serviceId + APPLICATION_XML;
                File file = new File(applicationXmlPath);
                if (!file.exists()) {
                    Application application = new Application(true, deployedTime, username, version,
                            MultitenantUtils.getTenantDomain(username));
                    JAXBContext jaxbContext = JAXBContext.newInstance(Application.class);
                    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                    jaxbMarshaller.marshal(application, file);
                    if (log.isDebugEnabled()) {
                        log.debug("API created successfully for " + serviceId);
                    }
                }
            } catch (FileNotFoundException e) {
                log.error("couldn't found path :" + dssRepositoryPath + " application xml file for " + serviceId +
                        "Service", e);
            } catch (XMLStreamException e) {
                log.error("couldn't write application xml file for " + serviceId + "Service", e);
            } catch (Exception e) {
                log.error("Couldn't get ServiceMetaData Object for the service " + serviceId, e);
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
     * @param resourceMap map of resources in swagger12 json
     * @return API model
     */
    private static API createAPIModel(APIProvider apiProvider, String apiContext, String apiEndpoint, String authType,
                                      APIIdentifier identifier, Data data, Map<String, JSONArray> resourceMap) {
        API api = null;
        try {
            api = new API(identifier);
            api.setContext(apiContext);
            api.setUriTemplates(getURITemplates(apiEndpoint, authType, data, apiProvider.getTiers(), resourceMap));
            api.setVisibility(APIConstants.API_GLOBAL_VISIBILITY);
            api.addAvailableTiers(apiProvider.getTiers());
            api.setEndpointSecured(false);
            api.setStatus(APIStatus.CREATED);
            api.setTransports(Constants.TRANSPORT_HTTP + "," + Constants.TRANSPORT_HTTPS);
            api.setSubscriptionAvailability(APIConstants.SUBSCRIPTION_TO_ALL_TENANTS);
            api.setResponseCache(APIConstants.DISABLED);
            api.setImplementation("endpoint");
            String endpointConfig = "{\"production_endpoints\":{\"url\":\"" + apiEndpoint +
                    "\",\"config\":null},\"endpoint_type\":\"http\"}";
            api.setEndpointConfig(endpointConfig);
            if (log.isDebugEnabled()) {
                log.debug(
                        "API Object Created for API:" + identifier.getApiName() + "version:" + identifier.getVersion());
            }
        } catch (APIManagementException e) {
            log.error("couldn't get tiers for provider:" + identifier.getProviderName(), e);
        }
        return api;
    }

    /**
     * To get URI templates
     *
     * @param endpoint Endpoint URL
     * @param authType Authentication type
     * @param data     data service object
     * @param tiers tiers according to publisher
     * @param resourceMap map of resources in swagger12 json
     * @return URI templates
     */
    private static Set<URITemplate> getURITemplates(String endpoint, String authType, Data data, Set<Tier> tiers,
                                                    Map<String, JSONArray> resourceMap) {
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
                for (Tier tier : tiers.toArray(new Tier[tiers.size()])) {
                    template.setThrottlingTier(tier.getName());
                }
                uriTemplates.add(template);
                addApiArray(template, resource.getCallQuery().getWithParams(), resourceMap);
            }
            for (Operation operation : operations) {
                URITemplate template = new URITemplate();
                template.setAuthType(APIConstants.AUTH_NO_AUTHENTICATION);
                template.setHTTPVerb(getOperationBasedHttpVerbs(operation.getCallQuery().getHref(), data));
                template.setResourceURI(endpoint);
                template.setUriTemplate("/" + operation.getName());
                for (Tier tier : tiers.toArray(new Tier[tiers.size()])) {
                    template.setThrottlingTier(tier.getName());
                }
                uriTemplates.add(template);
                addApiArray(template, operation.getCallQuery().getWithParams(), resourceMap);
            }
        } else {
            for (Operation operation : operations) {
                URITemplate template = new URITemplate();
                template.setAuthType(APIConstants.AUTH_APPLICATION_OR_USER_LEVEL_TOKEN);
                template.setHTTPVerb(getOperationBasedHttpVerbs(operation.getCallQuery().getHref(), data));
                template.setResourceURI(endpoint);
                template.setUriTemplate("/" + operation.getName());
                for (Tier tier : tiers.toArray(new Tier[tiers.size()])) {
                    template.setThrottlingTier(tier.getName());
                }
                uriTemplates.add(template);
                addApiArray(template, operation.getCallQuery().getWithParams(), resourceMap);
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
                for (Tier tier : tiers.toArray(new Tier[tiers.size()])) {
                    template.setThrottlingTier(tier.getName());
                }
                uriTemplates.add(template);
                addApiArray(template, resource.getCallQuery().getWithParams(), resourceMap);
            }
        }
        return uriTemplates;
    }

    /**
     * @param queryId QueryId of the Operation
     * @param data    data service object
     * @return type of http verb can used to operation
     */

    private static String getOperationBasedHttpVerbs(String queryId, Data data) {
        String httpVerb = "POST";
        if (queryId != null) {
            Query query = data.getQuery(queryId);
            if (query != null) {
                if (Pattern.compile(Pattern.quote("SELECT"), Pattern.CASE_INSENSITIVE).matcher(query.getSql()).find())
                    httpVerb = "GET";
                if (Pattern.compile(Pattern.quote("INSERT"), Pattern.CASE_INSENSITIVE).matcher(query.getSql()).find())
                    httpVerb = "PUT";
                if (Pattern.compile(Pattern.quote("DELETE"), Pattern.CASE_INSENSITIVE).matcher(query.getSql()).find())
                    httpVerb = "DELETE";
                if (Pattern.compile(Pattern.quote("UPDATE"), Pattern.CASE_INSENSITIVE).matcher(query.getSql()).find())
                    httpVerb = "POST";
            }
        }
        return httpVerb;
    }

    /**
     * To make sure that the API is available for given service and to the given user of a given tenant
     *
     * @param serviceId service name of the service
     * @param tenantId  tenant domain
     * @return availability of the API
     */
    public boolean checkApiAvailability(String serviceId, int tenantId) {
        boolean checkApiAvailability = false;
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
                JAXBContext jaxbContext = JAXBContext.newInstance(Application.class);
                Unmarshaller jaxbUnMarshaller = jaxbContext.createUnmarshaller();
                Application application = (Application) jaxbUnMarshaller.unmarshal(file);
                checkApiAvailability = application.getManagedApi();
            }
        } catch (JAXBException e) {
            log.error("Couldn't parse xml file", e);
        }
        return checkApiAvailability;
    }

    /**
     * To make sure that the API having active subscriptions for given service
     *
     * @param serviceId service name of the service
     * @param username  username of the logged user
     * @return availability of the API
     */
    public long getApiSubscriptions(String serviceId, String username, String version) {
        long subscriptionCount = 0;
        APIProvider apiProvider = getAPIProvider(username);
        String provider = org.wso2.carbon.apimgt.impl.utils.APIUtil.replaceEmailDomain(username);

        APIIdentifier identifier = new APIIdentifier(provider, serviceId, version);
        try {
            subscriptionCount = apiProvider.getAPISubscriptionCountByAPI(identifier);
        } catch (APIManagementException e) {
            log.error("error getting subscription count for API:" + serviceId + "for version:" + version, e);
        }
        return subscriptionCount;
    }

    /**
     * To remove API availability form a given user in the given tenant domain
     *
     * @param serviceId service name of the service
     * @param username  username of the logged user
     * @param version   version of the api
     */
    public static boolean removeApi(String serviceId, String username, String version) {
        boolean status = false;
        APIProvider apiProvider = getAPIProvider(username);
        String provider = org.wso2.carbon.apimgt.impl.utils.APIUtil.replaceEmailDomain(username);

        APIIdentifier identifier = new APIIdentifier(provider, serviceId, version);
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
            log.error("couldn't remove API" + serviceId + "version:" + version, e);
        }
        return status;
    }

    /**
     * To get the list of APIs
     *
     * @param serviceId service name of the service
     * @param username  username of the logged user
     * @return api list according to the given parameters
     */
    public List<API> getApi(String serviceId, String username) {
        String version = null;
        List<API> apiList = null;
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        APIProvider apiProvider;
        String DSSRepositoryPath;
        String provider = org.wso2.carbon.apimgt.impl.utils.APIUtil.replaceEmailDomain(username);
        apiProvider = getAPIProvider(provider);
        if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
            DSSRepositoryPath = CarbonUtils.getCarbonRepository() + "/dataservices";
        } else {
            DSSRepositoryPath = CarbonUtils.getCarbonTenantsDirPath() + "/" + tenantId + "/dataservices";
        }
        try {
            String applicationXmlPath = DSSRepositoryPath + "/" + serviceId + APPLICATION_XML;
            File file = new File(applicationXmlPath);
            if (file.exists()) {
                JAXBContext jaxbContext = JAXBContext.newInstance(Application.class);
                Unmarshaller jaxbUnMarshaller = jaxbContext.createUnmarshaller();
                Application application = (Application) jaxbUnMarshaller.unmarshal(file);
                version = application.getVersion();
            }
            apiList = new ArrayList<API>();
            apiList.add(apiProvider.getAPI(new APIIdentifier(provider, serviceId, version)));
        } catch (APIManagementException e) {
            log.error("couldn't find api for Service:" + serviceId, e);
        } catch (JAXBException e) {
            log.error("Couldn't parse xml file", e);
        }
        return apiList;
    }

    /**
     * To update an API
     *
     * @param serviceId service name of the service
     * @param username  username of the logged user
     * @param version   version of the api
     */
    public static void updateApi(String serviceId, String username, Data data, String version) {
        APIProvider apiProvider = getAPIProvider(username);
        Map<String, JSONArray> resourceMap = new LinkedHashMap<String, JSONArray>();
        JSONObject swagger12Json = new JSONObject();
        API api = createApiObject(serviceId, username, data, version, apiProvider, resourceMap);
        if (api != null) {
            try {
                apiProvider.changeAPIStatus(api, APIStatus.PROTOTYPED, username, false);
                apiProvider.updateAPI(api);
                api.setStatus(APIStatus.PROTOTYPED);
                updateSwagger12Definition(api, apiProvider, swagger12Json, resourceMap);
                apiProvider.changeAPIStatus(api, APIStatus.PUBLISHED, username, false);
            } catch (APIManagementException e) {
                log.error("error while updating api:" + serviceId + "for version:" + version, e);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * To update an API
     *
     * @param serviceId service name of the service
     * @param username  username of the logged user
     * @param version   version of the api
     */
    public LifeCycleEventDao[] getLifeCycleEventList(String serviceId, String username, String version) {
        APIProvider apiProvider = getAPIProvider(username);
        List<LifeCycleEvent> lifeCycleEventList;
        List<LifeCycleEventDao> lifeCycleEventDaoList = new ArrayList<LifeCycleEventDao>();
        String provider = org.wso2.carbon.apimgt.impl.utils.APIUtil.replaceEmailDomain(username);

        APIIdentifier apiIdentifier = new APIIdentifier(provider, serviceId, version);

        try {
            lifeCycleEventList = apiProvider.getLifeCycleEvents(apiIdentifier);
            if (lifeCycleEventList.size() > 0) {

                for (LifeCycleEvent lifeCycleEvent : lifeCycleEventList) {
                    LifeCycleEventDao lifeCycleEventDao;
                    if ((lifeCycleEvent.getOldStatus() != null)) {
                        lifeCycleEventDao =
                                new LifeCycleEventDao(apiIdentifier, lifeCycleEvent.getOldStatus().name(),
                                        lifeCycleEvent.getNewStatus().name(),
                                        lifeCycleEvent.getUserId(),
                                        lifeCycleEvent.getDate().toString());
                    } else
                        lifeCycleEventDao =
                                new LifeCycleEventDao(apiIdentifier, "", lifeCycleEvent.getNewStatus().name(),
                                        lifeCycleEvent.getUserId(),
                                        lifeCycleEvent.getDate().toString());
                    lifeCycleEventDaoList.add(lifeCycleEventDao);
                }
            }
        } catch (APIManagementException e) {
            log.error("error while getting lifecycle history api:" + serviceId + "for version:" + version, e);
        }

        return lifeCycleEventDaoList.toArray(new LifeCycleEventDao[lifeCycleEventDaoList.size()]);
    }

    /**
     * To create API model
     *
     * @param serviceId   service name of the service
     * @param username    username of the logged user
     * @param data        data service object
     * @param version     version of the api
     * @param apiProvider API Provider
     * @param resourceMap map of resources in swagger12 json
     * @return created api model
     */
    private static API createApiObject(String serviceId, String username, Data data, String version,
                                       APIProvider apiProvider,
                                       Map<String, JSONArray> resourceMap) {
        String apiEndpoint;
        String apiContext;
        String tenantDomain = MultitenantUtils.getTenantDomain(username);
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
            apiEndpoint =
                    "http://" + System.getProperty(HOST_NAME) + ":" + System.getProperty(HTTP_PORT) + "/services/" +
                            serviceId + "?wsdl";
            apiContext = "/api/" + serviceId;
        } else {
            apiEndpoint =
                    "http://" + System.getProperty(HOST_NAME) + ":" + System.getProperty(HTTP_PORT) + "/services/t/" +
                            tenantDomain + "/" + serviceId + "?wsdl";
            apiContext = "/t/" + tenantDomain + "/api/" + serviceId;
        }

        String provider = org.wso2.carbon.apimgt.impl.utils.APIUtil.replaceEmailDomain(username);
        String apiVersion;
        apiVersion = version;
        //noinspection UnnecessaryLocalVariable
        String apiName = serviceId;
        String authType = "Any";
        APIIdentifier identifier = new APIIdentifier(provider, apiName, apiVersion);
        return createAPIModel(apiProvider, apiContext, apiEndpoint, authType, identifier, data, resourceMap);
    }

    /**
     * To Create api_doc and info json objects in Swagger12 Json
     *
     * @param apiIdentifier API identifier
     * @param swagger12Json swagger12 json object
     * @throws ParseException
     */
    private static void createApiDocInfo(APIIdentifier apiIdentifier, JSONObject swagger12Json) throws ParseException {
        JSONObject api_doc = new JSONObject();
        api_doc.put("apiVersion", apiIdentifier.getVersion());
        api_doc.put("swaggerVersion", "1.2");
        api_doc.put("authorizations", new JSONParser().parse("{\"oauth2\":{\n" +
                "            \"scopes\":[\n" +
                "\n" +
                "            ],\n" +
                "            \"type\":\"oauth2\"\n" +
                "         }}"));
        api_doc.put("info", new JSONParser().parse("{\n" +
                "         \"title\":\"" + apiIdentifier.getApiName() + "\",\n" +
                "         \"termsOfServiceUrl\":\"\",\n" +
                "         \"description\":\"\",\n" +
                "         \"license\":\"\",\n" +
                "         \"contact\":\"\",\n" +
                "         \"licenseUrl\":\"\"\n" +
                "      }"));
        swagger12Json.put("api_doc", api_doc);
    }

    /**
     * To Create apis Json Object in swagger12 Json
     *
     * @param api api Object
     * @param swagger12Json swagger12 json object
     * @throws ParseException
     */
    private static void createApis(API api, JSONObject swagger12Json) throws ParseException {
        Set<String> resourceMap = new LinkedHashSet<String>();
        JSONArray jsonArray = new JSONArray();
        Set<URITemplate> uriTemplateSet = api.getUriTemplates();
        Set<URITemplate> tempUriTemplates = new HashSet<URITemplate>();
        for (URITemplate uriTemplate : uriTemplateSet) {
            String uriTemplateString = uriTemplate.getUriTemplate();
            if (Pattern.compile("[/][{]\\w+[}]").matcher(uriTemplateString).find()) {
                String tempUriTemplate = uriTemplateString.replaceAll("[/][{]\\w+[}]", "");
                resourceMap.add(tempUriTemplate);

                uriTemplate.setUriTemplate(uriTemplateString.replaceFirst("[/][{]\\w+[}]", "/*"));
                tempUriTemplates.add(uriTemplate);
            } else {
                resourceMap.add(uriTemplateString);
                tempUriTemplates.add(uriTemplate);
            }
        }
        api.setUriTemplates(tempUriTemplates);
        for (String resource : resourceMap) {
            jsonArray.add(new JSONParser().parse("{\n" +
                    "            \"description\":\"\",\n" +
                    "            \"path\":\"" + resource + "\"\n" +
                    "         }"));
        }
        JSONObject api_doc = (JSONObject) swagger12Json.get("api_doc");
        api_doc.put("apis", jsonArray);
        swagger12Json.remove("api_doc");
        swagger12Json.put("api_doc", api_doc);
    }

    /**
     * To Create API Resources in swagger12 definition
     *  @param api api Object
     * @param swagger12Json swagger12 json object
     * @param resourceMap map of resources in swagger12 json
     */
    private static void createAPIResources(API api, JSONObject swagger12Json, Map<String, JSONArray> resourceMap) {
        JSONArray resourcesArray = new JSONArray();
        Set<String> resources = resourceMap.keySet();
        for (String resource : resources) {
            JSONObject resourcesObject = new JSONObject();
            resourcesObject.put("apiVersion", api.getId().getVersion());
            resourcesObject.put("basePath",
                    "http://" + System.getProperty(HOST_NAME) + ":" + System.getProperty(HTTP_PORT) +
                            api.getContext() + api.getId().getVersion());
            resourcesObject.put("swaggerVersion", "1.2");
            resourcesObject.put("resourcePath", resource);
            resourcesObject.put("apis", resourceMap.get(resource));
            resourcesArray.add(resourcesObject);
        }
        swagger12Json.put("resources", resourcesArray);
    }

    /**
     * To Add api according to the Uri Template to Swagger12  Json
     *  @param template   URITemplate according to path
     * @param withParams parameters in Sql Query
     * @param resourceMap map of resources in swagger12 json
     */
    private static void addApiArray(URITemplate template, List<WithParam> withParams,
                                    Map<String, JSONArray> resourceMap) {
        String tempKey = template.getUriTemplate();
        String key = tempKey.replaceAll("[/][{]\\w+[}]", "");

        if (resourceMap.containsKey(key)) {
            JSONArray APIArray = resourceMap.get(key);
            if (APIArray != null) {
                JSONObject newApiObject = new JSONObject();
                newApiObject.put("path", template.getUriTemplate());
                JSONArray operationsArray = new JSONArray();
                JSONObject operationObject = new JSONObject();
                createOperationObject(withParams, template, operationObject);
                operationsArray.add(operationObject);
                newApiObject.put("operations", operationsArray);
                APIArray.add(newApiObject);
                resourceMap.remove(key);
                resourceMap.put(key, APIArray);
            } else {
                JSONArray newApiArray = new JSONArray();
                JSONObject newApiObject = new JSONObject();
                newApiObject.put("path", template.getUriTemplate());
                JSONArray operationsArray = new JSONArray();
                JSONObject operationObject = new JSONObject();
                createOperationObject(withParams, template, operationObject);
                operationsArray.add(operationObject);
                newApiObject.put("operations", operationsArray);
                newApiArray.add(newApiObject);
                resourceMap.put(key, newApiArray);
            }

        } else {
            JSONArray APIArray = new JSONArray();
            JSONObject newApiObject = new JSONObject();
            newApiObject.put("path", template.getUriTemplate());
            JSONArray operationsArray = new JSONArray();
            JSONObject operationObject = new JSONObject();
            createOperationObject(withParams, template, operationObject);
            operationsArray.add(operationObject);
            newApiObject.put("operations", operationsArray);
            APIArray.add(newApiObject);
            resourceMap.put(key, APIArray);
        }
    }

    /**
     * To create Operations objects in Swagger12
     *
     * @param withParams      parameters in Sql Query
     * @param template        URITemplate according to path
     * @param operationObject JSONObject of the Operation in Swagger12
     */
    private static void createOperationObject(List<WithParam> withParams, URITemplate template,
                                              JSONObject operationObject) {
        operationObject.put("nickname", template.getHTTPVerb().toLowerCase() + "_" +
                template.getUriTemplate().replaceFirst("/", ""));
        operationObject.put("method", template.getHTTPVerb().toUpperCase());
        JSONArray parametersArray = new JSONArray();
        String pathParam = null;
        Matcher paramNamePattern = Pattern.compile("[{]\\w+[}]").matcher(template.getUriTemplate());
        try {
            if (paramNamePattern.find()) {
                pathParam = paramNamePattern.group().replace("{", "");
                pathParam = pathParam.replace("}", "");
                parametersArray.add(new JSONParser().parse("{\n" +
                        "                           \"description\":\"Request Body\",\n" +
                        "                           \"name\":\"" + pathParam + "\",\n" +
                        "                           \"allowMultiple\":false,\n" +
                        "                           \"required\":true,\n" +
                        "                           \"type\":\"string\",\n" +
                        "                           \"paramType\":\"path\"\n" +
                        "                        }"));
            }
            for (WithParam param : withParams) {
                if (!param.getName().equals(pathParam)) {
                    if ("GET".equals(template.getHTTPVerb().toUpperCase())) {

                        parametersArray.add(new JSONParser().parse("{\n" +
                                "                           \"description\":\"Request Body\",\n" +
                                "                           \"name\":\"" + param.getName() + "\",\n" +
                                "                           \"allowMultiple\":false,\n" +
                                "                           \"required\":true,\n" +
                                "                           \"type\":\"string\",\n" +
                                "                           \"paramType\":\"query\"\n" +
                                "                        }"));
                    } else {
                        parametersArray.add(new JSONParser().parse("{\n" +
                                "                           \"description\":\"Request Body\",\n" +
                                "                           \"name\":\"" + param.getName() + "\",\n" +
                                "                           \"allowMultiple\":false,\n" +
                                "                           \"required\":true,\n" +
                                "                           \"type\":\"string\",\n" +
                                "                           \"paramType\":\"form\"\n" +
                                "                        }"));
                    }
                }


            }

        } catch (ParseException e) {
            log.error("Couldn't parse swagger parameter Json", e);
        }

        operationObject.put("parameters", parametersArray);
        operationObject.put("auth_type", template.getAuthType());
        operationObject.put("throttling_tier", template.getThrottlingTiers());
    }

    /**
     * To Update Swagger12 definition according to api
     *
     * @param api         api Object
     * @param apiProvider API Provider
     * @param swagger12Json swagger12 json object
     * @param resourceMap map of resources in swagger12 json
     * @throws APIManagementException
     */
    public static void updateSwagger12Definition(API api, APIProvider apiProvider, JSONObject swagger12Json,
                                                 Map<String, JSONArray> resourceMap) throws APIManagementException {
        try {
            createApiDocInfo(api.getId(),swagger12Json);
            createApis(api,swagger12Json);
            createAPIResources(api, swagger12Json, resourceMap);
            String apiJSON = ((JSONObject) swagger12Json.get("api_doc")).toJSONString();
            apiProvider.updateSwagger12Definition(api.getId(), APIConstants.API_DOC_1_2_RESOURCE_NAME, apiJSON);
            JSONArray resources = (JSONArray) swagger12Json.get("resources");
            for (Object resource : resources) {
                JSONObject tempResource = (JSONObject) resource;
                String resourcePath = (String) tempResource.get("resourcePath");
                apiProvider.updateSwagger12Definition(api.getId(), resourcePath, tempResource.toJSONString());
            }
        } catch (ParseException e) {
            log.error("couldn't Create Swagger12Json for Api " + api.getId().getApiName(), e);
        }
    }

    /**
     * This method is used to handle exceptions
     *
     * @param errorMessage error
     * @param e            throwable error
     * @throws DSSAPIException
     */

    public static void handleException(String errorMessage, Throwable e) throws DSSAPIException {
        log.error(errorMessage, e);
        throw new DSSAPIException(errorMessage, e);
    }

    /**
     *
     * @param serviceId data service Name
     * @return data object of data service
     * @throws AxisFault
     * @throws XMLStreamException
     */
    public static Data populateDataServiceData(String serviceId) throws AxisFault, XMLStreamException {
        String serviceContents = new DataServiceAdmin().getDataServiceContentAsString(serviceId);
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(
                new StringReader(serviceContents));
        OMElement configElement = (new StAXOMBuilder(reader)).getDocumentElement();
        configElement.build();
        Data data = new Data();
        data.populate(configElement);
        return data;
    }
}
