// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EventListener;
import java.util.EventObject;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.generic.DatepickerCombo;
import com.microsoft.tfs.client.common.ui.dialogs.vc.FindChangesetDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelSpecParseException;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpecParseException;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.DateVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LabelVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;
import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.ValidatorHelper;

/**
 * <p>
 * {@link VersionPickerControl} is a UI control that allows the user to select a
 * version, expressed as an {@link VersionSpec}. The control is a horizontal
 * control that has a {@link Combo} to select the version type, and a data area
 * to enter version-specific data like a changeset number.
 * </p>
 *
 * <p>
 * To use a {@link VersionPickerControl}:
 * <ul>
 * <li>Create one, passing a parent {@link Composite}.</li>
 * <li>Set a {@link TFSRepository} for the control to use (
 * {@link #setRepository(TFSRepository)}).</li>
 * <li>If you want to control the initial version type that is shown in the
 * control, call {@link #setVersionType(VersionDescription)}.</li>
 * <li>Alternatively, to pre-set the control with a version spec, call
 * {@link #setVersionSpec(VersionSpec)}.</li>
 * <li>Optionally, you can set a "path" for the control by calling
 * {@link #setPath(String)}. The path is used when choosing certain version
 * types and should be derived from context. For instance, if the version picker
 * is being shown next to a selected server item, the path could be the path of
 * the item.</li>
 * <li>{@link VersionPickerControl} implements the {@link Validatable}
 * interface, and validates that a complete version spec has been entered.</li>
 * <li>To get the entered version spec, call {@link #getVersionSpec()}.
 * {@link #getVersionType()} returns the type that has been selected.</li>
 * <li>To get notifications when the version spec has changed, call
 * {@link #addVersionSpecChangedListener(VersionSpecChangedListener)}.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The .NET equivalent of this control is <code>ControlChooseVersion</code>.
 * </p>
 */
public class VersionPickerControl extends BaseControl implements Validatable {
    /**
     * An enumerated type that defines the kinds of versions that a
     * {@link VersionPickerControl} can generate.
     */
    public static class VersionDescription {
        /**
         * A {@link VersionDescription} that represents an
         * {@link ChangesetVersionSpec} .
         */
        public static final VersionDescription CHANGESET =
            new VersionDescription(Messages.getString("VersionPickerControl.VersionDescriptionChangeset")); //$NON-NLS-1$

        /**
         * A {@link VersionDescription} that represents an
         * {@link DateVersionSpec}.
         */
        public static final VersionDescription DATE =
            new VersionDescription(Messages.getString("VersionPickerControl.VersionDescriptionDate")); //$NON-NLS-1$

        /**
         * A {@link VersionDescription} that represents an
         * {@link LabelVersionSpec}.
         */
        public static final VersionDescription LABEL =
            new VersionDescription(Messages.getString("VersionPickerControl.VersionDescriptionLabel")); //$NON-NLS-1$

        /**
         * A {@link VersionDescription} that represents an
         * {@link LatestVersionSpec}.
         */
        public static final VersionDescription LATEST =
            new VersionDescription(Messages.getString("VersionPickerControl.VersionDescriptionLatest")); //$NON-NLS-1$

        /**
         * A {@link VersionDescription} that represents an
         * {@link WorkspaceVersionSpec} .
         */
        public static final VersionDescription WORKSPACE =
            new VersionDescription(Messages.getString("VersionPickerControl.VersionDescriptionWorkspace")); //$NON-NLS-1$

        private final String type;

        private VersionDescription(final String type) {
            this.type = type;
        }

        /**
         * The description for the UI.
         *
         * @return The description suitable for display in the UI
         */
        public String toUIString() {
            return type;
        }
    }

    /**
     * A listener that receives notification when a {@link VersionPickerControl}
     * 's version spec has changed, including when it has changed to be invalid.
     */
    public static interface VersionSpecChangedListener extends EventListener {
        /**
         * Called when a {@link VersionPickerControl}'s version spec has
         * changed.
         *
         * @param e
         *        a {@link VersionSpecChangedEvent}, never <code>null</code>
         */
        public void onVersionSpecChanged(VersionSpecChangedEvent e);
    }

    /**
     * An event that a {@link VersionPickerControl} sends to registered
     * {@link VersionSpecChangedListener} when the version spec has changed.
     */
    public static class VersionSpecChangedEvent extends EventObject {
        private final VersionDescription versionType;
        private final VersionSpec versionSpec;

        /**
         * Creates a new {@link VersionSpecChangedEvent}.
         *
         * @param source
         *        the {@link VersionPickerControl} that is sending the event
         *        (must not be <code>null</code>)
         * @param versionType
         *        the {@link VersionDescription} that is currently selected in
         *        the {@link VersionPickerControl}, or <code>null</code> if the
         *        {@link VersionPickerControl} is disabled
         * @param versionSpec
         *        the {@link VersionSpec} currently entered in the
         *        {@link VersionPickerControl}, or <code>null</code> if a valid
         *        version spec is not entered
         */
        public VersionSpecChangedEvent(
            final VersionPickerControl source,
            final VersionDescription versionType,
            final VersionSpec versionSpec) {
            super(source);

            this.versionType = versionType;
            this.versionSpec = versionSpec;
        }

        /**
         * @return the {@link VersionDescription} currently selected in the
         *         {@link VersionPickerControl}, or <code>null</code> if the
         *         {@link VersionPickerControl} is disabled
         */
        public VersionDescription getVersionType() {
            return versionType;
        }

        /**
         * @return the {@link VersionSpec} currently entered in the
         *         {@link VersionPickerControl}, or <code>null</code> if a valid
         *         version spec is not entered
         */
        public VersionSpec getVersionSpec() {
            return versionSpec;
        }

        /**
         * @return <code>true</code> if a valid version spec is currently
         *         entered in the {@link VersionPickerControl} - if this method
         *         returns <code>true</code>, the {@link #getVersionSpec()}
         *         method will return a non-<code>null</code>
         *         {@link VersionSpec}
         */
        public boolean isValid() {
            return versionSpec != null;
        }
    }

    private final VersionDescription DEFAULT_INITIAL_VERSION_TYPE;

    private final VersionDescription[] COMBO_ITEMS;

    private final ValidatorHelper validator;
    private final String errorMessage;
    private TFSRepository repository;

    private VersionDescription versionType;
    private VersionSpec versionSpec;
    private String path;

    private boolean prompt = true;

    private Label typeLabel;
    private Combo typeCombo;
    private Composite dataArea;
    private StackLayout dataAreaLayout;
    private Control[] dataAreaControls;
    private Text changesetText;
    private DatepickerCombo datepicker;
    private Text labelText;
    private Text workspaceText;

    private final SingleListenerFacade versionSpecListeners =
        new SingleListenerFacade(VersionSpecChangedListener.class);

    public final static int NO_PROMPT = 1 << 31;

    /**
     * Creates a new {@link VersionPickerControl}. The validator (
     * {@link #getValidator()}) will not have a message when the
     * {@link VersionPickerControl} is invalid.
     *
     * @param parent
     *        parent {@link Composite} (must not be <code>null</code>)
     * @param style
     *        SWT style bits
     */
    public VersionPickerControl(final Composite parent, final int style) {
        this(parent, style, null);
    }

    /**
     * Creates a new {@link VersionPickerControl}. The validator (
     * {@link #getValidator()}) will have the specified error message when the
     * {@link VersionPickerControl} is invalid.
     *
     * @param parent
     *        parent {@link Composite} (must not be <code>null</code>)
     * @param style
     *        SWT style bits
     * @param errorMessage
     *        validator error message, or <code>null</code> for no message when
     *        invalid
     */
    public VersionPickerControl(final Composite parent, final int style, final String errorMessage) {
        this(parent, style, errorMessage, null, null);
    }

    /**
     * Creates a new {@link VersionPickerControl}. The validator (
     * {@link #getValidator()}) will have the specified error message when the
     * {@link VersionPickerControl} is invalid.
     *
     * @param parent
     *        parent {@link Composite} (must not be <code>null</code>)
     * @param style
     *        SWT style bits
     * @param errorMessage
     *        validator error message, or <code>null</code> for no message when
     *        invalid
     */
    public VersionPickerControl(
        final Composite parent,
        final int style,
        final String errorMessage,
        final VersionDescription[] VersionTypes,
        final VersionDescription defaultVersionType) {
        super(parent, style);

        if (VersionTypes == null) {
            COMBO_ITEMS = new VersionDescription[] {
                VersionDescription.CHANGESET,
                VersionDescription.DATE,
                VersionDescription.LABEL,
                VersionDescription.LATEST,
                VersionDescription.WORKSPACE
            };
        } else {
            COMBO_ITEMS = VersionTypes;
        }

        if (defaultVersionType == null) {
            DEFAULT_INITIAL_VERSION_TYPE = VersionDescription.LATEST;
        } else {
            DEFAULT_INITIAL_VERSION_TYPE = defaultVersionType;
        }
        prompt = !((style & NO_PROMPT) == NO_PROMPT);
        this.errorMessage = errorMessage;

        validator = new ValidatorHelper(this);

        createControls(this);
    }

    /**
     * <p>
     * Sets the {@link TFSRepository} that will be used by this
     * {@link VersionPickerControl}. A {@link VersionPickerControl}'s repository
     * can be changed as often as needed.
     * </p>
     *
     * <p>
     * If this {@link VersionPickerControl} has a valid {@link VersionSpec},
     * that version spec is preserved. Otherwise, the version type is reset to a
     * default value.
     * </p>
     *
     * @param repository
     *        the {@link TFSRepository} to use, or <code>null</code> to disable
     *        this {@link VersionPickerControl}
     */
    public void setRepository(final TFSRepository repository) {
        this.repository = repository;

        if (repository == null) {
            dataAreaLayout.topControl = null;
            dataArea.layout();

            setEnabled(false);

            setNewVersionSpec(null);
        } else {
            setEnabled(true);

            if (versionSpec != null) {
                /*
                 * TODO Might be nice to compare the new TFSRepository and the
                 * old (by instance ID). If it's a different server, we probably
                 * don't want to reuse the same version spec (e.g., could be a
                 * changeset that doesn't exist on the new server). Not a big
                 * deal though, as the user can easily enter a bad version spec.
                 */
                setVersionSpec(versionSpec);
            } else {
                setVersionType(DEFAULT_INITIAL_VERSION_TYPE);
            }
        }
    }

    /**
     * Adds a listener that will be notified when the version spec changes.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addVersionSpecChangedListener(final VersionSpecChangedListener listener) {
        versionSpecListeners.addListener(listener);
    }

    /**
     * Removes a previously added {@link VersionSpecChangedListener}.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeVersionSpecChangedListener(final VersionSpecChangedListener listener) {
        versionSpecListeners.removeListener(listener);
    }

    /**
     * @return the {@link VersionDescription} currently selected in this
     *         {@link VersionPickerControl}, or <code>null</code> if this
     *         {@link VersionPickerControl} is disabled
     */
    public VersionDescription getVersionType() {
        if (repository == null) {
            return null;
        }

        return versionType;
    }

    /**
     * Sets the {@link VersionDescription} currently selected in this
     * {@link VersionPickerControl}. Any entered {@link VersionSpec} will be
     * discarded. If this {@link VersionPickerControl} does not have a
     * repository (see {@link #setRepository(TFSRepository)}), an exception is
     * thrown.
     *
     * @param versionType
     *        the {@link VersionDescription} to select (must not be
     *        <code>null</code>)
     */
    public void setVersionType(final VersionDescription versionType) {
        Check.notNull(versionType, "versionType"); //$NON-NLS-1$

        if (repository == null) {
            throw new IllegalStateException("repository is null"); //$NON-NLS-1$
        }

        int versionTypeIndex = -1;

        for (int i = 0; i < COMBO_ITEMS.length; i++) {
            if (COMBO_ITEMS[i] == versionType) {
                versionTypeIndex = i;
                break;
            }
        }

        if (versionTypeIndex != -1) {
            typeCombo.select(versionTypeIndex);
            newVersionTypeSelected(versionTypeIndex);
        }
    }

    /**
     * @return the entered {@link VersionSpec}, or <code>null</code> if no valid
     *         version spec is entered
     */
    public VersionSpec getVersionSpec() {
        return versionSpec;
    }

    /**
     * Sets the {@link VersionSpec} entered in this {@link VersionPickerControl}
     * . Any existing version spec will be discarded. If this
     * {@link VersionPickerControl} does not have a repository (see
     * {@link #setRepository(TFSRepository)}), an exception is thrown.
     *
     * @param versionSpec
     *        the {@link VersionSpec} to set in this
     *        {@link VersionPickerControl} (must not be <code>null</code>)
     */
    public void setVersionSpec(final VersionSpec versionSpec) {
        Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$

        if (versionSpec instanceof ChangesetVersionSpec) {
            final ChangesetVersionSpec changesetVersionSpec = (ChangesetVersionSpec) versionSpec;
            setVersionType(VersionDescription.CHANGESET);
            changesetText.setText(String.valueOf(changesetVersionSpec.getChangeset()));
        } else if (versionSpec instanceof DateVersionSpec) {
            final DateVersionSpec dateVersionSpec = (DateVersionSpec) versionSpec;
            setVersionType(VersionDescription.DATE);
            datepicker.setDate(dateVersionSpec.getDate().getTime());
        } else if (versionSpec instanceof LabelVersionSpec) {
            final LabelVersionSpec labelVersionSpec = (LabelVersionSpec) versionSpec;
            setVersionType(VersionDescription.LABEL);
            String s = labelVersionSpec.getLabel();
            if (labelVersionSpec.getScope() != null) {
                s += "@" + labelVersionSpec.getScope(); //$NON-NLS-1$
            }
            labelText.setText(s);
        } else if (versionSpec instanceof LatestVersionSpec) {
            setVersionType(VersionDescription.LATEST);
        } else if (versionSpec instanceof WorkspaceVersionSpec) {
            final WorkspaceVersionSpec workspaceVersionSpec = (WorkspaceVersionSpec) versionSpec;
            setVersionType(VersionDescription.WORKSPACE);
            String s = workspaceVersionSpec.getName();
            if (workspaceVersionSpec.getOwnerDisplayName() != null) {
                s += ";" + workspaceVersionSpec.getOwnerDisplayName(); //$NON-NLS-1$
            }
            workspaceText.setText(s);
        } else {
            throw new IllegalArgumentException(MessageFormat.format(
                "illegal version spec type: [{0}]", //$NON-NLS-1$
                versionSpec.getClass().getName()));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.Validatable#getValidator()
     */
    @Override
    public Validator getValidator() {
        return validator;
    }

    /**
     * Sets the "path" used by this {@link VersionPickerControl}. The path is
     * used when displaying child modal dialogs for picking certain version
     * types. The path should be derived from the context that this
     * {@link VersionPickerControl} is displayed in.
     *
     * @param path
     *        the path, or <code>null</code> for no path
     */
    public void setPath(final String path) {
        this.path = path;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
     */
    @Override
    public void setEnabled(final boolean enabled) {
        if (prompt) {
            typeLabel.setEnabled(enabled);
        }

        typeCombo.setEnabled(enabled);
        UIHelpers.setCompositeEnabled(dataArea, enabled);
        if (!enabled) {
            validator.suspendValidation(true);
        } else {
            validator.resumeValidation();
        }
    }

    private void createControls(final Composite composite) {
        final int width = prompt ? 3 : 2;

        final GridLayout layout = new GridLayout(width, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        if (prompt) {
            typeLabel = SWTUtil.createLabel(composite, Messages.getString("VersionPickerControl.VersionLabelText")); //$NON-NLS-1$
        }

        typeCombo = new Combo(composite, SWT.READ_ONLY);

        dataArea = SWTUtil.createComposite(composite);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(dataArea);

        dataAreaLayout = new StackLayout();
        dataArea.setLayout(dataAreaLayout);

        dataAreaControls = new Control[COMBO_ITEMS.length];

        for (int i = 0; i < COMBO_ITEMS.length; i++) {
            typeCombo.add(COMBO_ITEMS[i].toUIString());

            dataAreaControls[i] = createVersionTypeControls(dataArea, COMBO_ITEMS[i]);
        }

        typeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final Combo combo = (Combo) e.widget;
                final int ix = combo.getSelectionIndex();
                newVersionTypeSelected(ix);
            }
        });

        setRepository(null);
    }

    public void setText(final String text) {
        if (prompt) {
            typeLabel.setText(text);
        }
    }

    private Control createVersionTypeControls(final Composite parent, final VersionDescription controlVersionType) {
        if (VersionDescription.CHANGESET == controlVersionType) {
            return createChangesetControls(parent);
        } else if (VersionDescription.DATE == controlVersionType) {
            return createDateControls(parent);
        } else if (VersionDescription.LABEL == controlVersionType) {
            return createLabelControls(parent);
        } else if (VersionDescription.LATEST == controlVersionType) {
            return null;
        } else if (VersionDescription.WORKSPACE == controlVersionType) {
            return createWorkspaceControls(parent);
        }

        return null;
    }

    private Control createChangesetControls(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);

        final GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        SWTUtil.createLabel(composite, Messages.getString("VersionPickerControl.ChangesetLabelText")); //$NON-NLS-1$

        changesetText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(changesetText);

        changesetText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                onChangesetTextChanged();
            }
        });

        final Button button =
            SWTUtil.createButton(composite, Messages.getString("VersionPickerControl.ChangesetBrowseButtonText")); //$NON-NLS-1$
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                findChangeset();
            }
        });

        return composite;
    }

    private Control createDateControls(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);

        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        SWTUtil.createLabel(composite, Messages.getString("VersionPickerControl.DateLabelText")); //$NON-NLS-1$

        datepicker = new DatepickerCombo(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(datepicker);

        datepicker.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                onDatePickerChanged();
            }
        });

        return composite;
    }

    private Control createLabelControls(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);

        final GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        SWTUtil.createLabel(composite, Messages.getString("VersionPickerControl.LabelLabelText")); //$NON-NLS-1$

        labelText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(labelText);

        labelText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                onLabelTextChanged();
            }
        });

        final Button button =
            SWTUtil.createButton(composite, Messages.getString("VersionPickerControl.LabelBrowseButtonText")); //$NON-NLS-1$
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                findLabel();
            }
        });

        return composite;
    }

    private Control createWorkspaceControls(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);

        final GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        SWTUtil.createLabel(composite, Messages.getString("VersionPickerControl.WorkspaceLabelText")); //$NON-NLS-1$

        workspaceText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(workspaceText);

        workspaceText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                onWorkspaceTextChanged();
            }
        });

        /*
         * dummy label needed for layout
         */
        final Label dummyLabel = SWTUtil.createLabel(composite);
        GridDataBuilder.newInstance().vGrab().vFill().applyTo(dummyLabel);
        dummyLabel.setVisible(false);

        return composite;
    }

    private void onWorkspaceTextChanged() {
        final String text = workspaceText.getText().trim();
        VersionSpec versionSpec = null;
        if (text.length() != 0) {
            WorkspaceSpec workspaceSpec;
            try {
                workspaceSpec = WorkspaceSpec.parse(text, VersionControlConstants.AUTHENTICATED_USER);
                versionSpec = new WorkspaceVersionSpec(workspaceSpec);
            } catch (final WorkspaceSpecParseException e) {
            }
        }

        setNewVersionSpec(versionSpec);
    }

    private void findLabel() {
        final FindLabelDialog dialog = new FindLabelDialog(getShell(), repository, null);
        if (IDialogConstants.OK_ID != dialog.open()) {
            return;
        }

        final VersionControlLabel label = dialog.getSelectedLabel();
        if (label == null) {
            return;
        }

        final String spec = new LabelSpec(label.getName(), label.getScope()).toString();
        labelText.setText(spec);
    }

    private void findChangeset() {
        final FindChangesetDialog dialog = new FindChangesetDialog(getShell(), repository);

        if (path != null) {
            dialog.setPath(path);
        }

        if (IDialogConstants.OK_ID != dialog.open()) {
            return;
        }

        final Changeset changeset = dialog.getSelectedChangeset();
        changesetText.setText(String.valueOf(changeset.getChangesetID()));
    }

    private void onLabelTextChanged() {
        final String text = labelText.getText().trim();
        VersionSpec versionSpec = null;
        if (text.length() != 0) {
            try {
                versionSpec = new LabelVersionSpec(LabelSpec.parse(text, null, false));
            } catch (final LabelSpecParseException e) {
            }
        }

        setNewVersionSpec(versionSpec);
    }

    private void onChangesetTextChanged() {
        final String text = changesetText.getText().trim();
        VersionSpec versionSpec = null;
        if (text.length() > 0) {
            try {
                final int changeset = Integer.parseInt(text);
                versionSpec = new ChangesetVersionSpec(changeset);
            } catch (final NumberFormatException e) {

            }
        }
        setNewVersionSpec(versionSpec);
    }

    private void onDatePickerChanged() {
        final Date date = datepicker.getDate();
        if (date != null) {
            final Calendar c = Calendar.getInstance();
            c.setTime(date);
            final DateVersionSpec dateVersionSpec = new DateVersionSpec(c);
            setNewVersionSpec(dateVersionSpec);
        } else {
            setNewVersionSpec(null);
        }
    }

    private void newVersionTypeSelected(final int index) {
        dataAreaLayout.topControl = dataAreaControls[index];
        dataArea.layout();

        versionType = COMBO_ITEMS[index];
        initialize(versionType);
    }

    private void initialize(final VersionDescription vt) {
        if (VersionDescription.CHANGESET == vt) {
            changesetText.setText(""); //$NON-NLS-1$
        } else if (VersionDescription.DATE == vt) {
            onDatePickerChanged();
        } else if (VersionDescription.LABEL == vt) {
            labelText.setText(""); //$NON-NLS-1$
        } else if (VersionDescription.WORKSPACE == vt) {
            final Workspace workspace = repository.getWorkspace();
            workspaceText.setText(workspace.getDisplayName());
        } else if (VersionDescription.LATEST == vt) {
            setNewVersionSpec(LatestVersionSpec.INSTANCE);
        }
    }

    private void setNewVersionSpec(final VersionSpec versionSpec) {
        this.versionSpec = versionSpec;

        final VersionSpecChangedEvent event = new VersionSpecChangedEvent(this, getVersionType(), versionSpec);
        ((VersionSpecChangedListener) versionSpecListeners.getListener()).onVersionSpecChanged(event);

        if (versionSpec != null) {
            validator.setValid();
        } else {
            validator.setInvalid(errorMessage);
        }
    }
}
