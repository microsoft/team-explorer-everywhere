// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs;

import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._LabelItemSpec;

/**
 * Describes a version control object (by path) at a specific version.
 *
 * @since TEE-SDK-10.1
 */
public final class LabelItemSpec extends WebServiceObjectWrapper {
    public LabelItemSpec() {
        super(new _LabelItemSpec());
    }

    public LabelItemSpec(final _LabelItemSpec spec) {
        super(spec);
    }

    /**
     * Constructs an instance with the following parameters.
     *
     * @param item
     *        the item to label.
     * @param version
     *        the version of the item to which the label will apply, which may
     *        be null if exclude is true.
     * @param exclude
     *        true if this item is to be excluded from the label, false if it is
     *        to be included in the label.
     */
    public LabelItemSpec(final ItemSpec item, final VersionSpec version, final boolean exclude) {
        super(
            new _LabelItemSpec(
                exclude,
                item.getWebServiceObject(),
                version == null ? null : version.getWebServiceObject()));

        Check.isTrue(exclude == true || version != null, "exclude == true || version != null"); //$NON-NLS-1$
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _LabelItemSpec getWebServiceObject() {
        return (_LabelItemSpec) webServiceObject;
    }

    public ItemSpec getItemSpec() {
        return new ItemSpec(getWebServiceObject().getItemSpec());
    }

    public void setItemSpec(final ItemSpec item) {
        getWebServiceObject().setItemSpec(item.getWebServiceObject());
    }

    public VersionSpec getVersion() {
        return VersionSpec.fromWebServiceObject(getWebServiceObject().getVersion());
    }

    public void setVersion(final VersionSpec version) {
        getWebServiceObject().setVersion(version.getWebServiceObject());
    }

    public boolean isExclude() {
        return getWebServiceObject().isEx();
    }

    public void setExclude(final boolean exclude) {
        getWebServiceObject().setEx(exclude);
    }
}
