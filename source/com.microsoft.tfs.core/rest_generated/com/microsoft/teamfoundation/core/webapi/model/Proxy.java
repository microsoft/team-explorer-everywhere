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

package com.microsoft.teamfoundation.core.webapi.model;


/** 
 */
public class Proxy {

    /**
    * This is a description string
    */
    private String description;
    /**
    * The friendly name of the server
    */
    private String friendlyName;
    private boolean globalDefault;
    /**
    * This is a string representation of the site that the proxy server is located in (e.g. "NA-WA-RED")
    */
    private String site;
    private boolean siteDefault;
    /**
    * The URL of the proxy server
    */
    private String url;

    /**
    * This is a description string
    */
    public String getDescription() {
        return description;
    }

    /**
    * This is a description string
    */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
    * The friendly name of the server
    */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
    * The friendly name of the server
    */
    public void setFriendlyName(final String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public boolean getGlobalDefault() {
        return globalDefault;
    }

    public void setGlobalDefault(final boolean globalDefault) {
        this.globalDefault = globalDefault;
    }

    /**
    * This is a string representation of the site that the proxy server is located in (e.g. &quot;NA-WA-RED&quot;)
    */
    public String getSite() {
        return site;
    }

    /**
    * This is a string representation of the site that the proxy server is located in (e.g. &quot;NA-WA-RED&quot;)
    */
    public void setSite(final String site) {
        this.site = site;
    }

    public boolean getSiteDefault() {
        return siteDefault;
    }

    public void setSiteDefault(final boolean siteDefault) {
        this.siteDefault = siteDefault;
    }

    /**
    * The URL of the proxy server
    */
    public String getUrl() {
        return url;
    }

    /**
    * The URL of the proxy server
    */
    public void setUrl(final String url) {
        this.url = url;
    }
}
