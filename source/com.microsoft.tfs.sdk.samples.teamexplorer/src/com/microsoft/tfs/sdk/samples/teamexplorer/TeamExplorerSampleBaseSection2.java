// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.teamexplorer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.sections.ITeamExplorerSection;
import com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerSectionRegenerateListener;

public class TeamExplorerSampleBaseSection2 implements ITeamExplorerSection {
    private String id;
    private String title;

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return true;
    }

    @Override
    public boolean initializeInBackground(final TeamExplorerContext context) {
        return false;
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context, final Object state) {
    }

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);

        /*
         * FORM TOOLKIT STYLE NOTE
         *
         * When the windowing system is GTK, Text controls inside composites
         * created by FormToolkit do not draw their own borders by default (that
         * is, when the Text's style bits don't include SWT.BORDER).
         *
         * To get the thin "flat" borders that are conventional in Eclipse
         * forms, we must call FormToolkit.paintBordersFor() on each composite
         * we create that has children which need these borders. The borders are
         * most commonly desired on Text controls, but they might be desired on
         * controls like Tree or Table.
         *
         * On Windows, Text controls draw their own borders without this extra
         * step. However, enabling the painter is harmless (the controls look
         * the same when it is enabled), and it should always be enabled for
         * portability.
         *
         * SWT.SEARCH STYLE NOTE
         *
         * If you enable the SWT.SEARCH style on a Text when the painter is
         * enabled, on Linux you'll see two borders: one the Text draws itself
         * (SWT.SEARCH enables SWT.BORDER), and one from the FormToolkit's
         * painter.
         *
         * To avoid double borders, you can unset SWT.SEARCH if you don't need
         * the search icon display behavior (for instance, you only want the
         * watermark message behavior which works fine without SWT.SEARCH).
         * Alternatively, you can leave SWT.SEARCH enabled but prevent the
         * painter from drawing the flat border for that control with:
         *
         * widget.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
         *
         * But then the border you see may not match the "flat" border other
         * Text controls get (this is more pronounced on GTK, less on Windows).
         * See the Javadoc for FormToolkit.paintBordersFor() for details.
         */
        toolkit.paintBordersFor(composite);

        /*
         * When paintBordersFor() is enabled, there must be at least 1 pixel of
         * margin in the horizontal and vertical directions for the thin border
         * (or it won't be visible).
         */
        SWTUtil.gridLayout(composite, 1, false, 1, 5);

        final Button button = toolkit.createButton(composite, "Display TFS Collection URL", SWT.NONE);//$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(button);

        final Text text = toolkit.createText(composite, ""); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(text);

        final String collectionURL = context.getServer().getConnection().getBaseURI().toString();
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {

                text.setText(collectionURL);
                text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_YELLOW));

            }
        });

        return composite;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public void setID(final String id) {
        this.id = id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public void addSectionRegenerateListener(final TeamExplorerSectionRegenerateListener listener) {
    }

    @Override
    public void removeSectionRegenerateListener(final TeamExplorerSectionRegenerateListener listener) {
    }

    @Override
    public Object saveState() {
        return null;
    }
}
