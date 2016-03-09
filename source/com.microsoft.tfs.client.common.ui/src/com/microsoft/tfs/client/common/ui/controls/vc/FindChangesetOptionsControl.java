// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.generic.DatepickerCombo;
import com.microsoft.tfs.client.common.ui.controls.vc.history.HistoryInput;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ServerItemPickerDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.WorkspaceItemSource;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.DateVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.webservices.IdentityHelper;
import com.microsoft.tfs.util.Check;

public class FindChangesetOptionsControl extends BaseControl {
    public static final class ValidationException extends Exception {
        public ValidationException(final String message) {
            super(message);
        }
    }

    public static final class VersionRangeType {
        public static final VersionRangeType ALL = new VersionRangeType("ALL"); //$NON-NLS-1$
        public static final VersionRangeType CHANGESET_RANGE = new VersionRangeType("CHANGESET_RANGE"); //$NON-NLS-1$
        public static final VersionRangeType DATE_RANGE = new VersionRangeType("DATE_RANGE"); //$NON-NLS-1$

        private final String type;

        public VersionRangeType(final String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    public static final class ChangesetRange {
        private final VersionRangeType rangeType;
        private final VersionSpec startRange;
        private final VersionSpec endRange;

        public ChangesetRange(
            final VersionRangeType rangeType,
            final VersionSpec startRange,
            final VersionSpec endRange) {
            Check.notNull(rangeType, "rangeType"); //$NON-NLS-1$

            this.rangeType = rangeType;
            this.startRange = startRange;
            this.endRange = endRange;
        }

        public VersionRangeType getRangeType() {
            return rangeType;
        }

        public VersionSpec getStartRange() {
            return startRange;
        }

        public VersionSpec getEndRange() {
            return endRange;
        }
    }

    private final TFSRepository repository;

    private Text pathText;
    private Text usernameText;
    private DatepickerCombo dateRangeFromDatepicker;
    private DatepickerCombo dateRangeToDatepicker;

    private final List<Control> changesetRangeControls = new ArrayList<Control>();
    private final List<Control> dateRangeControls = new ArrayList<Control>();

    private String pathValue;
    private String usernameValue;
    private String changesetRangeFromValue;
    private String changesetRangeToValue;
    private Date dateRangeFromValue;
    private Date dateRangeToValue;

    private VersionRangeType versionRangeType = VersionRangeType.ALL;

    public FindChangesetOptionsControl(final Composite parent, final int style, final TFSRepository repository) {
        super(parent, style);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        this.repository = repository;

        /* Set from date to beginning of today, to date to now */
        dateRangeToValue = new Date();

        final Calendar beginningOfDay = Calendar.getInstance();
        beginningOfDay.setTime(dateRangeToValue);
        beginningOfDay.set(Calendar.HOUR_OF_DAY, 0);
        beginningOfDay.set(Calendar.MINUTE, 0);
        beginningOfDay.set(Calendar.SECOND, 0);
        dateRangeFromValue = beginningOfDay.getTime();
        createControls(this);
    }

    public HistoryInput getHistoryInput() throws ValidationException {
        final ChangesetRange range = getRange();
        String path = getPath();
        RecursionType recursionType;
        VersionSpec itemVersion = LatestVersionSpec.INSTANCE;

        if (path == null) {
            path = ServerPath.ROOT;
            recursionType = RecursionType.FULL;
        } else {
            final VersionSpec itemQueryVersion = LatestVersionSpec.INSTANCE;

            /*
             * VS control always includes deleted items and source renames.
             */
            Item item = null;

            try {
                item = repository.getVersionControlClient().getItem(
                    path,
                    itemQueryVersion,
                    DeletedState.ANY,
                    GetItemsOptions.INCLUDE_SOURCE_RENAMES);
            } catch (final Exception e) {
                throw new ValidationException(e.getLocalizedMessage());
            }

            if (item == null) {
                throw new ValidationException(
                    MessageFormat.format(
                        Messages.getString("FindChangesetOptionsControl.ItemNotFoundInSourceControlAtVersionFormat"), //$NON-NLS-1$
                        path,
                        itemQueryVersion));
            }

            path = item.getServerItem();
            recursionType = (item.getItemType() == ItemType.FILE ? RecursionType.NONE : RecursionType.FULL);
            if (item.getDeletionID() != 0) {
                itemVersion = new ChangesetVersionSpec(item.getChangeSetID() - 1);
            }
        }

        final HistoryInput.Builder builder =
            new HistoryInput.Builder(getShell(), repository, path, itemVersion, recursionType);
        builder.setUserFilter(IdentityHelper.getUniqueNameIfCurrentUser(
            repository.getConnection().getAuthorizedIdentity(),
            getUsername()));
        builder.setVersionFrom(range.getStartRange());
        builder.setVersionTo(range.getEndRange());

        /*
         * Use slot mode for 2010, item mode for previous. This is a slight hack
         * (and different from VS's query behavior in this same dialog) because
         * our history table control isn't perfect.
         *
         * VS sets slot mode false always, because its history table control
         * handles expansion along branches and renames for 2010 and item mode
         * is the correct behavior for 2005 and 2008. TEE's history control
         * shows too many rows for item mode in 2010 (it queries as slot mode),
         * so do a simple switch on server version to work around this.
         */
        builder.setSlotMode(
            repository.getVersionControlClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue());

        return builder.build();
    }

    public String getPath() {
        return pathValue;
    }

    public void setPath(final String path) {
        pathValue = path;
        if (pathValue != null && pathValue.trim().length() == 0) {
            pathValue = null;
        }
        pathText.setText(pathValue != null ? pathValue : ""); //$NON-NLS-1$
    }

    public String getUsername() {
        return usernameValue;
    }

    public void setUsername(final String username) {
        usernameValue = username;
        if (usernameValue != null && usernameValue.trim().length() == 0) {
            usernameValue = null;
        }
        usernameText.setText(usernameValue != null ? usernameValue : ""); //$NON-NLS-1$
    }

    public ChangesetRange getRange() throws ValidationException {
        if (VersionRangeType.CHANGESET_RANGE == versionRangeType) {
            final ChangesetVersionSpec spec1 = buildChangesetVersionSpec(changesetRangeFromValue);
            final ChangesetVersionSpec spec2 = buildChangesetVersionSpec(changesetRangeToValue);
            if (spec1.getChangeset() < spec2.getChangeset()) {
                return new ChangesetRange(versionRangeType, spec1, spec2);
            }
            return new ChangesetRange(versionRangeType, spec2, spec1);
        }

        if (VersionRangeType.DATE_RANGE == versionRangeType) {
            if (dateRangeFromValue == null || dateRangeToValue == null) {
                throw new ValidationException(Messages.getString("FindChangesetOptionsControl.PleaseSpecifyADate")); //$NON-NLS-1$
            }

            Calendar c = Calendar.getInstance();
            c.setTime(dateRangeFromValue);
            final DateVersionSpec spec1 = new DateVersionSpec(c);

            c = Calendar.getInstance();
            c.setTime(dateRangeToValue);
            final DateVersionSpec spec2 = new DateVersionSpec(c);

            if (dateRangeFromValue.before(dateRangeToValue)) {
                return new ChangesetRange(versionRangeType, spec1, spec2);
            }
            return new ChangesetRange(versionRangeType, spec2, spec1);
        }

        return new ChangesetRange(VersionRangeType.ALL, null, null);
    }

    @Override
    public boolean setFocus() {
        return pathText.setFocus();
    }

    private ChangesetVersionSpec buildChangesetVersionSpec(final String stringValue) throws ValidationException {
        if (stringValue == null || stringValue.trim().length() == 0) {
            throw new ValidationException(Messages.getString("FindChangesetOptionsControl.PleaseSpecifyAChangeset")); //$NON-NLS-1$
        }

        int intValue;
        try {
            intValue = Integer.parseInt(stringValue);
        } catch (final NumberFormatException ex) {
            final String msg = MessageFormat.format(
                Messages.getString("FindChangesetOptionsControl.NumberIsNotAValidChangesetNumberFormat"), //$NON-NLS-1$
                stringValue);
            throw new ValidationException(msg);
        }

        if (intValue <= 0) {
            final String msg = MessageFormat.format(
                Messages.getString("FindChangesetOptionsControl.NumberIsNotAValidChangesetNumberFormat"), //$NON-NLS-1$
                Integer.toString(intValue));
            throw new ValidationException(msg);
        }

        return new ChangesetVersionSpec(intValue);
    }

    private void createControls(final Composite composite) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        final Label containingFileLabel = new Label(composite, SWT.NONE);
        containingFileLabel.setText(Messages.getString("FindChangesetOptionsControl.ContainingFileLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(containingFileLabel);

        pathText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(pathText);
        if (pathValue != null) {
            pathText.setText(pathValue);
        }
        pathText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                pathValue = ((Text) e.widget).getText().trim();
                if (pathValue.length() == 0) {
                    pathValue = null;
                }
            }
        });

        final Button browseButton = new Button(composite, SWT.NONE);
        browseButton.setText(Messages.getString("FindChangesetOptionsControl.BrowseButtonText")); //$NON-NLS-1$
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                browsePressed();
            }
        });

        final Label byUserLabel = new Label(composite, SWT.NONE);
        byUserLabel.setText(Messages.getString("FindChangesetOptionsControl.ByUserLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(byUserLabel);

        usernameText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hSpan(layout).hGrab().hFill().applyTo(usernameText);
        if (usernameValue != null) {
            usernameText.setText(usernameValue);
        }
        usernameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                usernameValue = ((Text) e.widget).getText().trim();
                if (usernameValue.length() == 0) {
                    usernameValue = null;
                }
            }
        });

        final Label changesetRangeLabel = new Label(composite, SWT.NONE);
        changesetRangeLabel.setText(Messages.getString("FindChangesetOptionsControl.RangeLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(changesetRangeLabel);

        final Button allChangesButton = new Button(composite, SWT.RADIO);
        allChangesButton.setText(Messages.getString("FindChangesetOptionsControl.AllChangesRadioText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(allChangesButton);
        allChangesButton.setSelection(VersionRangeType.ALL == versionRangeType);
        allChangesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                versionRangeTypeChanged(VersionRangeType.ALL);
            }
        });

        final Button changesetRangeButton = new Button(composite, SWT.RADIO);
        changesetRangeButton.setText(Messages.getString("FindChangesetOptionsControl.ChangesetNumberRadioText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(changesetRangeButton);
        changesetRangeButton.setSelection(VersionRangeType.CHANGESET_RANGE == versionRangeType);
        changesetRangeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                versionRangeTypeChanged(VersionRangeType.CHANGESET_RANGE);
            }
        });

        final Composite changesetNumberComposite = new Composite(composite, SWT.NONE);
        final GridLayout changesetNumberLayout = new GridLayout(4, false);
        changesetNumberLayout.marginWidth = 0;
        changesetNumberLayout.marginHeight = 0;
        changesetNumberLayout.horizontalSpacing = getHorizontalSpacing();
        changesetNumberLayout.verticalSpacing = getVerticalSpacing();
        changesetNumberComposite.setLayout(changesetNumberLayout);
        GridDataBuilder.newInstance().hSpan(layout).hGrab().hFill().hIndent(getHorizontalSpacing() * 4).applyTo(
            changesetNumberComposite);

        final Label changesetFromLabel = new Label(changesetNumberComposite, SWT.NONE);
        changesetFromLabel.setText(Messages.getString("FindChangesetOptionsControl.FromLabelText")); //$NON-NLS-1$
        changesetRangeControls.add(changesetFromLabel);

        final Text changesetFromText = new Text(changesetNumberComposite, SWT.BORDER);
        changesetRangeControls.add(changesetFromText);
        if (changesetRangeFromValue != null) {
            changesetFromText.setText(changesetRangeFromValue);
        }
        changesetFromText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                changesetRangeFromValue = ((Text) e.widget).getText();
            }
        });
        GridDataBuilder.newInstance().wCHint(changesetFromText, 10).applyTo(changesetFromText);

        final Label changesetToLabel = new Label(changesetNumberComposite, SWT.NONE);
        changesetToLabel.setText(Messages.getString("FindChangesetOptionsControl.ToLabelText")); //$NON-NLS-1$
        changesetRangeControls.add(changesetToLabel);

        final Text changesetToText = new Text(changesetNumberComposite, SWT.BORDER);
        changesetRangeControls.add(changesetToText);
        if (changesetRangeToValue != null) {
            changesetToText.setText(changesetRangeToValue);
        }
        changesetToText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                changesetRangeToValue = ((Text) e.widget).getText();
            }
        });
        GridDataBuilder.newInstance().wCHint(changesetToText, 10).applyTo(changesetToText);

        final Button createdDateButton = new Button(composite, SWT.RADIO);
        createdDateButton.setText(Messages.getString("FindChangesetOptionsControl.CreatedDateRadioText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(createdDateButton);
        createdDateButton.setSelection(VersionRangeType.DATE_RANGE == versionRangeType);
        createdDateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                versionRangeTypeChanged(VersionRangeType.DATE_RANGE);
            }
        });

        final Composite dateComposite = new Composite(composite, SWT.NONE);
        final GridLayout dateLayout = new GridLayout(4, false);
        dateLayout.marginWidth = 0;
        dateLayout.marginHeight = 0;
        dateLayout.horizontalSpacing = getHorizontalSpacing();
        dateLayout.verticalSpacing = getVerticalSpacing();
        dateComposite.setLayout(dateLayout);
        GridDataBuilder.newInstance().hSpan(layout).hGrab().hFill().hIndent(getHorizontalSpacing() * 4).applyTo(
            dateComposite);

        final Label dateRangeFromLabel = new Label(dateComposite, SWT.NONE);
        dateRangeFromLabel.setText(Messages.getString("FindChangesetOptionsControl.BetweenLabelText")); //$NON-NLS-1$
        dateRangeControls.add(dateRangeFromLabel);

        dateRangeFromDatepicker = new DatepickerCombo(dateComposite, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(dateRangeFromDatepicker);
        dateRangeControls.add(dateRangeFromDatepicker);
        if (dateRangeFromValue != null) {
            dateRangeFromDatepicker.setDate(dateRangeFromValue);
        }
        dateRangeFromDatepicker.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                dateRangeFromValue = dateRangeFromDatepicker.getDate();
            }
        });

        final Label dateRangeToLabel = new Label(dateComposite, SWT.NONE);
        dateRangeToLabel.setText(Messages.getString("FindChangesetOptionsControl.AndLabelText")); //$NON-NLS-1$
        dateRangeControls.add(dateRangeToLabel);

        dateRangeToDatepicker = new DatepickerCombo(dateComposite, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(dateRangeToDatepicker);
        dateRangeControls.add(dateRangeToDatepicker);
        if (dateRangeToValue != null) {
            dateRangeToDatepicker.setDate(dateRangeToValue);
        }
        dateRangeToDatepicker.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                dateRangeToValue = dateRangeToDatepicker.getDate();
            }
        });

        // initial enablement
        versionRangeTypeChanged(versionRangeType);
    }

    private void versionRangeTypeChanged(final VersionRangeType versionRangeType) {
        this.versionRangeType = versionRangeType;

        enableControls(changesetRangeControls, VersionRangeType.CHANGESET_RANGE == versionRangeType);
        enableControls(dateRangeControls, VersionRangeType.DATE_RANGE == versionRangeType);
    }

    private void enableControls(final List<Control> controls, final boolean enable) {
        for (final Iterator<Control> it = controls.iterator(); it.hasNext();) {
            final Control control = it.next();
            control.setEnabled(enable);
        }
    }

    private void browsePressed() {
        final ServerItemSource serverItemSource = new WorkspaceItemSource(repository.getWorkspace());
        final ServerItemPickerDialog dialog =
            new ServerItemPickerDialog(
                getShell(),
                Messages.getString("FindChangesetOptionsControl.BrowseDialogTitle"), //$NON-NLS-1$
                pathValue,
                serverItemSource);

        if (IDialogConstants.OK_ID != dialog.open()) {
            return;
        }

        setPath(dialog.getSelectedServerPath());
    }
}
