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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.dssapi.observer.APIObserver;
import org.wso2.carbon.dssapi.observer.DataHolder;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;


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
