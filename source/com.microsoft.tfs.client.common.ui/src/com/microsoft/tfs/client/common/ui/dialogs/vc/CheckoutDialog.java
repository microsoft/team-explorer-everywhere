// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
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
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.TypedItemSpecTable;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.vc.TypedItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

/**
 * A dialog which presents a collection of {@link ItemSpec}s in a checked table
 * for the user's approval.
 *
 * @threadsafety thread-compatible
 */
public class CheckoutDialog extends BaseDialog {
    /**
     * Model class for the lock level combo box.
     *
     * @threadsafety thread-compatible
     */
    protected static class LockLevelComboItem {
        private final LockLevel lockLevel;
        private final String description;

        private LockLevelComboItem(final LockLevel lockLevel, final String description) {
            Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
            Check.notNull(description, "description"); //$NON-NLS-1$

            this.lockLevel = lockLevel;
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public LockLevel getLockLevel() {
            return lockLevel;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object o) {
            if (o == null) {
                return false;
            }

            if (o == this) {
                return true;
            }

            if (o instanceof LockLevelComboItem == false) {
                return false;
            }

            return lockLevel == ((LockLevelComboItem) o).lockLevel;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return lockLevel.hashCode();
        }
    }

    public static final String LOCK_LEVEL_COMBO_ID = "CheckoutDialog.lockTypeCombo"; //$NON-NLS-1$

    protected static final LockLevelComboItem LOCK_LEVEL_UNCHANGED =
        new LockLevelComboItem(LockLevel.UNCHANGED, Messages.getString("CheckoutDialog.KeepLockChoice")); //$NON-NLS-1$

    protected static final LockLevelComboItem LOCK_LEVEL_NONE =
        new LockLevelComboItem(LockLevel.NONE, Messages.getString("CheckoutDialog.AllowSharedCheckoutChoice")); //$NON-NLS-1$

    protected static final LockLevelComboItem LOCK_LEVEL_CHECKOUT =
        new LockLevelComboItem(LockLevel.CHECKOUT, Messages.getString("CheckoutDialog.PreventOthersChoice")); //$NON-NLS-1$

    protected static final LockLevelComboItem LOCK_LEVEL_CHECKIN =
        new LockLevelComboItem(LockLevel.CHECKIN, Messages.getString("CheckoutDialog.PreventCheckinChoice")); //$NON-NLS-1$

    protected static final LockLevelComboItem[] KNOWN_LOCK_LEVELS = {
        LOCK_LEVEL_UNCHANGED,
        LOCK_LEVEL_NONE,
        LOCK_LEVEL_CHECKOUT,
        LOCK_LEVEL_CHECKIN
    };

    private TypedItemSpecTable itemSpecTable;
    private final TypedItemSpec[] initialItemSpecs;

    /**
     * Updated when the combo box's selection changes.
     */
    private boolean lockLevelForced = false;
    private LockLevel lockLevel = LockLevel.NONE;

    public CheckoutDialog(final Shell parentShell, final TypedItemSpec[] initialItemSpecs) {
        this(parentShell, initialItemSpecs, null);
    }

    public CheckoutDialog(final Shell parentShell, final TypedItemSpec[] initialItemSpecs, final LockLevel lockLevel) {
        super(parentShell);

        Check.notNull(initialItemSpecs, "initialItemSpecs"); //$NON-NLS-1$
        this.initialItemSpecs = initialItemSpecs;

        if (lockLevel != null) {
            this.lockLevel = lockLevel;
            lockLevelForced = true;
        }
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        dialogArea.setLayout(layout);

        itemSpecTable = new TypedItemSpecTable(dialogArea, SWT.CHECK | SWT.MULTI);
        itemSpecTable.setTypedItemSpecs(initialItemSpecs);
        itemSpecTable.setText(Messages.getString("CheckoutDialog.FilesLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(2).grab().fill().applyTo(itemSpecTable);
        ControlSize.setCharHeightHint(itemSpecTable, 10);

        final Label lockTypeLabel = new Label(dialogArea, SWT.NONE);
        lockTypeLabel.setText(Messages.getString("CheckoutDialog.LockTypeLabelText")); //$NON-NLS-1$

        final Combo lockTypeCombo = new Combo(dialogArea, SWT.DROP_DOWN | SWT.READ_ONLY);
        AutomationIDHelper.setWidgetID(lockTypeCombo, LOCK_LEVEL_COMBO_ID);

        final LockLevelComboItem[] enabledLockLevelItems = getEnabledLockLevelItems();
        Check.notNull(enabledLockLevelItems, "enabledLockLevelItems"); //$NON-NLS-1$

        final String[] descriptions = new String[enabledLockLevelItems.length];
        String currentDescription = null;

        for (int i = 0; i < enabledLockLevelItems.length; i++) {
            descriptions[i] = enabledLockLevelItems[i].getDescription();

            if (enabledLockLevelItems[i].getLockLevel().equals(lockLevel)) {
                currentDescription = enabledLockLevelItems[i].getDescription();
            }
        }

        lockTypeCombo.setItems(descriptions);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(lockTypeCombo);

        lockTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final LockLevelComboItem[] enabledLockLevelItems = getEnabledLockLevelItems();

                final int selectedIndex = ((Combo) e.getSource()).getSelectionIndex();
                lockLevel = enabledLockLevelItems[selectedIndex].getLockLevel();
            };
        });

        /*
         * Users may be forcing a specific lock level (for example, when the
         * server is configured to force checkout locks.)
         */
        if (lockLevelForced == true) {
            lockTypeCombo.setText(currentDescription != null ? currentDescription : lockLevel.toUIString());
            lockTypeCombo.setEnabled(false);
        } else {
            lockLevel = getDefaultLockLevelItem().getLockLevel();
            lockTypeCombo.setText(getDefaultLockLevelItem().getDescription());
        }

        /*
         * TODO: description of the version being checked out -- if force get
         * latest is on (on the client or server) we should report that.
         */

        itemSpecTable.checkAll();
    }

    @Override
    protected void hookAfterButtonsCreated() {
        final Button button = getButton(IDialogConstants.OK_ID);

        new ButtonValidatorBinding(button).bind(itemSpecTable.getCheckboxValidator());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String provideDialogTitle() {
        return Messages.getString("CheckoutDialog.DialogTitle"); //$NON-NLS-1$
    }

    /**
     * Extending classes should override to control the available lock types
     * presented to the user.
     *
     * @return the items to show in the lock level combo box, in the correct
     *         order, instances to choose from are all defined as static fields
     *         in this class (must not be <code>null</code>)
     */
    protected LockLevelComboItem[] getEnabledLockLevelItems() {
        return new LockLevelComboItem[] {
            CheckoutDialog.LOCK_LEVEL_UNCHANGED,
            CheckoutDialog.LOCK_LEVEL_CHECKOUT,
            CheckoutDialog.LOCK_LEVEL_CHECKIN
        };
    }

    /**
     * Extending classes should override to change the default lock type.
     *
     * @return the item in the array returned by
     *         {@link #getEnabledLockLevelItems()} that should be the default
     *         selected item in the lock level combo box
     */
    protected LockLevelComboItem getDefaultLockLevelItem() {
        final LockLevel defaultLockLevel = getDefaultLockLevel();

        final LockLevelComboItem[] items = getEnabledLockLevelItems();

        for (int i = 0; i < items.length; i++) {
            if (items[i].getLockLevel().equals(defaultLockLevel)) {
                return items[i];
            }
        }

        return CheckoutDialog.LOCK_LEVEL_UNCHANGED;
    }

    /**
     * @return the {@link LockLevel} most recently selected by the user
     */
    public LockLevel getLockLevel() {
        return lockLevel;
    }

    public static LockLevel getLockLevelFromDescription(final String description) {
        for (int i = 0; i < KNOWN_LOCK_LEVELS.length; i++) {
            if (KNOWN_LOCK_LEVELS[i].getDescription().equals(description)) {
                return KNOWN_LOCK_LEVELS[i].getLockLevel();
            }
        }

        return null;
    }

    public static String getDescriptionFromLockLevel(final LockLevel lockLevel) {
        for (int i = 0; i < KNOWN_LOCK_LEVELS.length; i++) {
            if (KNOWN_LOCK_LEVELS[i].getLockLevel().equals(lockLevel)) {
                return KNOWN_LOCK_LEVELS[i].getDescription();
            }
        }

        return null;
    }

    /**
     * @return the {@link TypedItemSpec}s which are checked in the dialog
     */
    public TypedItemSpec[] getCheckedTypedItemSpecs() {
        return itemSpecTable.getCheckedTypedItemSpecs();
    }

    /**
     * Return the LockLevel that is used by default in the checkout dialog.
     * Queries user preferences.
     */
    public static LockLevel getDefaultLockLevel() {
        final String defaultLockLevel = TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getString(
            UIPreferenceConstants.CHECKOUT_LOCK_LEVEL);

        if (UIPreferenceConstants.CHECKOUT_LOCK_LEVEL_CHECKOUT.equals(defaultLockLevel)) {
            return LockLevel.CHECKOUT;
        } else if (UIPreferenceConstants.CHECKOUT_LOCK_LEVEL_CHECKIN.equals(defaultLockLevel)) {
            return LockLevel.CHECKIN;
        }

        return LockLevel.UNCHANGED;
    }
}
