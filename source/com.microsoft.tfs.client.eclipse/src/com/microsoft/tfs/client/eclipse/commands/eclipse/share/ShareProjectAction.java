// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.commands.eclipse.share;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * The type of sharing we're doing - whether some projects need to be merely
 * connected (eg, are already mapped) and some need to be uploaded, or whether
 * we merely connect all projects, or upload all projects.
 */
public final class ShareProjectAction extends TypesafeEnum {
    public static final ShareProjectAction CONNECT = new ShareProjectAction(0);
    public static final ShareProjectAction UPLOAD = new ShareProjectAction(1);
    public static final ShareProjectAction MAP_AND_UPLOAD = new ShareProjectAction(2);

    private ShareProjectAction(final int type) {
        super(type);
    }
}
