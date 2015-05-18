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
package org.wso2.carbon.dssapi.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.dssapi.observer.APIObserver;
import org.wso2.carbon.dssapi.valve.DSSAPIValve;
import org.wso2.carbon.tomcat.ext.valves.CarbonTomcatValve;
import org.wso2.carbon.tomcat.ext.valves.TomcatValveContainer;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.ArrayList;

/**
 * @scr.component name="org.wso2.carbon.dssapi.internal" immediate="true"
 * @scr.reference name="config.context.service" interface="org.wso2.carbon.utils.ConfigurationContextService"
 * <p/>
 * cardinality="1..1" policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 */

/**
 * bundle activator
 */
@SuppressWarnings("unused")
public class APIPublisherComponents {
    private ConfigurationContext configurationContext;
    private static final Log log = LogFactory.getLog(APIObserver.class);

    protected void activate(ComponentContext context) {
        if (context != null) {
            AxisConfiguration axisConfig = configurationContext.getAxisConfiguration();
            APIObserver apiObserver = new APIObserver();
            apiObserver.init(axisConfig);
            axisConfig.addObservers(apiObserver);
            APIPublisherConfigurator apiPublisherConfigurator = new APIPublisherConfigurator();
            context.getBundleContext()
                    .registerService(Axis2ConfigurationContextObserver.class.getName(), apiPublisherConfigurator, null);
            ArrayList<CarbonTomcatValve> valves = new ArrayList<CarbonTomcatValve>();
            valves.add(new DSSAPIValve());
            TomcatValveContainer.addValves(valves);
        }
    }

    protected void setConfigurationContextService(ConfigurationContextService cfgCtxService) {
        if (log.isDebugEnabled()) {
            log.debug("ConfigurationContextService bound to DSSAPI component");
        }
        configurationContext = cfgCtxService.getServerConfigContext();
    }

    protected void unsetConfigurationContextService(ConfigurationContextService cfgCtxService) {
        if (log.isDebugEnabled()) {
            log.debug("ConfigurationContextService unbound from the DSSAPI Component");
        }
        configurationContext = null;
    }
}
