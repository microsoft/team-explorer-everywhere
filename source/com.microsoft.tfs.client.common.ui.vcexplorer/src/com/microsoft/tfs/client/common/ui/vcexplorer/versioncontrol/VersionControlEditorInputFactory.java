// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class VersionControlEditorInputFactory implements IElementFactory {

    @Override
    public IAdaptable createElement(final IMemento memento) {
        final IMemento folderControlMemento = memento.getChild("folder-control"); //$NON-NLS-1$
        if (folderControlMemento == null) {
            return new VersionControlEditorInput();
        }

        return new VersionControlEditorInput(memento);
    }

}
