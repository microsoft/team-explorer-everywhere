// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerResizeListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.CheckinCommentChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;

public class TeamExplorerPendingChangesCommentSection extends TeamExplorerPendingChangesBaseSection {
    private Text textComment;
    private SizeConstrainedComposite textContainer;
    private CheckinCommentChangedListener listener;
    private TeamExplorerResizeListener resizeListener;

    public static final String COMMENT_TEXTBOX_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerPendingChangesCommentSection#commentTextBox"; //$NON-NLS-1$

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);
        SWTUtil.gridLayout(composite, 1, true, 0, 0);

        if (context.isConnectedToCollection()
            || (context.getDefaultRepository() != null
                && context.getDefaultRepository().getWorkspace() != null
                && context.getDefaultRepository().getWorkspace().getLocation() == WorkspaceLocation.LOCAL)) {
            final String comment = getModel().getComment();

            // Stuff this in a size constrained composite to encourage wrapping
            textContainer = new SizeConstrainedComposite(composite, SWT.NONE);
            toolkit.adapt(textContainer);

            // Text controls present in size constrained composite, enable
            // form-style borders, must have at least 2 pixel margins.
            toolkit.paintBordersFor(textContainer);

            final FillLayout fillLayout = new FillLayout();
            fillLayout.marginHeight = 2;
            fillLayout.marginWidth = 1;
            textContainer.setLayout(fillLayout);
            textContainer.setDefaultSize(SizeConstrainedComposite.STATIC, SWT.DEFAULT);

            textComment = toolkit.createText(textContainer, comment, SWT.MULTI | SWT.WRAP);
            AutomationIDHelper.setWidgetID(textComment, COMMENT_TEXTBOX_ID);

            // Allow tab to traverse focus out of the text box.
            textComment.addTraverseListener(new TraverseListener() {
                @Override
                public void keyTraversed(final TraverseEvent e) {
                    e.doit = true;
                }
            });

            textComment.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    TeamExplorerHelpers.relayoutIfResized(textContainer);

                    // Use the light weight update of the model for each
                    // keystroke. Using this update will not cause change events
                    // to be fired or the comment to be written to the local
                    // cache. The uncommitted comment when the comment is about
                    // to be read from the model or the text box loses focus.
                    // This is done to avoid a bug on MAC which occurs if the
                    // Check-in button is clicked while the focus is still in
                    // the comment text box. In this case, at least on some MAC
                    // versions of Eclipse, the button click event is raised
                    // before the focus out event. Since the comment is
                    // transferred and committed to the model on focus out, we
                    // can miss the comment on MAC. This change allows us to
                    // track the content of the comment text box without
                    // incurring the expense of committing it on each keystroke
                    // -- and the uncommitted comment will be picked up by
                    // check-in when the comment is read from the model.
                    getModel().setUncommittedComment(textComment.getText());
                }
            });

            textComment.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(final FocusEvent e) {
                    getModel().setComment(textComment.getText());
                }
            });

            textComment.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(final DisposeEvent e) {
                    getModel().setComment(textComment.getText());
                }
            });

            GridDataBuilder.newInstance().applyTo(textContainer);

            listener = new CommentChangedListener();
            getModel().addCheckinCommentChangedListener(listener);

            resizeListener = new TeamExplorerResizeListener(textContainer);
            context.getEvents().addListener(TeamExplorerEvents.FORM_RESIZED, resizeListener);
        } else {
            createDisconnectedContent(toolkit, composite);
        }

        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                if (listener != null) {
                    getModel().removeCheckinCommentChangedListener(listener);
                }
                if (resizeListener != null) {
                    context.getEvents().removeListener(TeamExplorerEvents.FORM_RESIZED, resizeListener);
                }
            }
        });

        return composite;
    }

    private class CommentChangedListener implements CheckinCommentChangedListener {
        @Override
        public void onCheckinCommentChanged(final String comment) {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    if (textComment.isDisposed()) {
                        return;
                    }

                    textComment.setText(comment);
                }
            });
        }
    }
}
