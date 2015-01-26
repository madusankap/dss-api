package org.wso2.carbon.dssapi.observer;

import org.apache.axis2.context.ConfigurationContext;

/**
 * Created by tharindud on 1/26/15.
 */
public class DataHolder {
    private static ConfigurationContext configurationContext;

    public static ConfigurationContext unSetConfigurationContext(ConfigurationContext configurationContext) {
        return configurationContext;
    }

    public static void setConfigurationContext (ConfigurationContext configurationContext) {
        DataHolder.configurationContext= configurationContext;
    }

    public static ConfigurationContext getConfigurationContext() {
        return configurationContext;
    }
}
