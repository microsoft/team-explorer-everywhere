// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertypages;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.dialogs.vc.SetEncodingDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.tasks.vc.ChangeUnixExecutablePropertyTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.CheckoutTask;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

public abstract class BaseGeneralPropertyPage extends PropertyPage {
    /*
     * Max value width in characters before wrapping
     */
    private static final int MAX_VALUE_WIDTH = 80;

    /* GeneralPropertyRowId to value text map */
    private final static Map idToValueMap = new HashMap();

    private TFSRepository repository = null;

    /*
     * Initial path and encoding value, plus new encoding value (if any).
     */
    private String filePath = null;
    private FileEncoding currentEncoding = null;
    private Button encodingButton = null;

    private FileEncoding newEncoding = null;

    /*
     * Initial and current Unix executable property state.
     */
    private Button executableButton = null;
    private boolean currentExecutable;
    private boolean newExecutable;

    public BaseGeneralPropertyPage() {
    }

    @Override
    protected final Control createContents(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);

        final GridLayout layout = new GridLayout(2, false);
        composite.setLayout(layout);
        layout.verticalSpacing = 15;

        doCreateContents(composite);

        return composite;
    }

    protected final String encodingToString(final int encoding) {
        final FileEncoding fileEncoding = new FileEncoding(encoding);

        return fileEncoding.getName();
    }

    protected final void addRow(
        final Composite parent,
        final GeneralPropertyRowID id,
        final String label,
        String value) {
        final int textWidthHint = convertWidthInCharsToPixels(MAX_VALUE_WIDTH);

        final Label labelWidget = new Label(parent, SWT.NONE);
        GridDataBuilder.newInstance().vAlignTop().hAlignPrompt().applyTo(labelWidget);
        labelWidget.setText(label);

        final Text text = new Text(parent, SWT.WRAP | SWT.READ_ONLY);
        GridDataBuilder.newInstance().hGrab().hFill().wHint(textWidthHint).applyTo(text);
        text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        if (value == null) {
            value = ""; //$NON-NLS-1$
        }
        text.setText(value);

        /*
         * Hack: add the encoding button and executable checkbox here.
         */
        if (id == GeneralPropertyRowID.ENCODING) {
            // Encoding
            final Label fillerLabel = new Label(parent, SWT.NONE);
            fillerLabel.setText(""); //$NON-NLS-1$

            encodingButton = new Button(parent, SWT.PUSH);
            encodingButton.setText(Messages.getString("BaseGeneralPropertyPage.SetEncodingButtonText")); //$NON-NLS-1$
            encodingButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    setEncoding();
                }
            });
            encodingButton.setEnabled(repository != null && filePath != null && currentEncoding != null);

            // Executable

            executableButton = new Button(parent, SWT.CHECK);
            executableButton.setText(Messages.getString("BaseGeneralPropertyPage.ExecutableCheckboxText")); //$NON-NLS-1$
            executableButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    newExecutable = executableButton.getSelection();
                }
            });

            // Disable by default; setItem() will re-enable for files
            configureExecutableButton(ItemType.ANY, false);
        }

        idToValueMap.put(id, text);
    }

    /**
     * Set the enabled state, checked state, and tooltip text on the
     * "executable" check box according to the version of the service we're
     * connected to and the type of item we're viewing properties on.
     *
     * @param itemType
     *        the item type, which may be {@link ItemType#ANY} (must not be
     *        <code>null</code>)
     * @param <code>true</code>
     *        if the item already has the executable property set on it,
     *        <code>false</code> if it does not
     */
    private void configureExecutableButton(final ItemType itemType, final boolean initialExecutable) {
        /*
         * If you make changes to this method, see the duplicate in
         * GeneralPropertiesTab (for SCE).
         */

        Check.notNull(itemType, "itemType"); //$NON-NLS-1$

        if (itemType != ItemType.FILE
            || repository == null
            || repository.getVersionControlClient().getServiceLevel().getValue() < WebServiceLevel.TFS_2012.getValue()
            || !repository.getWorkspace().isLocalPathMapped(filePath)
            || FileSystemUtils.getInstance().getAttributes(filePath).isSymbolicLink()) {
            executableButton.setEnabled(false);
            executableButton.setSelection(initialExecutable);
            executableButton.setToolTipText(null);
        } else {
            executableButton.setEnabled(true);
            executableButton.setSelection(initialExecutable);
            executableButton.setToolTipText(Messages.getString("BaseGeneralPropertyPage.ExecuteBitCheckboxTooltip")); //$NON-NLS-1$
        }
    }

    protected abstract void doCreateContents(Composite parent);

    /**
     * Only call for file items (not directories).
     * <p>
     * Always call this <b>after</b> adding the "encoding" row (if it will be
     * added at all).
     */
    protected final void setItem(
        final TFSRepository repository,
        final String path,
        final FileEncoding initialEncoding,
        final boolean initialExecutable) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(initialEncoding, "initialEncoding"); //$NON-NLS-1$

        this.repository = repository;
        filePath = path;
        currentEncoding = initialEncoding;
        currentExecutable = initialExecutable;
        newExecutable = initialExecutable;

        if (encodingButton != null) {
            encodingButton.setEnabled(true);
        }

        if (encodingButton != null) {
            configureExecutableButton(ItemType.FILE, initialExecutable);
        }
    }

    private final void setEncoding() {
        Check.notNull(filePath, "filePath"); //$NON-NLS-1$
        Check.notNull(currentEncoding, "initialEncoding"); //$NON-NLS-1$

        final FileEncoding displayEncoding = newEncoding != null ? newEncoding : currentEncoding;
        final SetEncodingDialog encodingDialog = new SetEncodingDialog(getShell(), filePath, displayEncoding);

        if (encodingDialog.open() != IDialogConstants.OK_ID
            || encodingDialog.getFileEncoding().getCodePage() == displayEncoding.getCodePage()) {
            return;
        }

        /*
         * Don't pend the encoding change here, just set newEncoding, which will
         * be applied when the user hits OK or Apply.
         */
        newEncoding = encodingDialog.getFileEncoding();

        final Text encodingText = (Text) idToValueMap.get(GeneralPropertyRowID.ENCODING);

        if (encodingText != null) {
            encodingText.setText(encodingToString(newEncoding.getCodePage()));
        }
    }

    /*
     * Reset encoding value to the initial value.
     */
    @Override
    protected void performDefaults() {
        if (currentEncoding != null) {
            newEncoding = null;

            final Text encodingText = (Text) idToValueMap.get(GeneralPropertyRowID.ENCODING);

            if (encodingText != null) {
                encodingText.setText(encodingToString(currentEncoding.getCodePage()));
            }
        }

        if (executableButton != null) {
            newEncoding = currentEncoding;
            executableButton.setSelection(currentExecutable);
        }
    }

    @Override
    protected void performApply() {
        if (newEncoding != null && pendEncodingChange()) {
            currentEncoding = newEncoding;
            newEncoding = null;
        }

        if (newExecutable != currentExecutable && pendExecutableChange()) {
            currentExecutable = newExecutable;
        }
    }

    @Override
    public boolean performOk() {
        boolean failure = false;

        if (newEncoding != null) {
            if (pendEncodingChange()) {
                currentEncoding = newEncoding;
                newEncoding = null;
            } else {
                failure = true;
            }
        }

        if (newExecutable != currentExecutable) {
            if (pendExecutableChange()) {
                currentExecutable = newExecutable;
            } else {
                failure = true;
            }
        }

        return !failure;
    }

    private boolean pendEncodingChange() {
        final CheckoutTask checkoutTask = new CheckoutTask(getShell(), repository, new ItemSpec[] {
            new ItemSpec(filePath, RecursionType.NONE)
        }, LockLevel.UNCHANGED, newEncoding);

        final IStatus checkoutStatus = checkoutTask.run();

        if (checkoutStatus.getSeverity() == IStatus.ERROR) {
            return false;
        }

        return true;
    }

    private boolean pendExecutableChange() {
        final ChangeUnixExecutablePropertyTask task =
            new ChangeUnixExecutablePropertyTask(getShell(), repository, new String[] {
                filePath
        }, newExecutable, LockLevel.UNCHANGED, PendChangesOptions.NONE);

        return task.run().isOK();
    }

    protected boolean containsExecutableProperty(final PropertyValue[] propertyValues) {
        return PropertyConstants.EXECUTABLE_ENABLED_VALUE.equals(
            PropertyUtils.selectMatching(propertyValues, PropertyConstants.EXECUTABLE_KEY));
    }

    public static class GeneralPropertyRowID extends TypesafeEnum {
        public static final GeneralPropertyRowID NONE = new GeneralPropertyRowID(0);
        public static final GeneralPropertyRowID NAME_SERVER = new GeneralPropertyRowID(1);
        public static final GeneralPropertyRowID NAME_LOCAL = new GeneralPropertyRowID(2);
        public static final GeneralPropertyRowID VERSION_LATEST = new GeneralPropertyRowID(3);
        public static final GeneralPropertyRowID VERSION_LOCAL = new GeneralPropertyRowID(4);
        public static final GeneralPropertyRowID VERSION_WORKSPACE = new GeneralPropertyRowID(5);
        public static final GeneralPropertyRowID VERSION_SHELVESET = new GeneralPropertyRowID(6);
        public static final GeneralPropertyRowID VERSION_CHANGESET = new GeneralPropertyRowID(7);
        public static final GeneralPropertyRowID ENCODING = new GeneralPropertyRowID(8);

        private GeneralPropertyRowID(final int value) {
            super(value);
        }
    }
}
