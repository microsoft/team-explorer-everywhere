// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.provision;

public class ProvisionValues {
    public static final String TYPES_NAMESPACE =
        "http://schemas.microsoft.com/VisualStudio/2008/workitemtracking/typedef"; //$NON-NLS-1$
    public static final String GLOBAL_LISTS_NAMESPACE =
        "http://schemas.microsoft.com/VisualStudio/2005/workitemtracking/globallists"; //$NON-NLS-1$
    public static final String CATEGORIES_NAMESPACE =
        "http://schemas.microsoft.com/VisualStudio/2008/workitemtracking/categories"; //$NON-NLS-1$

    public static final String APPLICATION = "Work item type editor"; //$NON-NLS-1$
    public static final String APP_VERSION = "1.0"; //$NON-NLS-1$

    // "from" values for copy/default rules
    public static final String SOURCE_VALUE = "value"; //$NON-NLS-1$
    public static final String SOURCE_FIELD = "field"; //$NON-NLS-1$
    public static final String SOURCE_CLOCK = "clock"; //$NON-NLS-1$
    public static final String SOURCE_CURRENT_USER = "currentuser"; //$NON-NLS-1$
    public static final String SOURCE_GUID = "guid"; //$NON-NLS-1$

    // Field types
    public static final String FIELD_TYPE_STRING = "String"; //$NON-NLS-1$
    public static final String FIELD_TYPE_INTEGER = "Integer"; //$NON-NLS-1$
    public static final String FIELD_TYPE_DATE_TIME = "DateTime"; //$NON-NLS-1$
    public static final String FIELD_TYPE_PLAIN_TEXT = "PlainText"; //$NON-NLS-1$
    public static final String FIELD_TYPE_HTML = "HTML"; //$NON-NLS-1$
    public static final String FIELD_TYPE_DOUBLE = "Double"; //$NON-NLS-1$
    public static final String FIELD_TYPE_TREE_PATH = "TreePath"; //$NON-NLS-1$
    public static final String FIELD_TYPE_HISTORY = "History"; //$NON-NLS-1$
    public static final String FIELD_TYPE_GUID = "GUID"; //$NON-NLS-1$
    public static final String FIELD_TYPE_BOOLEAN = "Boolean"; //$NON-NLS-1$

    // Reportability
    public static final String REPORTING_MEASURE = "measure"; //$NON-NLS-1$
    public static final String REPORTING_DIMENSION = "dimension"; //$NON-NLS-1$
    public static final String REPORTING_DETAIL = "detail"; //$NON-NLS-1$

    // Formula
    public static final String FORMULA_SUM = "sum"; //$NON-NLS-1$
    public static final String FORMULA_COUNT = "count"; //$NON-NLS-1$
    public static final String FORMULA_DISTINCT_COUNT = "distinctcount"; //$NON-NLS-1$
    public static final String FORMULA_AVG = "avg"; //$NON-NLS-1$
    public static final String FORMULA_MIN = "min"; //$NON-NLS-1$
    public static final String FORMULA_MAX = "max"; //$NON-NLS-1$

    public static final String EXCLUDE_GROUPS = "excludegroups"; //$NON-NLS-1$

    // Constant scope
    public static final String CONST_SCOPE_INSTANCE = "[team foundation]\\"; //$NON-NLS-1$
    public static final String CONST_SCOPE_GLOBAL = "[global]\\"; //$NON-NLS-1$
    public static final String CONST_SCOPE_PROJECT = "[project]\\"; //$NON-NLS-1$

    // Control types
    public static final String LINKS_CONTROL = "LinksControl"; //$NON-NLS-1$

    // Link Column Names
    public static final String COLUMN_TARGET_DESCRIPTION = "System.Links.Description"; //$NON-NLS-1$
    public static final String COLUMN_LINK_COMMENT = "System.Links.Comment"; //$NON-NLS-1$
    public static final String COLUMN_LINK_TYPE = "System.Links.LinkType"; //$NON-NLS-1$

    // Link Control filter types
    public static final String INCLUDE = "include"; //$NON-NLS-1$
    public static final String EXCLUDE = "exclude"; //$NON-NLS-1$
    public static final String INCLUDE_ALL = "includeAll"; //$NON-NLS-1$
    public static final String EXCLUDE_ALL = "excludeAll"; //$NON-NLS-1$
    public static final String FORWARD_NAME = "forwardname"; //$NON-NLS-1$
    public static final String REVERSE_NAME = "reversename"; //$NON-NLS-1$
    public static final String PROJECT = "project"; //$NON-NLS-1$
    public static final String ALL = "all"; //$NON-NLS-1$

    // Label control and web page control
    public static final String PARAM_TYPE_VALUE_ORIGINAL = "Original"; //$NON-NLS-1$
    public static final String PARAM_MACRO_PROCESS_GUIDANCE_URL = "@ProcessGuidance"; //$NON-NLS-1$
    public static final String PARAM_MACRO_PORTAL = "@PortalPage"; //$NON-NLS-1$
    public static final String PARAM_MACRO_REPORT_MANAGER_URL = "@ReportManagerUrl"; //$NON-NLS-1$
    public static final String PARAM_MACRO_REPORT_SERVICE_SITE_URL = "@ReportServiceSiteUrl"; //$NON-NLS-1$
    public static final String PARAM_MACRO_ME = "@Me"; //$NON-NLS-1$
    public static final String LABEL_CONTROL = "LabelControl"; //$NON-NLS-1$
    public static final String WEBPAGE_CONTROL = "WebpageControl"; //$NON-NLS-1$

}
