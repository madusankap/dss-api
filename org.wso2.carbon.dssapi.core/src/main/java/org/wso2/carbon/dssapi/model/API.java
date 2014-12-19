package org.wso2.carbon.dssapi.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by tharindud on 12/18/14.
 */
public class API implements Serializable {
   private String apiName;
    private String apiVersion;
    private Date lastUpdated;
    private String status;

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public API(String apiName, String apiVersion, Date lastUpdated, String status) {
        this.apiName = apiName;
        this.apiVersion = apiVersion;
        this.lastUpdated = lastUpdated;
        this.status = status;
    }

    public API() {
    }
}
