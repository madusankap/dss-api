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

package org.wso2.carbon.dssapi.core;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.dataservices.core.admin.DataServiceAdmin;
import org.wso2.carbon.dataservices.ui.beans.Data;
import org.wso2.carbon.dssapi.model.LifeCycleEventDao;
import org.wso2.carbon.dssapi.util.APIUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * To handle the API operations
 */
public class APIPublisher {
    private static final Log log = LogFactory.getLog(APIPublisher.class);

    /**
     * To check whether API is available for the given service or not
     *
     * @param serviceName name of the service
     * @return availability of api to DataServices
     */
    public boolean checkApiAvailability(String serviceName) {
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        if (log.isDebugEnabled()) {
            log.debug("check api available for service name:" + serviceName);
        }
        return new APIUtil().checkApiAvailability(serviceName, tenantId);
    }

    /**
     * To check whether API have active Subscriptions for the given service or not
     *
     * @param serviceName name of the service
     * @param version     version of the api
     * @return no of subscriptions to api to that  DataServices
     */
    public long viewSubscriptions(String serviceName, String version) {

        String username =
                MultitenantUtils.getTenantAwareUsername(CarbonContext.getThreadLocalCarbonContext().getUsername());

        if (log.isDebugEnabled()) {
            log.debug("check subscriptions for the API:" + serviceName + "and version:" + version);
        }
        return new APIUtil().getApiSubscriptions(serviceName, username, version);
    }

    /**
     * To list APIs by DataService Name
     *
     * @param serviceName name of the service
     * @return List of Api according to DataService
     */
    public org.wso2.carbon.dssapi.model.API[] listApi(String serviceName) {
        String username =
                MultitenantUtils.getTenantAwareUsername(CarbonContext.getThreadLocalCarbonContext().getUsername());
        List<API> apiList = new APIUtil().getApi(serviceName, username);
        List<org.wso2.carbon.dssapi.model.API> listApi = new ArrayList<org.wso2.carbon.dssapi.model.API>();
        if (!apiList.isEmpty()) {
            for (API api : apiList) {
                org.wso2.carbon.dssapi.model.API tempApi =
                        new org.wso2.carbon.dssapi.model.API(api.getId().getApiName(), api.getId().getVersion(),
                                                             api.getLastUpdated(), api.getStatus().getStatus());
                listApi.add(tempApi);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("list api available for service name:" + serviceName);
        }
        return listApi.toArray(new org.wso2.carbon.dssapi.model.API[listApi.size()]);
    }

    /**
     * To list LifeCycle Events by DataService Name and version
     *
     * @param serviceName name of the service
     * @param version     version of the api
     * @return List of LifeCycles according to the api.
     */
    public LifeCycleEventDao[] listLifeCycleEvents(String serviceName, String version) {
        String username =
                MultitenantUtils.getTenantAwareUsername(CarbonContext.getThreadLocalCarbonContext().getUsername());
        if (log.isDebugEnabled()) {
            log.debug("list life cycle history for API name:" + serviceName + "& version:" + version);
        }
        return new APIUtil().getLifeCycleEventList(serviceName, username, version);
    }

    /**
     * To add an API for a service
     *
     * @param serviceId service id of the service
     * @param version   version of the api
     */
    public boolean addApi(String serviceId, String version) throws DSSAPIException {
        String serviceContents;
        boolean status = false;
        try {
            serviceContents = new DataServiceAdmin().getDataServiceContentAsString(serviceId);
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(
                    new StringReader(serviceContents));
            OMElement configElement = (new StAXOMBuilder(reader)).getDocumentElement();
            configElement.build();
            Data data = new Data();
            data.populate(configElement);
	        String username =
			        MultitenantUtils.getTenantAwareUsername(CarbonContext.getThreadLocalCarbonContext().getUsername());
	        new APIUtil().addApi(serviceId, username,data, version);
            status = true;
            if (log.isDebugEnabled()) {
                log.debug("api created for Service Name:" + serviceId + "and for version:" + version);
            }
        } catch (AxisFault axisFault) {
            APIUtil.handleException("Couldn't create api for Service: " + serviceId, axisFault);
        } catch (XMLStreamException e) {
            APIUtil.handleException("Couldn't create api for Service: " + serviceId, e);
        }
        return status;
    }

    /**
     * To remove the API
     *
     * @param serviceId service id of the service
     * @param version   version of the api want to remove
     * @return api is removed from api manager
     */
    public boolean removeApi(String serviceId, String version) throws DSSAPIException {
        boolean status;

	    String username = MultitenantUtils.getTenantAwareUsername(
                CarbonContext.getThreadLocalCarbonContext().getUsername());
	    status = new APIUtil().removeApi(serviceId, username, version);
        if (log.isDebugEnabled()) {
            log.debug("api for Service:" + serviceId + "on version:" + version + " successfully removed");
        }

        return status;
    }


    /**
     * To update an API for a service
     *
     * @param serviceId service id of the service
     * @param version   version of the api
     */
    public boolean updateApi(String serviceId, String version) throws DSSAPIException{
        String serviceContents;
        boolean status = false;
        try {
            serviceContents = new DataServiceAdmin().getDataServiceContentAsString(serviceId);
            InputStream ins = new ByteArrayInputStream(serviceContents.getBytes());
            OMElement configElement = (new StAXOMBuilder(ins)).getDocumentElement();
            configElement.build();
            Data data = new Data();
            data.populate(configElement);
	        String username = MultitenantUtils.getTenantAwareUsername(
                    CarbonContext.getThreadLocalCarbonContext().getUsername());
	        new APIUtil().updateApi(serviceId, username,data, version);
            status = true;
            if (log.isDebugEnabled()) {
                log.debug("api for Service:" + serviceId + "on version:" + version + " successfully updated");
            }
        } catch (AxisFault axisFault) {
            APIUtil.handleException("Couldn't update api for Service: " + serviceId, axisFault);
        } catch (XMLStreamException e) {
            APIUtil.handleException("Couldn't update api for Service: " + serviceId, e);
        }
        return status;
    }
}