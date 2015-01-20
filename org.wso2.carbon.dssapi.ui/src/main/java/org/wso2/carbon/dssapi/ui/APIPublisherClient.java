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
package org.wso2.carbon.dssapi.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.dssapi.model.xsd.API;
import org.wso2.carbon.dssapi.model.xsd.LifeCycleEventDao;
import org.wso2.carbon.dssapi.stub.APIPublisherStub;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * API publisher client to talk to stub
 */
public class APIPublisherClient {

    //private static Log log = LogFactory.getLog(APIPublisherClient.class);
    APIPublisherStub stub;

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
     * @param serviceName name of the service
     * @return api availability
     * @throws RemoteException
     */
    public boolean checkApiAvailability(String serviceName) throws RemoteException {
        return stub.checkApiAvailability(serviceName);
    }


    /**
     * To publish API for a given service
     *
     * @param serviceName name of the service
     * @return status of the operation
     * @throws RemoteException
     */
    public boolean publishAPI(String serviceName, String version) throws RemoteException {
        return stub.addApi(serviceName, version);
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
     * To retrieve number of active subscriptions for the service
     *
     * @param serviceName name of the service
     * @return number of subscriptions
     * @throws RemoteException
     */
    public long checkNumberOfSubscriptions(String serviceName, String version) throws RemoteException {
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

    /**
     * To get the published date of the API
     *
     * @param serviceName  name of the service
     * @param version     version of the api
     * @return formatted published date
     * @throws RemoteException
     */
    public String getPublishedDate(String serviceName, String version) throws RemoteException, ParseException {
        LifeCycleEventDao[] cycleEventDaos = stub.listLifeCycleEvents(serviceName, version);
        SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd',' hh:mm:ss");
        return dateFormat.format(parseFormat.parse(cycleEventDaos[cycleEventDaos.length - 1].getDate()));
        //return cycleEventDaos[0].getDate();

        //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
       // return dateFormat.parse(publishedDate).toString();
        //return parsedDate.toString();
        //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        //return dateFormat.format(publishedDate).toString();
        //return publishedDate;
    }

    /**
     * To get the updated date of the API
     *
     * @param serviceName name of the service
     * @param version     version of the api
     * @return formatted updated date
     * @throws RemoteException
     */
    public String getUpdatedDate(String serviceName, String version) throws RemoteException, ParseException {
        LifeCycleEventDao[] cycleEventDaos = stub.listLifeCycleEvents(serviceName, version);
        SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd',' hh:mm:ss");
        return dateFormat.format(parseFormat.parse(cycleEventDaos[cycleEventDaos.length - 1].getDate()));

       // SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
       // Date parsedDate = dateFormat.parse(publishedDate);
       // return parsedDate.toString();
        //Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());

        //Timestamp t = publishedDate.
        //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        //return dateFormat.format(publishedDate).toString();
        //return publishedDate;
    }

    /**
     * To get the history of the API
     *
     * @param serviceName name of the service
     * @param version     version of the api
     * @return api life cycle history
     * @throws RemoteException
     */
    public LifeCycleEventDao[] getLifeCycleEvents(String serviceName, String version) throws RemoteException {
        return stub.listLifeCycleEvents(serviceName, version);
    }

    /**
     * To update the API
     *
     * @param serviceName name of the service
     * @param version     version of the api
     * @return the status of the operation
     * @throws RemoteException
     */
    public boolean updateApi(String serviceName, String version) throws RemoteException {
        return stub.updateApi(serviceName, version);
    }
}
