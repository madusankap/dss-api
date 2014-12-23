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
package org.wso2.carbon.dssapi.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.dssapi.model.xsd.API;
import org.wso2.carbon.dssapi.model.xsd.LifeCycleEventDao;
import org.wso2.carbon.dssapi.stub.APIPublisherException;
import org.wso2.carbon.dssapi.stub.APIPublisherStub;
import org.wso2.carbon.service.mgt.xsd.ServiceMetaData;
import org.wso2.carbon.service.mgt.xsd.ServiceMetaDataWrapper;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * API publisher client to talk to stub
 */
public class APIPublisherClient {

    //private static Log log = LogFactory.getLog(APIPublisherClient.class);
    APIPublisherStub stub;
    ServiceMetaDataWrapper serviceMetaDataWrapper;

    public APIPublisherClient(String cookie, String url, ConfigurationContext configContext) throws AxisFault {
        String serviceEndpoint = "";

        serviceEndpoint = url + "APIPublisher";
        stub = new APIPublisherStub(configContext, serviceEndpoint);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    /**
     * To get number of pages to display
     *
     * @return number of pages
     * @throws RemoteException
     * @throws APIPublisherException
     */
    public int getNumberOfPages() throws RemoteException, APIPublisherException {
        serviceMetaDataWrapper = stub.listDssServices("", 0);
        return serviceMetaDataWrapper.getNumberOfPages();
    }

    /**
     * To get number of active services
     *
     * @return number of active services
     * @throws RemoteException
     * @throws APIPublisherException
     */
    public int getNumberOfActiveServices() throws Exception {
        return stub.listDssServices("", 0).getNumberOfActiveServices();
    }

    /**
     * To get number of services for API operations
     *
     * @return number of services for API operations
     * @throws RemoteException
     * @throws APIPublisherException
     */
    public int getNumberOfServices() throws Exception {
        return stub.listDssServices("", 0).getServices().length;
    }

    /**
     * To get all available services
     *
     * @return all the services
     * @throws RemoteException
     * @throws APIPublisherException
     */
    public ServiceMetaData[] getServices(String searchQuery) throws RemoteException, APIPublisherException {
        return stub.listDssServices(searchQuery, 0).getServices();
    }

    /**
     * @param serviceName name of the service
     * @return api availability
     * @throws RemoteException
     */
    public boolean isAPIAvailable(String serviceName) throws RemoteException {
        return stub.apiAvailable(serviceName, "");
    }


    /**
     * To publish API for a given service
     *
     * @param serviceMetaData service details
     * @return status of the operation
     * @throws RemoteException
     * @throws APIPublisherException
     */
    public boolean publishAPI(ServiceMetaData serviceMetaData, String version) throws RemoteException, APIPublisherException {
        String serviceId = serviceMetaData.getName();
        return stub.addApi(serviceId, version);
    }

    /**
     * To un-publish API for a given service
     *
     * @param serviceName name of the service
     * @param version     version of the service
     * @return status of the operation
     * @throws RemoteException
     */
    public boolean unpublishAPI(String serviceName, String version) throws RemoteException {
        return stub.removeApi(serviceName, version);
    }


    /**
     * To get number of faulty services
     *
     * @param serviceMetaData service details
     * @return number of faulty service groups
     * @throws RemoteException
     * @throws APIPublisherException
     */
    public int getNumofFaultyServices(ServiceMetaData serviceMetaData) throws RemoteException, APIPublisherException {
        return stub.listDssServices("", 0).getNumberOfFaultyServiceGroups();
    }

    /**
     * To get the service details by service name
     *
     * @param serviceName name of the service
     * @return service details
     * @throws RemoteException
     * @throws APIPublisherException
     */
    public ServiceMetaDataWrapper getServiceData(String serviceName) throws RemoteException, APIPublisherException {
        return stub.listDssServices(serviceName, 0);
    }

    /**
     * To retrieve number of active subscriptions for the service
     *
     * @param serviceName name of the service
     * @return number of subscriptions
     * @throws RemoteException
     */
    public long checkNumberOfSubcriptions(String serviceName, String version) throws RemoteException {
        return stub.viewSubscriptions(serviceName, version);

    }

    /**
     * To get the current version of the API
     *
     * @param serviceName name of the service
     * @return the current API version
     * @throws RemoteException
     */
    public String getCurrentApiVersion(String serviceName) throws RemoteException {
        API[] apiArray = stub.listApi(serviceName);
        return apiArray[apiArray.length - 1].getApiVersion();
    }

    public String getPublishedDate(String serviceName, String version) throws RemoteException {
        LifeCycleEventDao[] cycleEventDaos = stub.listLifeCycleEvents(serviceName, version);
        Date publishedDate = cycleEventDaos[0].getDate();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        return dateFormat.format(publishedDate).toString();
    }

    public String getUpdatedDate(String serviceName, String version) throws RemoteException {
        LifeCycleEventDao[] cycleEventDaos = stub.listLifeCycleEvents(serviceName, version);
        Date publishedDate = cycleEventDaos[cycleEventDaos.length - 1].getDate();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        return dateFormat.format(publishedDate).toString();
    }

    public LifeCycleEventDao[] getLifeCycleEvents(String serviceName, String version) throws RemoteException {
        return stub.listLifeCycleEvents(serviceName, version);
    }
}
