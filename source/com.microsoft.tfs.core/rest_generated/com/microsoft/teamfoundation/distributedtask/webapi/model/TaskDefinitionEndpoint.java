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

package com.microsoft.teamfoundation.distributedtask.webapi.model;


/** 
 */
public class TaskDefinitionEndpoint {

    /**
    * An ID that identifies a service connection to be used for authenticating endpoint requests.
    */
    private String connectionId;
    /**
    * The scope as understood by Connected Services. Essentialy, a project-id for now.
    */
    private String scope;
    /**
    * An XPath/Json based selector to filter response returned by fetching the endpoint Url. An XPath based selector must be prefixed with the string "xpath:". A Json based selector must be prefixed with "json:".  The following selector defines an XPath for extracting nodes named 'ServiceName'.  endpoint.Selector = "xpath://ServiceName";
    */
    private String selector;
    /**
    * TaskId that this endpoint belongs to.
    */
    private String taskId;
    /**
    * URL to GET.
    */
    private String url;

    /**
    * An ID that identifies a service connection to be used for authenticating endpoint requests.
    */
    public String getConnectionId() {
        return connectionId;
    }

    /**
    * An ID that identifies a service connection to be used for authenticating endpoint requests.
    */
    public void setConnectionId(final String connectionId) {
        this.connectionId = connectionId;
    }

    /**
    * The scope as understood by Connected Services. Essentialy, a project-id for now.
    */
    public String getScope() {
        return scope;
    }

    /**
    * The scope as understood by Connected Services. Essentialy, a project-id for now.
    */
    public void setScope(final String scope) {
        this.scope = scope;
    }

    /**
    * An XPath/Json based selector to filter response returned by fetching the endpoint Url. An XPath based selector must be prefixed with the string &quot;xpath:&quot;. A Json based selector must be prefixed with &quot;json:&quot;.  The following selector defines an XPath for extracting nodes named 'ServiceName'.  endpoint.Selector = &quot;xpath://ServiceName&quot;;
    */
    public String getSelector() {
        return selector;
    }

    /**
    * An XPath/Json based selector to filter response returned by fetching the endpoint Url. An XPath based selector must be prefixed with the string &quot;xpath:&quot;. A Json based selector must be prefixed with &quot;json:&quot;.  The following selector defines an XPath for extracting nodes named 'ServiceName'.  endpoint.Selector = &quot;xpath://ServiceName&quot;;
    */
    public void setSelector(final String selector) {
        this.selector = selector;
    }

    /**
    * TaskId that this endpoint belongs to.
    */
    public String getTaskId() {
        return taskId;
    }

    /**
    * TaskId that this endpoint belongs to.
    */
    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }

    /**
    * URL to GET.
    */
    public String getUrl() {
        return url;
    }

    /**
    * URL to GET.
    */
    public void setUrl(final String url) {
        this.url = url;
    }
}
