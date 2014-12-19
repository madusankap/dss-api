package org.wso2.carbon.dssapi.model;

import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.APIStatus;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by tharindud on 12/19/14.
 */
public class LifeCycleEventDao implements Serializable {
    private APIIdentifier api;
    private String oldStatus;
    private String newStatus;
    private String userId;
    private Date date;

    public APIIdentifier getApi() {
        return api;
    }

    public void setApi(APIIdentifier api) {
        this.api = api;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public LifeCycleEventDao() {

    }

    public LifeCycleEventDao(APIIdentifier api, String oldStatus, String newStatus, String userId, Date date) {
        this.api = api;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.userId = userId;
        this.date = date;
    }
}
