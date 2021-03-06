/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.dssapi.observer;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.axis2.engine.AxisObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.dataservices.common.DBConstants;
import org.wso2.carbon.dataservices.core.admin.DataServiceAdmin;
import org.wso2.carbon.dataservices.core.engine.DataService;
import org.wso2.carbon.dataservices.ui.beans.Data;
import org.wso2.carbon.dssapi.model.Application;
import org.wso2.carbon.dssapi.util.APIUtil;
import org.wso2.carbon.service.mgt.ServiceAdmin;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;


/**
 * Observer class to observe the service updates, parameter changes and other changes in data service
 * Later the published api will be changed according to the updates
 */

public class APIObserver implements AxisObserver {
    private static final Log log = LogFactory.getLog(APIObserver.class);


    @Override
    public void init(AxisConfiguration axisConfiguration) {
    }

    @Override
    public void serviceUpdate(AxisEvent axisEvent, AxisService axisService){

        if (axisEvent.getEventType() == AxisEvent.SERVICE_DEPLOY) {
            DataService dataService =
                    (DataService) axisService.getParameter(DBConstants.DATA_SERVICE_OBJECT).getValue();
            if (dataService != null) {
                String location = dataService.getDsLocation();
                location = location.substring(0, location.lastIndexOf("/"));
                File file = new File(location + "/" + dataService.getName() + APIUtil.APPLICATION_XML);
                if (file.exists()) {
                    Application application;
                    try {

                        JAXBContext jaxbContext = JAXBContext.newInstance(Application.class);
                        Unmarshaller jaxbUnMarshaller = jaxbContext.createUnmarshaller();
                        jaxbUnMarshaller.setProperty("jaxb.encoding", "UTF-8");
                        application = (Application) jaxbUnMarshaller.unmarshal(file);
                        String serviceContents =
                                new DataServiceAdmin().getDataServiceContentAsString(dataService.getName());
                        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(
                                new StringReader(serviceContents));
                        OMElement configElement = (new StAXOMBuilder(reader)).getDocumentElement();
                        configElement.build();
                        Data data = new Data();
                        data.populate(configElement);
                        String tempDeployedTime =
                                new ServiceAdmin(DataHolder.getConfigurationContext().getAxisConfiguration())
                                        .getServiceData(data.getName()).getServiceDeployedTime();
                        if (!application.getDeployedTime().equalsIgnoreCase(tempDeployedTime)) {
                            APIUtil.updateApi(dataService.getName(), application.getUserName(), data,
                                    application.getVersion());
                            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                            jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
                            application.setDeployedTime(tempDeployedTime);
                            jaxbMarshaller.marshal(application, file);
                        }
                    } catch (JAXBException e) {
                        log.error("An error occurred while reading the data for " + dataService.getName(), e);
                    } catch (XMLStreamException e) {
                        log.error("An error occurred while reading xml data for " + dataService.getName(), e);
                    } catch (AxisFault axisFault) {
                        log.error("An error occurred while reading the service " + dataService.getName(), axisFault);
                    } catch (Exception e) {
                        log.error("Couldn't get service meta data for the service " + dataService.getName(), e);
                    }
                    //Logged observed errors and let the process to continue
                }

            }
        }

    }

    @Override
    public void serviceGroupUpdate(AxisEvent axisEvent, AxisServiceGroup axisServiceGroup) {

    }

    @Override
    public void moduleUpdate(AxisEvent axisEvent, AxisModule axisModule) {

    }

    @Override
    public void addParameter(Parameter parameter) throws AxisFault {

    }

    @Override
    public void removeParameter(Parameter parameter) throws AxisFault {

    }

    @Override
    public void deserializeParameters(OMElement omElement) throws AxisFault {

    }

    @Override
    public Parameter getParameter(String s) {
        return null;
    }

    @Override
    public ArrayList<Parameter> getParameters() {
        return null;
    }

    @Override
    public boolean isParameterLocked(String s) {
        return false;
    }
}
