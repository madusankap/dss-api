package org.wso2.carbon.dssapi.internal;

import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.dssapi.observer.APIObserver;

/**
 * Created by tharindud on 1/22/15.
 */
public class APIPublisherComponents {
    protected void activate(ComponentContext ctxt) {
        ctxt.getBundleContext().registerService(APIObserver.class.getName(), new APIObserver(), null);
    }
}
