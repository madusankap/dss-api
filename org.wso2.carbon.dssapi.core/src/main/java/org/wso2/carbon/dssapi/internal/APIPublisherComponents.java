package org.wso2.carbon.dssapi.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.core.multitenancy.TenantAxisConfiguration;
import org.wso2.carbon.core.multitenancy.TenantAxisConfigurator;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.dssapi.observer.APIObserver;
import org.wso2.carbon.dssapi.org.wso2.carbon.dssapi.valve.DSSAPIValve;
import org.wso2.carbon.tomcat.ext.valves.CarbonTomcatValve;
import org.wso2.carbon.tomcat.ext.valves.TomcatValveContainer;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.ArrayList;

/**

 * @scr.component name="org.wso2.carbon.dssapi.internal" immediate="true"

 * @scr.reference name="config.context.service" interface="org.wso2.carbon.utils.ConfigurationContextService"

 * cardinality="1..1" policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 */
public class APIPublisherComponents {
    private ConfigurationContext cfgCtx;
    private static final Log log = LogFactory.getLog(APIObserver.class);
    protected void activate(ComponentContext ctxt) throws Exception {
         if(ctxt!=null){
            AxisConfiguration axisConfig = cfgCtx.getAxisConfiguration();
            APIObserver apiObserver=new APIObserver();
            apiObserver.init(axisConfig);
           axisConfig.addObservers(apiObserver);
             APIPublisherConfigurator apiPublisherConfigurator =new APIPublisherConfigurator();
             ctxt.getBundleContext().registerService(Axis2ConfigurationContextObserver.class.getName(), apiPublisherConfigurator, null);
             ArrayList<CarbonTomcatValve> valves = new ArrayList<CarbonTomcatValve>();
             valves.add(new DSSAPIValve());
             TomcatValveContainer.addValves(valves);
         }
    }
    protected void setConfigurationContextService(ConfigurationContextService cfgCtxService) {
        if (log.isDebugEnabled()) {
            log.debug("ConfigurationContextService bound to DSSAPI component");
        }
        cfgCtx = cfgCtxService.getServerConfigContext();
    }

    protected void unsetConfigurationContextService(ConfigurationContextService cfgCtxService) {
        if (log.isDebugEnabled()) {
            log.debug("ConfigurationContextService unbound from the DSSAPI Component");
        }
        cfgCtx = null;
    }
}
