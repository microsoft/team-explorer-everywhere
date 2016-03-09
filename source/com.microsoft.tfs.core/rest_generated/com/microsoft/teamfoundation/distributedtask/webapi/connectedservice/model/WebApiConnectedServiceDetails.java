// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

 /*
* ---------------------------------------------------------
* Generated file, DO NOT EDIT
* ---------------------------------------------------------
*
* See following wiki page for instructions on how to regenerate:
*   https://vsowiki.com/index.php?title=Rest_Client_Generation
*/

package com.microsoft.teamfoundation.distributedtask.webapi.connectedservice.model;


/** 
 */
public class WebApiConnectedServiceDetails
    extends WebApiConnectedServiceRef {

    /**
    * Meta data for service connection
    */
    private WebApiConnectedService connectedServiceMetaData;
    /**
    * Credential info
    */
    private String credentialsXml;
    /**
    * Optional uri to connect directly to the service such as https://windows.azure.com
    */
    private String endPoint;

    /**
    * Meta data for service connection
    */
    public WebApiConnectedService getConnectedServiceMetaData() {
        return connectedServiceMetaData;
    }

    /**
    * Meta data for service connection
    */
    public void setConnectedServiceMetaData(final WebApiConnectedService connectedServiceMetaData) {
        this.connectedServiceMetaData = connectedServiceMetaData;
    }

    /**
    * Credential info
    */
    public String getCredentialsXml() {
        return credentialsXml;
    }

    /**
    * Credential info
    */
    public void setCredentialsXml(final String credentialsXml) {
        this.credentialsXml = credentialsXml;
    }

    /**
    * Optional uri to connect directly to the service such as https://windows.azure.com
    */
    public String getEndPoint() {
        return endPoint;
    }

    /**
    * Optional uri to connect directly to the service such as https://windows.azure.com
    */
    public void setEndPoint(final String endPoint) {
        this.endPoint = endPoint;
    }
}
