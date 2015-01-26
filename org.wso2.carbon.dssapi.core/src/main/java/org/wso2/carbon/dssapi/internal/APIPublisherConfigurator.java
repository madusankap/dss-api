package org.wso2.carbon.dssapi.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.dssapi.observer.APIObserver;
import org.wso2.carbon.dssapi.observer.DataHolder;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;

import javax.xml.crypto.Data;


public class APIPublisherConfigurator extends AbstractAxis2ConfigurationContextObserver {
    private static final Log log = LogFactory.getLog(APIPublisherConfigurator.class);

    @Override
    public void createdConfigurationContext(ConfigurationContext configContext) {
        APIObserver apiObserver=new APIObserver();
        apiObserver.init(configContext.getAxisConfiguration());
        configContext.getAxisConfiguration().addObservers(apiObserver);
        DataHolder.setConfigurationContext(configContext);
            }

    @Override
    public void terminatedConfigurationContext(ConfigurationContext configCtx) {
        log.info("Configuration Context for Tenant: "+ TenantAxisUtils.getTenantId(configCtx)+ "terminated.....");
        DataHolder.unSetConfigurationContext(null);
    }
}
