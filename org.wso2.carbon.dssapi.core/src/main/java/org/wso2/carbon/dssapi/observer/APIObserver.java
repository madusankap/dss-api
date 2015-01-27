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
import org.wso2.carbon.dataservices.core.DBUtils;
import org.wso2.carbon.dataservices.core.admin.DataServiceAdmin;
import org.wso2.carbon.dataservices.core.engine.DataService;
import org.wso2.carbon.dataservices.ui.beans.Data;
import org.wso2.carbon.dssapi.model.Application;
import org.wso2.carbon.dssapi.util.APIUtil;
import org.wso2.carbon.service.mgt.ServiceAdmin;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class APIObserver implements AxisObserver {
    private static final Log log = LogFactory.getLog(APIObserver.class);


    @Override
    public void init(AxisConfiguration axisConfiguration) {
    }

     @Override
    public void serviceUpdate(AxisEvent axisEvent, AxisService axisService) {

         if(axisEvent.getEventType()==AxisEvent.SERVICE_DEPLOY){
             DataService dataService = (DataService) axisService.getParameter(DBConstants.DATA_SERVICE_OBJECT).getValue();
                 if (dataService != null) {
                     String location = dataService.getDsLocation();
                     location = location.substring(0, location.lastIndexOf("/"));
                     File file = new File(location + "/" + dataService.getName() + APIUtil.APPLICATION_XML);
                     if (file.exists()) {
                         Application application;
                         try {

                             JAXBContext jaxbContext = JAXBContext.newInstance(Application.class);
                             Unmarshaller jaxbUnMarshaller = jaxbContext.createUnmarshaller();
                             application = (Application) jaxbUnMarshaller.unmarshal(file);
                             String serviceContents = new DataServiceAdmin().getDataServiceContentAsString(dataService.getName());
                             InputStream ins = new ByteArrayInputStream(serviceContents.getBytes());
                             OMElement configElement = (new StAXOMBuilder(ins)).getDocumentElement();
                             configElement.build();
                             Data data = new Data();
                             data.populate(configElement);
                             String tempDeployedTime=new ServiceAdmin(DataHolder.getConfigurationContext().getAxisConfiguration()).getServiceData(data.getName()).getServiceDeployedTime();
                             if(!application.getDeployedTime().equalsIgnoreCase(tempDeployedTime)){
                                 new APIUtil().updateApi(dataService.getName(), application.getUserName(), application.getTenantDomain(), data, application.getVersion());
                                 Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                                 jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                                 application.setDeployedTime(tempDeployedTime);
                                 jaxbMarshaller.marshal(application, file);
                             }
                         }  catch (Exception e) {
                             log.error("Couldn't get ServiceMetaData Object for the service"+dataService.getName(),e);
                         }

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
