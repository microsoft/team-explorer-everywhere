// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.generic.menubutton.MenuButton;
import com.microsoft.tfs.client.common.ui.controls.generic.menubutton.MenuButtonFactory;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ServerItemPickerDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.WorkspaceItemSource;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.valid.MultiValidator;
import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.ValidatorHelper;

public class ItemAndVersionPicker extends BaseControl implements Validatable {
    private final Text itemText;
    private final MenuButton button;
    private final VersionPickerControl versionPickerControl;
    private final ValidatorHelper textValidator;
    private final MultiValidator validator;
    private boolean isDirectory;

    private TFSRepository repository;

    private String item;

    public ItemAndVersionPicker(final Composite parent, final int style) {
        super(parent, style);

        final GridLayout layout = SWTUtil.gridLayout(this, 2);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        itemText = new Text(this, SWT.BORDER);
        GridDataBuilder.newInstance().wHint(getMinimumMessageAreaWidth()).hGrab().hFill().applyTo(itemText);
        itemText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                ItemAndVersionPicker.this.modifyText(e);
            }
        });
        itemText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                ItemAndVersionPicker.this.textFocusGained(e);
            }
        });

        textValidator = new ValidatorHelper(itemText);

        button = MenuButtonFactory.getMenuButton(this, SWT.NONE);
        button.setText(Messages.getString("ItemAndVersionPicker.BrowseButtonText")); //$NON-NLS-1$

        versionPickerControl = new VersionPickerControl(
            this,
            SWT.NONE,
            Messages.getString("ItemAndVersionPicker.VersionPickerControlText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).applyTo(versionPickerControl);

        validator = new MultiValidator(this);
        validator.addValidator(textValidator);
        validator.addValidatable(versionPickerControl);

        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                ItemAndVersionPicker.this.mainButtonSelected(e);
            }
        });

        final IAction browseLocalAction = new Action() {
            @Override
            public void run() {
                browseLocal();
            }
        };
        browseLocalAction.setText(Messages.getString("ItemAndVersionPicker.LocalPathActionName")); //$NON-NLS-1$

        final IAction browseServerAction = new Action() {
            @Override
            public void run() {
                browseServer();
            }
        };
        browseServerAction.setText(Messages.getString("ItemAndVersionPicker.ServerPathActionName")); //$NON-NLS-1$

        button.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                manager.add(browseLocalAction);
                manager.add(browseServerAction);
            }
        });

        setRepository(null);
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

    public void setRepository(final TFSRepository repository) {
        this.repository = repository;

        versionPickerControl.setRepository(repository);

        if (repository == null) {
            itemText.setText(""); //$NON-NLS-1$
            itemText.setEnabled(false);
            button.setEnabled(false);
        } else {
            itemText.setText(""); //$NON-NLS-1$
            itemText.setEnabled(true);
            button.setEnabled(true);
        }
    }

    public void setIsDirectory(final boolean isDir) {
        isDirectory = isDir;
    }

    public void setItem(String newItem) {
        if (repository == null) {
            throw new IllegalStateException();
        }

        if (newItem == null) {
            newItem = ""; //$NON-NLS-1$
        }
        itemText.setText(newItem);
    }

    public String getItem() {
        return item;
    }

    public VersionSpec getVersionSpec() {
        return versionPickerControl.getVersionSpec();
    }

    private void modifyText(final ModifyEvent e) {
        item = ((Text) e.widget).getText().trim();
        if (item.length() == 0) {
            item = null;
        }

        if (item == null) {
            textValidator.setInvalid(Messages.getString("ItemAndVersionPicker.ValidationMessageInvalidItem")); //$NON-NLS-1$
        } else {
            textValidator.setValid();
        }

        if (item != null && ServerPath.isServerPath(item)) {
            versionPickerControl.setEnabled(true);
            versionPickerControl.setPath(item);
        } else {
            versionPickerControl.setEnabled(false);
        }
    }

    private void textFocusGained(final FocusEvent e) {
        final Text text = (Text) e.widget;
        final String s = text.getText();
        text.setSelection(new Point(0, s.length()));
    }

    private void mainButtonSelected(final SelectionEvent e) {
        if (item == null || !ServerPath.isServerPath(item)) {
            browseLocal();
        } else {
            browseServer();
        }
    }

    private void browseLocal() {
        final Dialog dialog;
        if (isDirectory) {
            dialog = new DirectoryDialog(getShell(), SWT.OPEN);
        } else {
            dialog = new FileDialog(getShell(), SWT.OPEN);
        }

        if (item != null && !ServerPath.isServerPath(item)) {
            if (isDirectory) {
                ((DirectoryDialog) dialog).setFilterPath(item);
            } else {
                ((FileDialog) dialog).setFileName(item);
            }
        }

        final String selectedFile = isDirectory ? ((DirectoryDialog) dialog).open() : ((FileDialog) dialog).open();

        if (selectedFile != null) {
            setItem(selectedFile);
        }
    }

    private void browseServer() {
        String initialPath = null;

        if (item != null && ServerPath.isServerPath(item)) {
            initialPath = item;
        }

        final ServerItemSource serverItemSource = new WorkspaceItemSource(repository.getWorkspace());

        final ServerItemPickerDialog dialog =
            new ServerItemPickerDialog(
                getShell(),
                Messages.getString("ItemAndVersionPicker.BrowseDialogTitle"), //$NON-NLS-1$
                initialPath,
                serverItemSource);

        if (IDialogConstants.OK_ID != dialog.open()) {
            return;
        }

        setItem(dialog.getSelectedServerPath());
    }
}
