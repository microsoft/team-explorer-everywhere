// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerResizeListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.CheckinNoteFieldDefinitionsChangedListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.CheckinNoteFieldValuesChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldDefinition;

public class TeamExplorerPendingChangesNotesSection extends TeamExplorerPendingChangesBaseSection {
    private static final String CHECKIN_NOTE_FIELD_DEFINITION_TAG = "checkinNoteFieldDefinition"; //$NON-NLS-1$

    private final List<Text> textNotes = new ArrayList<Text>();
    private FieldDefinitionsChangedListener definitionsChangedListener;
    private FieldValuesChangedListener valuesChangedListener;

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        // Always visible disconnected for consistency (since we can't
        // accurately count the field definitions)
        return !context.isConnected() || getCheckinNoteFieldDefinitionCount() > 0;
    }

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);
        final GridLayout gridLayout = SWTUtil.gridLayout(composite, 1, true, 0, 5);

        // Allocated resize listeners.
        final List<TeamExplorerResizeListener> resizeListeners = new ArrayList<TeamExplorerResizeListener>();

        // Reduce the spacing between controls in this section.
        gridLayout.verticalSpacing = 2;

        if (context.isConnectedToCollection()) {
            final CheckinNoteFieldDefinition[] notes = getModel().getCheckinNoteFieldDefinitions();
            final Color requiredBackgroundColor = composite.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
            final FormColors colors = toolkit.getColors();

            for (int i = 0; i < notes.length; i++) {
                final CheckinNoteFieldDefinition note = notes[i];

                // Create the label for this check-in note.
                final Label labelNote = toolkit.createLabel(composite, note.getName());
                GridDataBuilder.newInstance().applyTo(labelNote);

                // Create a container that will encourage this text note to wrap
                final SizeConstrainedComposite textContainer = new SizeConstrainedComposite(composite, SWT.NONE);
                toolkit.adapt(textContainer);

                // Text controls present in size constrained composite, enable
                // form-style borders, must have at least 2 pixel margins.
                toolkit.paintBordersFor(textContainer);

                final FillLayout fillLayout = new FillLayout();
                fillLayout.marginHeight = 2;
                fillLayout.marginWidth = 1;
                textContainer.setLayout(fillLayout);
                textContainer.setDefaultSize(SizeConstrainedComposite.STATIC, SWT.DEFAULT);

                // Create the text box for this check-in note.
                final Text textNote = toolkit.createText(textContainer, "", SWT.MULTI | SWT.WRAP); //$NON-NLS-1$

                // Tag the control so we can identify its note when updating
                textNote.setData(CHECKIN_NOTE_FIELD_DEFINITION_TAG, note);

                // Allow tab to traverse focus out of the text box.
                textNote.addTraverseListener(new TraverseListener() {
                    @Override
                    public void keyTraversed(final TraverseEvent e) {
                        e.doit = true;
                    }
                });

                // Set the initial value if there is one.
                final String initialValue = getModel().getCheckinNoteFieldValue(note.getName());

                if (initialValue != null) {
                    textNote.setText(initialValue);
                }

                // Set the background color is there is no initial value on a
                // required field.
                if ((initialValue == null || initialValue.length() == 0) && note.isRequired()) {
                    textNote.setBackground(requiredBackgroundColor);
                }

                // Listen for text modified events.
                textNote.addModifyListener(new ModifyListener() {
                    @Override
                    public void modifyText(final ModifyEvent e) {
                        if (note.isRequired()) {
                            final int len = textNote.getText().length();

                            if (textNote.getBackground().equals(requiredBackgroundColor)) {
                                if (len > 0) {
                                    textNote.setBackground(colors.getBackground());
                                }
                            } else {
                                if (len == 0) {
                                    textNote.setBackground(requiredBackgroundColor);
                                }
                            }
                        }

                        TeamExplorerHelpers.relayoutIfResized(textContainer);
                    }
                });

                textNote.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(final FocusEvent e) {
                        getModel().setCheckinNoteFieldValue(note.getName(), textNote.getText());
                    }
                });

                textNote.addDisposeListener(new DisposeListener() {
                    @Override
                    public void widgetDisposed(final DisposeEvent e) {
                        getModel().setCheckinNoteFieldValue(note.getName(), textNote.getText());
                    }
                });

                // Add the new textbox to the list of notes
                textNotes.add(textNote);

                // Set the layout data for the text box.
                GridDataBuilder.newInstance().applyTo(textContainer);

                // Add a resize listener.
                final TeamExplorerResizeListener listener = new TeamExplorerResizeListener(textContainer);
                resizeListeners.add(listener);
                context.getEvents().addListener(TeamExplorerEvents.FORM_RESIZED, listener);
            }

            definitionsChangedListener = new FieldDefinitionsChangedListener();
            valuesChangedListener = new FieldValuesChangedListener();
            getModel().addCheckinNoteFieldDefinitionsChangedListener(definitionsChangedListener);
            getModel().addCheckinNoteFieldValuesChangedListener(valuesChangedListener);
        } else {
            createDisconnectedContent(toolkit, composite);
        }

        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                if (definitionsChangedListener != null) {
                    getModel().removeCheckinNoteFieldDefinitionsChangedListener(definitionsChangedListener);
                }
                if (valuesChangedListener != null) {
                    getModel().removeCheckinNoteFieldValuesChangedListener(valuesChangedListener);
                }
                for (final TeamExplorerResizeListener listener : resizeListeners) {
                    context.getEvents().removeListener(TeamExplorerEvents.FORM_RESIZED, listener);
                }
            }
        });

        return composite;
    }

    @Override
    public String getTitle() {
        final int count = getCheckinNoteFieldDefinitionCount();

        // This method can be called before the section has been initialized, so
        // the context may be null
        if (getContext() == null || !getContext().isConnected() || count == 0) {
            return baseTitle;
        }

        final String format = Messages.getString("TeamExplorerCommon.TitleWithCountFormat"); //$NON-NLS-1$
        return MessageFormat.format(format, baseTitle, count);
    }

    public int getCheckinNoteFieldDefinitionCount() {
        return getModel() == null ? 0 : getModel().getCheckinNoteFieldDefinitionCount();
    }

    private class FieldDefinitionsChangedListener implements CheckinNoteFieldDefinitionsChangedListener {
        @Override
        public void onCheckinNoteFieldDefinitionsChanged() {
            fireSectionRegenerateEvent();
        }
    }

    private class FieldValuesChangedListener implements CheckinNoteFieldValuesChangedListener {
        @Override
        public void onCheckinNoteFieldValuesChanged() {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    for (final Text textNote : textNotes) {
                        if (!textNote.isDisposed()) {
                            String value = null;

                            final CheckinNoteFieldDefinition definition =
                                (CheckinNoteFieldDefinition) textNote.getData(CHECKIN_NOTE_FIELD_DEFINITION_TAG);
                            if (definition != null) {
                                value = getModel().getCheckinNoteFieldValue(definition.getName());
                            }

                            if (value == null) {
                                value = ""; //$NON-NLS-1$
                            }

                            textNote.setText(value);
                        }
                    }
                }
            });
        }
    }
}
