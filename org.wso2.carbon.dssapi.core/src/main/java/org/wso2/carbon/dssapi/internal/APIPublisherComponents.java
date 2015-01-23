package org.wso2.carbon.dssapi.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.dssapi.observer.APIObserver;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * Created by tharindud on 1/22/15.
 */
public class APIPublisherComponents {
    private ConfigurationContext cfgCtx;
    private static final Log log = LogFactory.getLog(APIObserver.class);
    protected void activate(ComponentContext ctxt) {
        if(ctxt!=null){
            AxisConfiguration axisConfig = cfgCtx.getAxisConfiguration();
            APIObserver apiObserver=new APIObserver();
            apiObserver.init(axisConfig);
            axisConfig.addObservers(apiObserver);
        }
    }
    protected void setConfigurationContextService(ConfigurationContextService cfgCtxService) {
        if (log.isDebugEnabled()) {
            log.debug("ConfigurationContextService bound to the discovery proxy component");
        }
        cfgCtx = cfgCtxService.getServerConfigContext();
    }

    protected void unsetConfigurationContextService(ConfigurationContextService cfgCtxService) {
        if (log.isDebugEnabled()) {
            log.debug("ConfigurationContextService unbound from the discovery proxy component");
        }
        cfgCtx = null;
    }
}
