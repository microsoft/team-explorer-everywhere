// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.extend;

import org.eclipse.swt.widgets.Shell;

/**
 * Note: right now the concept of DataProviderActions is very weak. It is an
 * area to enhance in the future.
 *
 * In particular, DataProviderActions are only consulted when we show
 * TabularData in the UI (in the DataProviderOutputControl), and then the
 * actions are only enabled when a single item is selected.
 *
 * In the future, this entire method should probably go away and be replaced
 * with declarative Eclipse actions. This is basically a hack for now due to
 * limited time.
 */
public interface DataProviderAction {
    public void run(Shell shell, Object data);
}
