// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

import java.text.MessageFormat;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemType;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangesTable;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.util.Check;

public class SourceFilesCheckinControl extends AbstractCheckinSubControl {

    public static final String SOURCEFILES_TABLE_ID = "SourceFilesCheckinControl.changesTable"; //$NON-NLS-1$
    public static final String COMMENT_TEXT_ID = "SourceFilesCheckinControl.commentText"; //$NON-NLS-1$

    private final CheckinControlOptions options;

    private ChangesTable changesTable;
    private Text commentText;
    private final SashForm sashForm;

    private String comment;

    private IAction selectAllAction;
    private IAction copyAction;
    private IAction showHideCommentAction;
    private PropertyDialogAction propertiesAction;

    private TFSRepository repository;

    public SourceFilesCheckinControl(final Composite parent, final int style, final CheckinControlOptions options) {
        super(parent, style, Messages.getString("SourceFilesCheckinControl.Title"), CheckinSubControlType.SOURCE_FILES); //$NON-NLS-1$

        Check.notNull(options, "options"); //$NON-NLS-1$
        this.options = new CheckinControlOptions(options);

        final GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        setLayout(layout);

        final boolean commentOnTop = TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
            UIPreferenceConstants.PENDING_CHANGES_CONTROL_COMMENT_ON_TOP);

        if (options.getExternalContributionManager() == null) {
            if (!commentOnTop) {
                final String messageFormat = Messages.getString("SourceFilesCheckinControl.LabelTextFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, options.getChangesText());

                final Label label = new Label(this, SWT.NONE);
                label.setText(message);
                GridDataBuilder.newInstance().applyTo(label);
            }

            final ToolBar toolbar = new ToolBar(this, SWT.HORIZONTAL | SWT.RIGHT | SWT.FLAT);
            GridDataBuilder.newInstance().hGrab().hFill().hAlign(SWT.RIGHT).applyTo(toolbar);

            final IContributionManager contributionManager = new ToolBarManager(toolbar);
            setContributionManager(contributionManager);

            contributionManager.add(new Separator(StandardActionConstants.PRIVATE_CONTRIBUTIONS));
            contributionManager.add(new Separator(StandardActionConstants.HOSTING_CONTROL_CONTRIBUTIONS));
        }

        sashForm = new SashForm(this, SWT.VERTICAL);
        GridDataBuilder.newInstance().grab().fill().hSpan(2).applyTo(sashForm);

        int[] controlWeights;

        if (commentOnTop) {
            createCommentControl(sashForm);
            createPendingChangesTable(sashForm);
            controlWeights = new int[] {
                30,
                70
            };
        } else {
            createPendingChangesTable(sashForm);
            createCommentControl(sashForm);
            controlWeights = new int[] {
                70,
                30
            };
        }

        sashForm.setWeights(controlWeights);

        createActions();

        if (options.getExternalContributionManager() == null) {
            addContributions(getContributionManager(), StandardActionConstants.PRIVATE_CONTRIBUTIONS);
            getContributionManager().update(false);
        }
    }

    public void setRepository(final TFSRepository repository) {
        if (this.repository != repository) {
            if (repository == null && this.repository != null) {
                changesTable.setChangeItems(new ChangeItem[0], ChangeItemType.PENDING);
                commentText.setText(""); //$NON-NLS-1$
            }

            changesTable.setEnabled(repository != null);
            commentText.setEnabled(repository != null);
        }

        this.repository = repository;
    }

    @Override
    public boolean setFocus() {
        if (options.isSourceFilesCommentReadOnly()) {
            return changesTable.setFocus();
        } else {
            return commentText.setFocus();
        }
    }

    public Text getCommentText() {
        return commentText;
    }

    public ChangesTable getChangesTable() {
        return changesTable;
    }

    public String getComment() {
        if (comment != null && comment.trim().length() == 0) {
            return null;
        }
        return comment;
    }

    public void setComment(String comment) {
        if (comment == null) {
            comment = ""; //$NON-NLS-1$
        }
        commentText.setText(comment);
    }

    @Override
    public void addContributions(final IContributionManager contributionManager, final String groupName) {
        contributionManager.appendToGroup(groupName, showHideCommentAction);
    }

    @Override
    public void removeContributions(final IContributionManager contributionManager, final String groupname) {
        contributionManager.remove(new ActionContributionItem(showHideCommentAction));
    }

    private Composite createPendingChangesTable(final Composite parent) {
        int style = SWT.FULL_SELECTION | SWT.MULTI;
        if (options.isSourceFilesCheckboxes()) {
            style |= SWT.CHECK;
        }

        changesTable = new ChangesTable(parent, style);
        AutomationIDHelper.setWidgetID(changesTable, SOURCEFILES_TABLE_ID);

        changesTable.getContextMenu().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                fillMenu(manager);
            }
        });

        setSelectionProvider(changesTable);
        setContextMenu(changesTable.getContextMenu());

        return changesTable;
    }

    private void fillMenu(final IMenuManager manager) {
        manager.add(new Separator(StandardActionConstants.HOSTING_CONTROL_CONTRIBUTIONS));
        manager.add(new Separator(StandardActionConstants.PRIVATE_CONTRIBUTIONS));

        manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, propertiesAction);
    }

    private Composite createCommentControl(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);

        final GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 0;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        final Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("SourceFilesCheckinControl.CommentLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hIndent(4).vIndent(3).applyTo(label);

        int textStyle = SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL;
        if (options.isSourceFilesCommentReadOnly()) {
            textStyle |= SWT.READ_ONLY;
        }

        commentText = new Text(composite, textStyle);
        GridDataBuilder.newInstance().grab().fill().wHint(getMinimumMessageAreaWidth()).hCHint(commentText, 4).applyTo(
            commentText);
        AutomationIDHelper.setWidgetID(commentText, COMMENT_TEXT_ID);

        if (!options.isSourceFilesCommentReadOnly()) {
            commentText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    comment = commentText.getText();
                }
            });
        }

        return composite;
    }

    private void createActions() {
        selectAllAction = new Action() {
            @Override
            public void run() {
                changesTable.selectAll();
                changesTable.setFocus();
            }
        };

        copyAction = new Action() {
            @Override
            public void run() {
                changesTable.copySelectionToClipboard();
            }
        };

        showHideCommentAction = new Action() {
            @Override
            public void run() {
                if (sashForm.getMaximizedControl() == null) {
                    sashForm.setMaximizedControl(changesTable);
                    setChecked(false);
                } else {
                    sashForm.setMaximizedControl(null);
                    setChecked(true);
                }
            }
        };
        showHideCommentAction.setToolTipText(Messages.getString("SourceFilesCheckinControl.ShowHideActionTooltip")); //$NON-NLS-1$
        showHideCommentAction.setImageDescriptor(
            AbstractUIPlugin.imageDescriptorFromPlugin(
                TFSCommonUIClientPlugin.PLUGIN_ID,
                "images/vc/sourcefiles_showhidecomment.gif")); //$NON-NLS-1$
        showHideCommentAction.setChecked(true);

        /*
         * must use the deprecated constructor of PropertyDialogAction for 3.0
         * compatibility
         */
        propertiesAction = new PropertyDialogAction(getShell(), changesTable);
        propertiesAction.setEnabled(false);

        registerGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), selectAllAction);
        registerGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
        registerGlobalActionHandler(ActionFactory.PROPERTIES.getId(), propertiesAction);
    }

    public void afterCheckin() {
        commentText.setText(""); //$NON-NLS-1$
    }
}
