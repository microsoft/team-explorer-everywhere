// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * <p>
 * Represents the version of the web services on the server. For the value
 * {@link #PRE_TFS_2010}, check for specific features using the
 * {@link VersionControlClient#getServerSupportedFeatures()} method.
 * </p>
 * <p>
 * This class can be compared/sorted by its integer values ({@link #getValue()}
 * ).
 * </p>
 *
 * @threadsafety immutable
 */
public class WebServiceLevel extends TypesafeEnum {
    /*
     * Keep the integer values ordered so compare/sort by getValue() works.
     */

    public static final WebServiceLevel UNKNOWN = new WebServiceLevel(0);
    public static final WebServiceLevel PRE_TFS_2010 = new WebServiceLevel(1);
    public static final WebServiceLevel TFS_2010 = new WebServiceLevel(2);
    public static final WebServiceLevel TFS_2012 = new WebServiceLevel(3);
    public static final WebServiceLevel TFS_2012_1 = new WebServiceLevel(4);
    public static final WebServiceLevel TFS_2012_2 = new WebServiceLevel(5);
    public static final WebServiceLevel TFS_2012_3 = new WebServiceLevel(6);
    public static final WebServiceLevel TFS_2012_QU1 = new WebServiceLevel(7);
    public static final WebServiceLevel TFS_2012_QU1_1 = new WebServiceLevel(8);

    private WebServiceLevel(final int value) {
        super(value);
    }
}
