// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.vc.DeleteShelvesetsCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryShelvesetsCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.AutocompleteCombo;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ShelvesetDetailsDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.DoubleClickAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.prefs.MRUPreferenceSerializer;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.webservices.IdentityHelper;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.MRUSet;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class ShelvesetSearchControl extends BaseControl {
    public static final String OWNER_COMBO_ID = "ShelvesetSearchControl.ownerCombo"; //$NON-NLS-1$
    public static final String FIND_BUTTON_ID = "ShelvesetSearchControl.findButton"; //$NON-NLS-1$
    public static final String SHELVESETS_TABLE_ID = "ShelvesetSearchControl.shelvesetsTable"; //$NON-NLS-1$

    private static final int MRU_SHELVESET_OWNER_MAX = 10;

    public static final CodeMarker SHELVESETS_QUERY_STARTED =
        new CodeMarker("com.microsoft.tfs.client.common.ui.controls.vc.ShelvesetSearchControl#queryStarted"); //$NON-NLS-1$
    public static final CodeMarker SHELVESETS_QUERY_FINISHED =
        new CodeMarker("com.microsoft.tfs.client.common.ui.controls.vc.ShelvesetSearchControl#queryFinished"); //$NON-NLS-1$

    private final TFSRepository repository;

    private final ShelvesetSearchModifyListener shelvesetSearchModifyListener = new ShelvesetSearchModifyListener();

    private final Combo ownerCombo;
    private final MRUSet ownerComboMRUSet;

    private final Button findButton;
    private final ShelvesetsTable shelvesetsTable;
    private final boolean allowUnshelve;

    /*
     * mruOwnerFilter - filters which successfully-queried owner names are
     * accepted into the most-recently-used values list (which is persisted to
     * disk). This filter rejects the currently authenticated initial owner set
     * on the control (usually the currently authenticated user) so it does not
     * clutter the MRU
     */
    private final OwnerFilter mruOwnerFilter;

    private final Label progressLabel;
    private final ProgressBar progressBar;

    private ChangeItem[] checkedChangeItems;
    private boolean preserveShelveset = true;
    private boolean restoreData = true;

    private final SingleListenerFacade listeners = new SingleListenerFacade(ShelvesetSearchUnshelveListener.class);

    /**
     * Constructs a {@link ShelvesetSearchControl}.
     *
     * @param parent
     *        the parent control (must not be <code>null</code>)
     * @param style
     *        the style bits
     * @param allowUnshelve
     *        if <code>true</code> the shelveset details dialog offers the user
     *        an "unshelve" button; if <code>false</code> the shelveset details
     *        can only view the details and close
     * @param repository
     *        the current TFS repository. Cannot be <code>null</code>
     */
    public ShelvesetSearchControl(
        final Composite parent,
        final int style,
        final boolean allowUnshelve,
        final TFSRepository repository) {
        super(parent, style);
        this.allowUnshelve = allowUnshelve;
        this.repository = repository;

        final String owner = getLoggedInUsername();
        this.mruOwnerFilter = new OwnerFilter(owner);

        final GridLayout controlLayout = new GridLayout(2, false);
        controlLayout.horizontalSpacing = getHorizontalSpacing();
        controlLayout.verticalSpacing = getVerticalSpacing();
        controlLayout.marginWidth = 0;
        controlLayout.marginHeight = 0;
        setLayout(controlLayout);

        final Group searchGroup = new Group(this, SWT.NONE);
        searchGroup.setLayoutData(new GridDataBuilder().hAlignFill().hGrab().hSpan(2).getGridData());
        searchGroup.setText(Messages.getString("ShelvesetSearchControl.FindShelvesetsGroupText")); //$NON-NLS-1$

        final GridLayout searchGroupLayout = new GridLayout(3, false);
        searchGroupLayout.horizontalSpacing = getHorizontalSpacing();
        searchGroupLayout.verticalSpacing = getVerticalSpacing();
        searchGroupLayout.marginWidth = getHorizontalMargin();
        searchGroupLayout.marginHeight = getVerticalMargin();
        searchGroup.setLayout(searchGroupLayout);

        final Label ownerLabel = new Label(searchGroup, SWT.NONE);
        ownerLabel.setText(Messages.getString("ShelvesetSearchControl.OwnerLabelText")); //$NON-NLS-1$
        ownerLabel.setLayoutData(new GridDataBuilder().hAlignLeft().vAlignCenter().getGridData());

        ownerCombo = new AutocompleteCombo(searchGroup, SWT.BORDER);
        ownerCombo.setLayoutData(new GridDataBuilder().hAlignFill().vAlignCenter().hGrab().getGridData());
        ownerCombo.addModifyListener(shelvesetSearchModifyListener);
        AutomationIDHelper.setWidgetID(ownerCombo, OWNER_COMBO_ID);

        findButton = new Button(searchGroup, SWT.PUSH);
        findButton.setText(Messages.getString("ShelvesetSearchControl.FindButtonText")); //$NON-NLS-1$
        findButton.setLayoutData(
            new GridDataBuilder().hAlignRight().vAlignCenter().wButtonHint(findButton).getGridData());
        findButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                query();
            }
        });
        AutomationIDHelper.setWidgetID(findButton, FIND_BUTTON_ID);

        shelvesetsTable = new ShelvesetsTable(this, SWT.FULL_SELECTION | SWT.MULTI);
        shelvesetsTable.setText(Messages.getString("ShelvesetSearchControl.ResultsTableText")); //$NON-NLS-1$
        shelvesetsTable.setLayoutData(
            new GridDataBuilder().fill().grab().vIndent(getVerticalSpacing()).hSpan(2).getGridData());
        ControlSize.setCharHeightHint(shelvesetsTable, 8);

        shelvesetsTable.addDoubleClickListener(new DoubleClickAdapter() {
            @Override
            protected void doubleClick(final Object item) {
                detailsForSelectedShelveset();
            }
        });
        AutomationIDHelper.setWidgetID(shelvesetsTable, SHELVESETS_TABLE_ID);

        progressLabel = new Label(this, SWT.NONE);
        progressLabel.setText(Messages.getString("ShelvesetSearchControl.SearchingProgressLabelText")); //$NON-NLS-1$
        progressLabel.setVisible(false);

        progressBar = new ProgressBar(this, SWT.INDETERMINATE);
        progressBar.setLayoutData(new GridDataBuilder().hAlignFill().vAlignBottom().hGrab().getGridData());
        progressBar.setVisible(false);

        ownerComboMRUSet = new MRUPreferenceSerializer(TFSCommonUIClientPlugin.getDefault().getPreferenceStore()).read(
            MRU_SHELVESET_OWNER_MAX,
            UIPreferenceConstants.SHELVESET_SEARCH_CONTROL_OWNER_MRU_PREFIX);

        refreshOwnerComboItemsFromMRU();

        ownerCombo.setText(owner);
        ownerCombo.setSelection(new Point(0, ownerCombo.getText().length()));
    }

    private String getLoggedInUsername() {
        final TeamFoundationIdentity userIdentity =
            repository.getVersionControlClient().getConnection().getAuthorizedIdentity();
        return userIdentity.getDisplayName();
    }

    public void addUnshelveListener(final ShelvesetSearchUnshelveListener listener) {
        listeners.addListener(listener);
    }

    public void removeUnshelveListener(final ShelvesetSearchUnshelveListener listener) {
        listeners.removeListener(listener);
    }

    public Shelveset[] getSelectedShelvesets() {
        return shelvesetsTable.getSelectedShelvesets();
    }

    public void detailsForSelectedShelveset() {
        final Shelveset[] selectedShelvesets = getSelectedShelvesets();

        if (selectedShelvesets == null || selectedShelvesets.length != 1) {
            return;
        }

        final ShelvesetDetailsDialog detailsDialog =
            new ShelvesetDetailsDialog(getShell(), selectedShelvesets[0], repository, allowUnshelve);

        detailsDialog.setPreserveShelveset(preserveShelveset);
        detailsDialog.setRestoreData(restoreData);

        if (detailsDialog.open() == IDialogConstants.OK_ID) {
            checkedChangeItems = detailsDialog.getCheckedChangeItems();
            preserveShelveset = detailsDialog.isPreserveShelveset();
            restoreData = detailsDialog.isRestoreData();

            ((ShelvesetSearchUnshelveListener) listeners.getListener()).onShelvesetSearchUnshelve(
                new ShelvesetSearchUnshelveEvent(this));
        } else {
            /*
             * The user cancelled the dialog, so set the checked change items
             * for this shelveset to null (indicates the entire shelveset should
             * be unshelved). This prepares this control for details on another
             * shelveset, or the same shelveset again.
             */
            checkedChangeItems = null;
        }
    }

    public void deleteSelectedShelvesets() {
        final Shelveset[] selectedShelvesets = getSelectedShelvesets();

        if (selectedShelvesets == null || selectedShelvesets.length == 0) {
            return;
        }

        String title, message;

        if (selectedShelvesets.length == 1) {
            title = Messages.getString("ShelvesetSearchControl.DeleteShelvesetDialogTitle"); //$NON-NLS-1$
            message =
                MessageFormat.format(
                    Messages.getString("ShelvesetSearchControl.DeleteShelvesetDialogMessageFormat"), //$NON-NLS-1$
                    selectedShelvesets[0].getName());
        } else {
            title = Messages.getString("ShelvesetSearchControl.DeleteShelvesetsDialogTitle"); //$NON-NLS-1$
            message = Messages.getString("ShelvesetSearchControl.DeleteShelvesetsDialogMessage"); //$NON-NLS-1$
        }

        if (!MessageDialog.openQuestion(getShell(), title, message)) {
            return;
        }

        final DeleteShelvesetsCommand deleteCommand = new DeleteShelvesetsCommand(repository, selectedShelvesets);
        UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(deleteCommand);

        /*
         * Ignore the status -- we could have an error deleting some shelvesets
         * but have succeeded in deleting others. The command will tell us what
         * it actually did here.
         */
        shelvesetsTable.removeShelvesets(deleteCommand.getDeletedShelvesets());
    }

    /**
     * @return the "find" button, so dialogs can do things like make it their
     *         default button
     */
    public Button getFindButton() {
        return findButton;
    }

    public void addOwnerTextFocusListener(final FocusListener listener) {
        ownerCombo.addFocusListener(listener);
    }

    public void removeOwnerTextFocusListener(final FocusListener listener) {
        ownerCombo.removeFocusListener(listener);
    }

    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        shelvesetsTable.addSelectionChangedListener(listener);
    }

    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        shelvesetsTable.removeSelectionChangedListener(listener);
    }

    /**
     * @return the source control change items the user checked, or
     *         <code>null</code> if all items were checked
     */
    public ChangeItem[] getCheckedChangeItems() {
        return checkedChangeItems;
    }

    /**
     * @return the preserveShelveset
     */
    public boolean isPreserveShelveset() {
        return preserveShelveset;
    }

    /**
     * @param preserveShelveset
     *        the preserveShelveset to set
     */
    public void setPreserveShelveset(final boolean preserveShelveset) {
        this.preserveShelveset = preserveShelveset;
    }

    /**
     * @return the restoreData
     */
    public boolean isRestoreData() {
        return restoreData;
    }

    /**
     * @param restoreData
     *        the restoreData to set
     */
    public void setRestoreData(final boolean restoreData) {
        this.restoreData = restoreData;
    }

    public void query() {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        final Display display = getDisplay();

        // Get the owner text from the combo.
        final String ownerText = ownerCombo.getText();

        // Determine which owner name to send to the server.
        final String ownerQueryString = ownerText.equals("*") ? null //$NON-NLS-1$
            : IdentityHelper.getUniqueNameIfCurrentUser(repository.getConnection().getAuthorizedIdentity(), ownerText);

        final Point ownerSelection = ownerCombo.getSelection();
        final boolean ownerIsFocusControl = ownerCombo.isFocusControl();

        ownerCombo.setEnabled(false);
        findButton.setEnabled(false);
        progressLabel.setVisible(true);
        progressBar.setVisible(true);
        progressBar.setToolTipText(
            MessageFormat.format(
                Messages.getString("ShelvesetSearchControl.SearchingForShelvesetsProgressTooltipTextFormat"), //$NON-NLS-1$
                ownerQueryString));
        shelvesetsTable.setShelvesets(new Shelveset[0], repository);

        final Runnable queryRunner = new Runnable() {
            @Override
            public void run() {
                final QueryShelvesetsCommand queryCommand;
                final IStatus status[] = new IStatus[1];

                queryCommand = new QueryShelvesetsCommand(repository.getVersionControlClient(), null, ownerQueryString);

                try {
                    status[0] = queryCommand.run(null);
                } catch (final Exception e) {
                    status[0] = new Status(Status.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, e.getMessage(), null);
                }

                UIHelpers.runOnUIThread(display, true, new Runnable() {
                    @Override
                    public void run() {
                        if (isDisposed()) {
                            return;
                        }

                        progressLabel.setVisible(false);
                        progressBar.setVisible(false);

                        if (status[0].isOK()) {
                            shelvesetsTable.setShelvesets(queryCommand.getShelvesets(), repository);

                            /*
                             * If ownerQueryString is null, the user typed "*"
                             * so don't persist to MRU.
                             */
                            if (ownerQueryString != null
                                && (mruOwnerFilter.accepts(ownerText))
                                && ownerComboMRUSet.add(ownerText)) {
                                new MRUPreferenceSerializer(
                                    TFSCommonUIClientPlugin.getDefault().getPreferenceStore()).write(
                                        ownerComboMRUSet,
                                        UIPreferenceConstants.SHELVESET_SEARCH_CONTROL_OWNER_MRU_PREFIX);

                                /*
                                 * Can blow away combo's text, selection, and
                                 * focus (does on GTK). Will restore below.
                                 */
                                refreshOwnerComboItemsFromMRU();
                            }
                        } else {
                            ErrorDialog.openError(
                                getShell(),
                                Messages.getString("ShelvesetSearchControl.ErrorFindingShelvesetsDialogTitle"), //$NON-NLS-1$
                                null,
                                status[0]);
                        }

                        ownerCombo.setText(ownerText);
                        ownerCombo.setEnabled(true);
                        ownerCombo.setSelection(ownerSelection);
                        if (ownerIsFocusControl) {
                            ownerCombo.setFocus();
                        }

                        findButton.setEnabled(true);

                        CodeMarkerDispatch.dispatch(SHELVESETS_QUERY_FINISHED);
                    }
                });
            }
        };

        CodeMarkerDispatch.dispatch(SHELVESETS_QUERY_STARTED);
        new Thread(queryRunner).start();
    }

    /**
     * Refreshes the owner combo box items from the control's in-memory MRU
     * data. Combo behavior can differ by platform as to whether the text and
     * selection are preserved while this happens. Clients are responsible for
     * preserving those things if they care about them.
     */
    private void refreshOwnerComboItemsFromMRU() {
        /*
         * The MRUSet keeps most recent items at the end, but it's nice if
         * they're first in the drop-down, so reverse.
         */
        final List<String> mruItemsList = new ArrayList<String>(ownerComboMRUSet);
        Collections.reverse(mruItemsList);
        ownerCombo.setItems(mruItemsList.toArray(new String[mruItemsList.size()]));
    }

    private final class ShelvesetSearchModifyListener implements ModifyListener {
        @Override
        public void modifyText(final ModifyEvent e) {
            final String owner = ownerCombo.getText().trim();
            findButton.setEnabled(owner.length() > 0 && owner.length() < 256);
        }
    }

    public class ShelvesetSearchUnshelveEvent extends EventObject {
        private static final long serialVersionUID = -6680509068066295352L;

        public ShelvesetSearchUnshelveEvent(final ShelvesetSearchControl control) {
            super(control);
        }
    }

    public interface ShelvesetSearchUnshelveListener extends EventListener {
        public void onShelvesetSearchUnshelve(ShelvesetSearchUnshelveEvent event);
    }

    /**
     * An {@link OwnerFilter} which rejects the one specified owner, accepts all
     * non-empty others.
     *
     * @threadsafety thread-compatible
     */
    private static class OwnerFilter {
        private final String initialOwner;

        public OwnerFilter(final String initialOwner) {
            Check.notNullOrEmpty(initialOwner, "initialOwner"); //$NON-NLS-1$
            this.initialOwner = initialOwner;
        }

        /**
         * {@inheritDoc}
         */
        public boolean accepts(final String owner) {
            return owner.length() > 0 && initialOwner.equals(owner) == false;
        }
    }

}