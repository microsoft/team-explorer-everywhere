// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

 /*
* ---------------------------------------------------------
 * Generated file, DO NOT EDIT
 * ----------------------------------------------------------------------------
 * See following wiki page for instructions on how to regenerate:
 * https://vsowiki.com/index.php?title=Rest_Client_Generation
 */
package com.microsoft.teamfoundation.sourcecontrol.webapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.visualstudio.services.webapi.model.ReferenceLinks;

/**
 * Encapsulates the reference metadata of a Git media object.
 * 
 */
public class GitMediaObjectRef
{

    /**
     * Gets or sets the reference links of the Git media object.
     */
    private ReferenceLinks _links;
    /**
     * Gets or sets the Git media object identifier. This Id property duplicates
     * the Oid property, but is required by the VSTS REST specification.
     */
    private String id;
    /**
     * Gets or sets the Git media object identifier. This property exists for
     * adherence to the GitHub Git Media contract.
     */
    private String oid;
    /**
     * Gets or sets the size of the Git media object in bytes. This property
     * exists for adherence to the GitHub Git Media contract.
     */
    private long size;
    /**
     * Gets or sets the URL for the Git media object.
     */
    private String url;

    /**
     * Gets or sets the reference links of the Git media object.
     */
    @JsonProperty("_links")
    public ReferenceLinks getLinks()
    {
        return _links;
    }

    /**
     * Gets or sets the reference links of the Git media object.
     */
    @JsonProperty("_links")
    public void setLinks(final ReferenceLinks _links)
    {
        this._links = _links;
    }

    /**
     * Gets or sets the Git media object identifier. This Id property duplicates
     * the Oid property, but is required by the VSTS REST specification.
     */
    public String getId()
    {
        return id;
    }

    /**
     * Gets or sets the Git media object identifier. This Id property duplicates
     * the Oid property, but is required by the VSTS REST specification.
     */
    public void setId(final String id)
    {
        this.id = id;
    }

    /**
     * Gets or sets the Git media object identifier. This property exists for
     * adherence to the GitHub Git Media contract.
     */
    public String getOid()
    {
        return oid;
    }

    /**
     * Gets or sets the Git media object identifier. This property exists for
     * adherence to the GitHub Git Media contract.
     */
    public void setOid(final String oid)
    {
        this.oid = oid;
    }

    /**
     * Gets or sets the size of the Git media object in bytes. This property
     * exists for adherence to the GitHub Git Media contract.
     */
    public long getSize()
    {
        return size;
    }

    /**
     * Gets or sets the size of the Git media object in bytes. This property
     * exists for adherence to the GitHub Git Media contract.
     */
    public void setSize(final long size)
    {
        this.size = size;
    }

    /**
     * Gets or sets the URL for the Git media object.
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * Gets or sets the URL for the Git media object.
     */
    public void setUrl(final String url)
    {
        this.url = url;
    }
}
