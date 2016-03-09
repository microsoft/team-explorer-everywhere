// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.ui;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.SupportContact;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.SupportContactCategory;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.SupportProvider;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.launcher.Launcher;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;

public class SupportControl extends BaseControl {
    public SupportControl(final Composite parent, final int style, final SupportProvider supportProvider) {
        super(parent, style);

        final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
            }
        });

        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing() * 3;
        setLayout(layout);

        if (supportProvider.getDialogImageLeft() != null && supportProvider.getDialogImageRight() != null) {
            final Composite imageContainer = new Composite(this, SWT.NONE);
            imageContainer.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
            GridDataBuilder.newInstance().hGrab().hFill().hAlignFill().vAlignTop().applyTo(imageContainer);

            /*
             * Feature in Mac OS: it appears that SWT uses alpha trickery to
             * shade the background of group and tab controls to attempt to
             * match the native handling. (Subnote: it fails to do so.) So
             * instead of just setting a background (which would get alpha'd to
             * the wrong color), we need to explicitly paint it.
             */
            if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
                imageContainer.addPaintListener(new PaintListener() {
                    @Override
                    public void paintControl(final PaintEvent e) {
                        e.gc.fillRectangle(imageContainer.getBounds());
                    }
                });
            }

            final GridLayout imageContainerLayout = new GridLayout(2, false);
            imageContainerLayout.marginWidth = 0;
            imageContainerLayout.marginHeight = 0;
            imageContainerLayout.horizontalSpacing = 0;
            imageContainerLayout.verticalSpacing = 0;
            imageContainer.setLayout(imageContainerLayout);

            final Image imageLeft = imageHelper.getImage(supportProvider.getDialogImageLeft());
            final Image imageRight = imageHelper.getImage(supportProvider.getDialogImageRight());

            final Label imageLabelLeft = new Label(imageContainer, SWT.NONE);
            imageLabelLeft.setImage(imageLeft);
            imageLabelLeft.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

            final Label imageLabelRight = new Label(imageContainer, SWT.NONE);
            imageLabelRight.setImage(imageRight);
            imageLabelRight.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        } else if (supportProvider.getDialogImage() != null) {
            final Image image = imageHelper.getImage(supportProvider.getDialogImage());
            final Label imageLabel = new Label(this, SWT.NONE);
            imageLabel.setImage(image);
            imageLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
        }

        final Label dialogLabel = new Label(this, SWT.WRAP);
        dialogLabel.setText(supportProvider.getDialogText());
        dialogLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1));

        final SupportContactCategory[] categories = supportProvider.getSortedContactCategories();
        for (int i = 0; i < categories.length; i++) {
            final SupportContact[] contacts = supportProvider.getSortedContactsForCategory(categories[i]);
            final Composite composite = makeCompositeForSupportContacts(this, categories[i], contacts);

            composite.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1));
        }
    }

    private Composite makeCompositeForSupportContacts(
        final Composite parent,
        final SupportContactCategory category,
        final SupportContact[] contacts) {
        final Group composite = new Group(parent, SWT.NONE);
        composite.setText(category.getLabel());

        final GridLayout layout = new GridLayout(3, false);
        composite.setLayout(layout);

        for (int i = 0; i < contacts.length; i++) {
            final String messageFormat = Messages.getString("SupportControl.ContactLabelTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, contacts[i].getLabel());

            final Label label = new Label(composite, SWT.NONE);
            label.setText(message);

            /*
             * Don't use a label here. The Text control allows copy-and-paste of
             * the contents.
             */
            final Text text = new Text(composite, SWT.BORDER);
            text.setEditable(false);
            text.setText(contacts[i].getValue());
            final int textSpan = (contacts[i].isLaunchable() ? 1 : 2);
            text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, textSpan, 1));

            if (contacts[i].isLaunchable()) {
                final String url = contacts[i].getURL();
                final Button button = new Button(composite, SWT.NONE);
                button.setText(Messages.getString("SupportControl.LaunchButtonText")); //$NON-NLS-1$
                button.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                        Launcher.launch(url);
                    }
                });
            }

            /*
             * description goes here
             */

            if (i < contacts.length - 1) {
                final Label separatorLabel = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
                separatorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, layout.numColumns, 1));
            }
        }

        return composite;
    }
}
