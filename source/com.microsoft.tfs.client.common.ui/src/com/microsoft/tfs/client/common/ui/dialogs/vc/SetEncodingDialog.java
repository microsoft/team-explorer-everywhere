// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.FileEncodingDetector;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.CollatorFactory;

public class SetEncodingDialog extends BaseDialog {
    private static final FileEncoding[] suggestedEncodings = new FileEncoding[] {
        FileEncoding.BINARY,
        FileEncoding.UTF_8,
        FileEncoding.UTF_16
    };

    private FileEncoding[] encodings;

    private final String localPath;
    private final FileEncoding originalEncoding;
    private FileEncoding encoding;

    private Combo encodingCombo;
    private Button detectButton;

    public SetEncodingDialog(final Shell parent, final String localPath, final FileEncoding encoding) {
        super(parent);

        Check.notNull(localPath, "localPath"); //$NON-NLS-1$

        this.localPath = localPath;
        originalEncoding = encoding;
    }

    private String getFileName() {
        String fileName;

        if (ServerPath.isServerPath(localPath)) {
            fileName = ServerPath.getFileName(localPath);
        } else {
            fileName = LocalPath.getFileName(localPath);
        }

        return fileName;
    }

    @Override
    protected String provideDialogTitle() {
        final String messageFormat = Messages.getString("SetEncodingDialog.DialogTitleFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, getFileName());
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final String messageFormat = Messages.getString("SetEncodingDialog.ExplainLabelTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getFileName());

        final Label explanationLabel = new Label(dialogArea, SWT.NONE);
        explanationLabel.setText(message);
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(3).applyTo(explanationLabel);

        final Label promptLabel = new Label(dialogArea, SWT.NONE);
        promptLabel.setText(Messages.getString("SetEncodingDialog.PromptLabelText")); //$NON-NLS-1$

        encodingCombo = new Combo(dialogArea, SWT.DROP_DOWN | SWT.READ_ONLY);
        encodingCombo.setItems(getCharsetList());
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(encodingCombo);

        detectButton = new Button(dialogArea, SWT.NONE);
        detectButton.setText(Messages.getString("SetEncodingDialog.DetectButtonText")); //$NON-NLS-1$
        detectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                try {
                    final FileEncoding encoding =
                        FileEncodingDetector.detectEncoding(localPath, FileEncoding.AUTOMATICALLY_DETECT);
                    encodingCombo.select(getEncodingIndex(encoding));
                } catch (final Exception ex) {
                    MessageDialog.openError(
                        getShell(),
                        Messages.getString("SetEncodingDialog.ErrorDialogTitle"), //$NON-NLS-1$
                        ex.getLocalizedMessage());
                }
            }
        });

        if (originalEncoding != null) {
            encodingCombo.select(getEncodingIndex(originalEncoding));
        } else {
            encodingCombo.select(getEncodingIndex(FileEncoding.getDefaultTextEncoding()));
        }
    }

    @Override
    protected void hookDialogAboutToClose() {
        final int idx = encodingCombo.getSelectionIndex();

        if (idx >= 0 && encodings.length > 0 && idx < encodings.length) {
            encoding = encodings[idx];

            /*
             * If the user selected the dashed separator line, then we treat
             * this as a cancellation.
             */
            if (encoding == null) {
                setReturnCode(IDialogConstants.CANCEL_ID);
            }
        }
    }

    public FileEncoding getFileEncoding() {
        return encoding;
    }

    @Override
    protected void hookAfterButtonsCreated() {
        ButtonHelper.setButtonToButtonBarSize(detectButton);
    }

    private String[] getCharsetList() {
        /* List of encodings in display order (including null separator) */
        final List<FileEncoding> encodingList = new ArrayList<FileEncoding>();

        encodingList.addAll(Arrays.asList(suggestedEncodings));

        /* Add the default system encoding */
        final FileEncoding defaultEncoding = FileEncoding.getDefaultTextEncoding();
        if (!encodingList.contains(defaultEncoding)) {
            encodingList.add(defaultEncoding);
        }

        /* Null (separator) */
        encodingList.add(null);

        /* Encodings from the codepages.xml / system properties */
        final List<FileEncoding> otherEncodings = new ArrayList<FileEncoding>();

        final int[] codePages = CodePageMapping.getCodePages();
        for (int i = 0; i < codePages.length; i++) {
            otherEncodings.add(new FileEncoding(codePages[i]));
        }

        /* Make sure the original encoding is here */
        if (originalEncoding != null) {
            otherEncodings.add(originalEncoding);
        }

        Collections.sort(otherEncodings, new FileEncodingComparator());

        for (final Iterator<FileEncoding> i = otherEncodings.iterator(); i.hasNext();) {
            final FileEncoding encoding = i.next();

            if (!encodingList.contains(encoding)) {
                encodingList.add(encoding);
            }
        }

        encodings = encodingList.toArray(new FileEncoding[encodingList.size()]);

        /*
         * Setup charset names
         */
        final String[] charsetNames = new String[encodings.length];

        for (int i = 0; i < encodings.length; i++) {
            if (encodings[i] == null) {
                charsetNames[i] = "--------------------"; //$NON-NLS-1$
            } else if (encodings[i].equals(defaultEncoding)) {
                charsetNames[i] =
                    encodings[i].getName() + Messages.getString("SetEncodingDialog.DefaultEncodingIndicator"); //$NON-NLS-1$
            } else {
                charsetNames[i] = encodings[i].getName();
            }
        }

        return charsetNames;
    }

    private int getEncodingIndex(final FileEncoding encoding) {
        Check.notNull(encoding, "encoding"); //$NON-NLS-1$

        for (int i = 0; i < encodings.length; i++) {
            if (encoding.equals(encodings[i])) {
                return i;
            }
        }

        return 0;
    }

    private class FileEncodingComparator implements Comparator<FileEncoding> {
        private final Collator caseInsensitiveCollator = CollatorFactory.getCaseInsensitiveCollator();

        @Override
        public int compare(final FileEncoding encoding0, final FileEncoding encoding1) {
            return caseInsensitiveCollator.compare(encoding0.getName(), encoding1.getName());
        }
    }
}
