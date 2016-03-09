// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.product;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Provides full and short name information about TEE products.
 * <p>
 * This class is for internal use only.
 *
 * @see ProductInformation
 * @threadsafety thread-safe
 */
public class ProductName {
    /*
     * There are two levels of names in this class: one level for the entire
     * family of TEE products and one level for individual product applications
     * (plug-in for Eclipse, CLC, SDK).
     *
     * There are also usually two kinds of names: full (the long name marketing
     * wants to use) and short (more appropriate in dialog boxes, etc.).
     *
     * Ensure the SQM IDs match the values in the server so SQM data gets
     * processed correctly.
     */

    public static final ProductName PLUGIN = new ProductName(
        Messages.getString("ProductName.PluginFull"), //$NON-NLS-1$
        Messages.getString("ProductName.PluginFull", LocaleUtil.ROOT), //$NON-NLS-1$
        Messages.getString("ProductName.PluginShort"), //$NON-NLS-1$
        Messages.getString("ProductName.PluginShort", LocaleUtil.ROOT), //$NON-NLS-1$
        ClientSkuNumbers.Dev12TeamExplorerEveryWhere);

    public static final ProductName CLC = new ProductName(
        Messages.getString("ProductName.CLCFull"), //$NON-NLS-1$
        Messages.getString("ProductName.CLCFull", LocaleUtil.ROOT), //$NON-NLS-1$
        Messages.getString("ProductName.CLCShort"), //$NON-NLS-1$
        Messages.getString("ProductName.CLCShort", LocaleUtil.ROOT), //$NON-NLS-1$
        ClientSkuNumbers.Dev12CrossPlatformCommandline);

    public static final ProductName SDK = new ProductName(
        Messages.getString("ProductName.SDKFull"), //$NON-NLS-1$
        Messages.getString("ProductName.SDKFull", LocaleUtil.ROOT), //$NON-NLS-1$
        Messages.getString("ProductName.SDKShort"), //$NON-NLS-1$
        Messages.getString("ProductName.SDKShort", LocaleUtil.ROOT), //$NON-NLS-1$
        ClientSkuNumbers.Dev12SdkForJava);

    /*
     * Static family names.
     */

    private static final String FAMILY_FULL_NAME = Messages.getString("ProductName.FamilyFullName"); //$NON-NLS-1$
    private static final String FAMILY_FULL_NAME_NOLOC =
        Messages.getString("ProductName.FamilyFullName", LocaleUtil.ROOT); //$NON-NLS-1$

    private static final String FAMILY_SHORT_NAME = Messages.getString("ProductName.FamilyShortName"); //$NON-NLS-1$
    private static final String FAMILY_SHORT_NAME_NOLOC =
        Messages.getString("ProductName.FamilyShortName", LocaleUtil.ROOT); //$NON-NLS-1$

    /*
     * Instance fields.
     */

    private final String productFullName;
    private final String productShortName;
    private final String productFullNameNOLOC;
    private final String productShortNameNOLOC;
    private final int sqmID;

    private ProductName(
        final String productFullName,
        final String productFullNameNOLOC,
        final String productShortName,
        final String productShortNameNOLOC,
        final int sqmID) {
        this.productFullName = productFullName;
        this.productShortName = productShortName;
        this.productFullNameNOLOC = productFullNameNOLOC;
        this.productShortNameNOLOC = productShortNameNOLOC;
        this.sqmID = sqmID;
    }

    /**
     * @return the full application family name for the current locale, for
     *         example: "Team Explorer Everywhere"
     */
    public String getFamilyFullName() {
        return FAMILY_FULL_NAME;
    }

    /**
     * @return the full application family name in English, for example:
     *         "Team Explorer Everywhere"
     */
    public String getFamilyFullNameNOLOC() {
        return FAMILY_FULL_NAME_NOLOC;
    }

    /**
     * @return the short application family name for the current locale, for
     *         example: "Team Explorer Everywhere"
     */
    public String getFamilyShortName() {
        return FAMILY_SHORT_NAME;
    }

    /**
     * @return the short application family name in English, for example:
     *         "Team Explorer Everywhere"
     */
    public String getFamilyShortNameNOLOC() {
        return FAMILY_SHORT_NAME_NOLOC;
    }

    /**
     * @return the full product name for the current locale, for example:
     *         "Plugin for Eclipse", "Explorer", "Command Line Client"
     */
    public String getProductFullName() {
        return productFullName;
    }

    /**
     * @return the full product name in English, for example:
     *         "Plugin for Eclipse", "Explorer", "Command Line Client"
     */
    public String getProductFullNameNOLOC() {
        return productFullNameNOLOC;
    }

    /**
     * @return the short product name for the current locale, for example:
     *         "CLC", "Explorer", "Plugin"
     */
    public String getProductShortName() {
        return productShortName;
    }

    /**
     * @return the short product name in English, for example: "CLC",
     *         "Explorer", "Plugin"
     */
    public String getProductShortNameNOLOC() {
        return productShortNameNOLOC;
    }

    /**
     * For internal use only.
     *
     * @return the SQM product ID.
     */
    public int getSQMID() {
        return sqmID;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Same as {@link #getFamilyShortName()}.
     */
    @Override
    public String toString() {
        return getFamilyShortName();
    }
}