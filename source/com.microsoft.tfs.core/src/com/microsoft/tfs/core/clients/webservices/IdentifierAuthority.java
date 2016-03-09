// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

public abstract class IdentifierAuthority {
    /*
     * Values copied from .NET implementation of IdentifierAuthority enum.
     */

    public static final long NULL = 0;
    public static final long WORLD = 1;
    public static final long LOCAL = 2;
    public static final long CREATOR = 3;
    public static final long NON_UNIQUE = 4;
    public static final long NT = 5;
    public static final long SITE_SERVER = 6;
    public static final long INTERNET_SITE = 7;
    public static final long EXCHANGE = 8;
    public static final long RESOURCE_MANAGER = 9;
}