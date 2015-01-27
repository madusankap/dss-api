package org.wso2.carbon.dssapi.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by tharindud on 1/27/15.
 */
@XmlRootElement(name = "api")
public class Application {
    private boolean managedApi;
private String version,userName,tenantDomain,deployedTime;

    public Application(boolean managedApi, String deployedTime, String userName, String version, String tenantDomain) {
        this.managedApi = managedApi;
        this.deployedTime = deployedTime;
        this.userName = userName;
        this.version = version;
        this.tenantDomain = tenantDomain;
    }

    public Application() {
    }

    public boolean getManagedApi() {
        return managedApi;
    }
    @XmlElement
    public void setManagedApi(boolean managedApi) {
        this.managedApi = managedApi;
    }

    public String getVersion() {
        return version;
    }
    @XmlElement
    public void setVersion(String version) {
        this.version = version;
    }
    public String getUserName() {
        return userName;
    }
    @XmlElement
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }
    @XmlElement
    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public String getDeployedTime() {
        return deployedTime;
    }
    @XmlElement
    public void setDeployedTime(String deployedTime) {
        this.deployedTime = deployedTime;
    }
}
