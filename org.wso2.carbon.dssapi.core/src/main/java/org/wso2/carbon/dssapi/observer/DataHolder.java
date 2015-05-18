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

import org.apache.axis2.context.ConfigurationContext;

/**
 * Data holder class set and get configuration context
 */
public class DataHolder {
    private static ConfigurationContext configurationContext;

    public static ConfigurationContext unSetConfigurationContext(ConfigurationContext configurationContext) {
        return configurationContext;
    }

    public static void setConfigurationContext(ConfigurationContext configurationContext) {
        DataHolder.configurationContext = configurationContext;
    }

    public static ConfigurationContext getConfigurationContext() {
        return configurationContext;
    }
}
