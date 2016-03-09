// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.build.ui;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.UIPlugin;

import com.microsoft.tfs.checkinpolicies.build.Messages;
import com.microsoft.tfs.checkinpolicies.build.TFSBuildCheckinPolicyPlugin;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;

public class MarkerBrowseDialog extends BaseDialog {
    private String marker = ""; //$NON-NLS-1$

    private Text inputText;
    private List markerList;

    public MarkerBrowseDialog(final Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        SWTUtil.gridLayout(dialogArea);

        final Label inputLabel =
            SWTUtil.createLabel(dialogArea, SWT.WRAP, Messages.getString("MarkerBrowseDialog.InputLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(inputLabel);

        inputText = new Text(dialogArea, SWT.BORDER);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(inputText);
        inputText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                marker = inputText.getText().trim();
            }
        });

        final Label browseLabel =
            SWTUtil.createLabel(dialogArea, SWT.WRAP, Messages.getString("MarkerBrowseDialog.BrowseLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(browseLabel);

        markerList = new List(dialogArea, SWT.SINGLE | SWT.BORDER);
        GridDataBuilder.newInstance().fill().grab().applyTo(markerList);
        markerList.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                final String[] selectedStrings = markerList.getSelection();

                if (selectedStrings.length > 0) {
                    inputText.setText(selectedStrings[0].trim());
                }
            }
        });

        populateMarkers();

        ControlSize.setCharHeightHint(markerList, 10);
    }

    private void populateMarkers() {
        try {
            final IMarker[] markers =
                ResourcesPlugin.getWorkspace().getRoot().findMarkers(null, true, IResource.DEPTH_INFINITE);

            final Set markerTypes = new HashSet();

            for (int i = 0; i < markers.length; i++) {
                if (markers[i].getType() != null) {
                    markerTypes.add(markers[i].getType().trim());
                }
            }

            for (final Iterator iterator = markerTypes.iterator(); iterator.hasNext();) {
                final String type = (String) iterator.next();
                markerList.add(type);
            }
        } catch (final CoreException e) {
            UIPlugin.getDefault().getLog().log(
                new Status(
                    Status.ERROR,
                    TFSBuildCheckinPolicyPlugin.PLUGIN_ID,
                    0,
                    Messages.getString("MarkerBrowseDialog.ExceptionFindingMarkers"), //$NON-NLS-1$
                    e));
        }

    }

    @Override
    protected Point defaultComputeInitialSize() {
        return new Point(400, super.defaultComputeInitialSize().y);
    }

    /**
     * @return the marker string the user typed or selected.
     */
    public String getMarker() {
        return marker;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("MarkerBrowseDialog.DialogTitle"); //$NON-NLS-1$
    }
}
