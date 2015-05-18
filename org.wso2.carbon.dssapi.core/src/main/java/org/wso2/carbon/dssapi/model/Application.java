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
package org.wso2.carbon.dssapi.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "api")
@SuppressWarnings("unused")
/**
 * Model class for Application
 */
public class Application {
    private boolean managedApi;
    private String version;
    private String userName;
    private String tenantDomain;
    private String deployedTime;

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
