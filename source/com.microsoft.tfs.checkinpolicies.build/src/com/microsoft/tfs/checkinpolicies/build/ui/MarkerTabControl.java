// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.build.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.checkinpolicies.build.Messages;
import com.microsoft.tfs.checkinpolicies.build.settings.BuildPolicyConfiguration;
import com.microsoft.tfs.checkinpolicies.build.settings.MarkerMatch;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementEvent;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementListener;
import com.microsoft.tfs.util.Check;

public class MarkerTabControl extends BaseControl {
    private final BuildPolicyConfiguration configuration;

    private final MarkerListControl markerControl;

    private final Label currentlyEditing;

    private final Label markerTypeLabel;
    private final Text markerTypeText;
    private final Button includeSubtypesButton;

    private final Label commentLabel;
    private final Text commentText;

    private final Label severityLabel;
    private final Label priorityLabel;

    private final Button severityErrorButton;
    private final Button severityWarningButton;
    private final Button severityInfoButton;

    private final Button priorityHighButton;
    private final Button priorityNormalButton;
    private final Button priorityLowButton;

    private final Button[] allButtons;

    private final List italicLabels = new ArrayList();
    private final List italicFonts = new ArrayList();

    private class CheckClickHandler extends SelectionAdapter {
        @Override
        public void widgetSelected(final SelectionEvent e) {
            final Button button = (Button) e.getSource();
            final boolean selected = ((Button) e.getSource()).getSelection();
            final MarkerMatch marker = getSelectedMarker();

            if (marker == null) {
                return;
            }

            if (button == includeSubtypesButton) {
                marker.setIncludeSubtypes(selected);
            } else if (button == severityErrorButton) {
                marker.setSeverityError(selected);
            } else if (button == severityWarningButton) {
                marker.setSeverityWarning(selected);
            } else if (button == severityInfoButton) {
                marker.setSeverityInfo(selected);
            } else if (button == priorityHighButton) {
                marker.setPriorityHigh(selected);
            } else if (button == priorityNormalButton) {
                marker.setPriorityNormal(selected);
            } else if (button == priorityLowButton) {
                marker.setPriorityLow(selected);
            }
        }
    }

    public MarkerTabControl(final Composite parent, final int style, final BuildPolicyConfiguration configuration) {
        super(parent, style);

        Check.notNull(configuration, "configuration"); //$NON-NLS-1$
        this.configuration = configuration;

        SWTUtil.gridLayout(this);

        final Label label =
            SWTUtil.createLabel(this, SWT.WRAP, Messages.getString("MarkerTabControl.SummaryLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hGrab().wHint(getMinimumMessageAreaWidth()).applyTo(label);

        // Marker control.

        markerControl = new MarkerListControl(this, SWT.NONE);
        GridDataBuilder.newInstance().grab().fill().applyTo(markerControl);
        ControlSize.setCharHeightHint(markerControl, 6);
        markerControl.getTable().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                refreshEditControls();
            }
        });
        markerControl.getTable().addElementListener(new ElementListener() {
            @Override
            public void elementsChanged(final ElementEvent event) {
                MarkerTabControl.this.configuration.setMarkers(markerControl.getMarkers());
            }
        });

        // Separator and Edit composite.

        final Label separator = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(separator);

        currentlyEditing = new Label(this, SWT.NONE);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(currentlyEditing);

        final Composite currentComposite = SWTUtil.createComposite(this);
        SWTUtil.gridLayout(currentComposite, 2);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(currentComposite);

        // Comment.

        commentLabel =
            SWTUtil.createLabel(currentComposite, SWT.WRAP, Messages.getString("MarkerTabControl.CommentLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().applyTo(commentLabel);

        commentText = new Text(currentComposite, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(commentText);
        ControlSize.setCharHeightHint(commentText, 3);
        commentText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                final MarkerMatch marker = MarkerTabControl.this.getSelectedMarker();

                if (marker == null) {
                    return;
                }

                marker.setComment(commentText.getText());
            }
        });

        // "Validate"

        final Label validateLabel =
            SWTUtil.createLabel(currentComposite, SWT.WRAP, Messages.getString("MarkerTabControl.ValidateLabelText")); //$NON-NLS-1$
        styleItalic(validateLabel);
        GridDataBuilder.newInstance().hSpan(2).applyTo(validateLabel);

        // Type.

        markerTypeLabel =
            SWTUtil.createLabel(currentComposite, SWT.WRAP, Messages.getString("MarkerTabControl.MarkerTypeLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hIndent(20).applyTo(markerTypeLabel);

        markerTypeText = new Text(currentComposite, SWT.BORDER);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(markerTypeText);
        markerTypeText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                final MarkerMatch marker = MarkerTabControl.this.getSelectedMarker();

                if (marker == null) {
                    return;
                }

                marker.setMarkerType(markerTypeText.getText());

                markerControl.refreshTable();
            }
        });

        final CheckClickHandler checkClickHandler = new CheckClickHandler();

        // Empty cell.
        SWTUtil.createLabel(currentComposite);

        // Subtypes.
        includeSubtypesButton = SWTUtil.createButton(
            currentComposite,
            SWT.CHECK,
            Messages.getString("MarkerTabControl.IncludeSubtypesLabelText")); //$NON-NLS-1$
        includeSubtypesButton.addSelectionListener(checkClickHandler);
        GridDataBuilder.newInstance().hFill().hGrab().hAlign(SWT.LEFT).applyTo(includeSubtypesButton);

        // And...
        Label andLabel =
            SWTUtil.createLabel(currentComposite, SWT.WRAP, Messages.getString("MarkerTabControl.AndLabelText")); //$NON-NLS-1$
        styleItalic(andLabel);
        GridDataBuilder.newInstance().hSpan(2).applyTo(andLabel);

        // Labels for checks.

        severityLabel =
            SWTUtil.createLabel(currentComposite, SWT.WRAP, Messages.getString("MarkerTabControl.SeverityLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hIndent(20).applyTo(severityLabel);

        // Severity.

        final Composite severityComposite = SWTUtil.createComposite(currentComposite);
        SWTUtil.gridLayout(severityComposite, 3);

        severityErrorButton = SWTUtil.createButton(
            severityComposite,
            SWT.CHECK,
            Messages.getString("MarkerTabControl.SeverityErrorButtonText")); //$NON-NLS-1$
        severityErrorButton.addSelectionListener(checkClickHandler);

        severityWarningButton = SWTUtil.createButton(
            severityComposite,
            SWT.CHECK,
            Messages.getString("MarkerTabControl.SeverityWarningButtonText")); //$NON-NLS-1$
        severityWarningButton.addSelectionListener(checkClickHandler);

        severityInfoButton = SWTUtil.createButton(
            severityComposite,
            SWT.CHECK,
            Messages.getString("MarkerTabControl.SeverityInfoButtonText")); //$NON-NLS-1$
        severityInfoButton.addSelectionListener(checkClickHandler);

        // And...
        andLabel = SWTUtil.createLabel(currentComposite, SWT.WRAP, Messages.getString("MarkerTabControl.AndLabelText")); //$NON-NLS-1$
        styleItalic(andLabel);
        GridDataBuilder.newInstance().hSpan(2).applyTo(andLabel);

        // Priority.

        priorityLabel =
            SWTUtil.createLabel(currentComposite, SWT.WRAP, Messages.getString("MarkerTabControl.PriorityLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hIndent(20).applyTo(priorityLabel);

        final Composite priorityComposite = SWTUtil.createComposite(currentComposite);
        SWTUtil.gridLayout(priorityComposite, 3);

        priorityHighButton = SWTUtil.createButton(
            priorityComposite,
            SWT.CHECK,
            Messages.getString("MarkerTabControl.PriorityHighLabelText")); //$NON-NLS-1$
        priorityHighButton.addSelectionListener(checkClickHandler);

        priorityNormalButton = SWTUtil.createButton(
            priorityComposite,
            SWT.CHECK,
            Messages.getString("MarkerTabControl.PriorityNormalLabelText")); //$NON-NLS-1$
        priorityNormalButton.addSelectionListener(checkClickHandler);

        priorityLowButton = SWTUtil.createButton(
            priorityComposite,
            SWT.CHECK,
            Messages.getString("MarkerTabControl.PriorityLowLabelText")); //$NON-NLS-1$
        priorityLowButton.addSelectionListener(checkClickHandler);

        allButtons = new Button[] {
            severityErrorButton,
            severityWarningButton,
            severityInfoButton,

            priorityHighButton,
            priorityNormalButton,
            priorityLowButton
        };

        refreshMarkerControl();

        if (markerControl.getTable().getMarkers().length > 0) {
            markerControl.getTable().setSelectedMarker(markerControl.getTable().getMarkers()[0]);
        }

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                for (int i = 0; i < italicFonts.size(); i++) {
                    final Font font = (Font) italicFonts.get(i);
                    font.dispose();
                }
                italicFonts.clear();
            }
        });

        refreshEditControls();
    }

    private void styleItalic(final Label label) {
        final FontData[] fontData = label.getFont().getFontData();
        for (int i = 0; i < fontData.length; i++) {
            fontData[i].setStyle(fontData[i].getStyle() | SWT.ITALIC);
        }

        final Font font = new Font(getDisplay(), fontData);
        label.setFont(font);

        italicFonts.add(font);
        italicLabels.add(label);
    }

    private void refreshMarkerControl() {
        markerControl.setMarkers(configuration.getMarkers());
    }

    /**
     * Refreshes controls from data in this class's model (private fields).
     */
    private void refreshEditControls() {
        final MarkerMatch m = markerControl.getTable().getSelectedMarker();

        if (m == null) {
            // Disable.
            currentlyEditing.setEnabled(false);
            includeSubtypesButton.setEnabled(false);
            markerTypeLabel.setEnabled(false);
            markerTypeText.setEnabled(false);
            commentLabel.setEnabled(false);
            commentText.setEnabled(false);
            priorityLabel.setEnabled(false);
            severityLabel.setEnabled(false);
            setButtonsEnabled(allButtons, false);
            setItalicLabelsEnabled(false);

            // Clear data.
            currentlyEditing.setText(Messages.getString("MarkerTabControl.NoMarkerSelected")); //$NON-NLS-1$
            markerTypeText.setText(""); //$NON-NLS-1$
            includeSubtypesButton.setSelection(false);
            commentText.setText(""); //$NON-NLS-1$
            setButtons(allButtons, false);

            return;
        }

        // Enable.
        currentlyEditing.setEnabled(true);
        includeSubtypesButton.setEnabled(true);
        commentLabel.setEnabled(true);
        commentText.setEnabled(true);
        markerTypeLabel.setEnabled(true);
        markerTypeText.setEnabled(true);
        priorityLabel.setEnabled(true);
        severityLabel.setEnabled(true);
        setButtonsEnabled(allButtons, true);
        setItalicLabelsEnabled(true);

        // Set data from model.
        currentlyEditing.setText(Messages.getString("MarkerTabControl.MarkerAttributes")); //$NON-NLS-1$

        markerTypeText.setText(getSelectedMarker().getMarkerType());
        includeSubtypesButton.setSelection(getSelectedMarker().isIncludeSubtypes());

        commentText.setText(getSelectedMarker().getComment());

        setButtonsFromModel();
    }

    private void setItalicLabelsEnabled(final boolean enabled) {
        for (int i = 0; i < italicLabels.size(); i++) {
            final Label l = (Label) italicLabels.get(i);
            l.setEnabled(enabled);
        }
    }

    private void setButtonsEnabled(final Button[] buttons, final boolean enabled) {
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setEnabled(enabled);
        }
    }

    private MarkerMatch getSelectedMarker() {
        return markerControl.getTable().getSelectedMarker();
    }

    private void setButtonsFromModel() {
        final MarkerMatch marker = getSelectedMarker();

        if (marker == null) {
            return;
        }

        final Button[] buttons = allButtons;

        for (int i = 0; i < buttons.length; i++) {
            boolean selected = false;

            if (buttons[i] == includeSubtypesButton) {
                selected = marker.isIncludeSubtypes();
            } else if (buttons[i] == severityErrorButton) {
                selected = marker.isSeverityError();
            } else if (buttons[i] == severityWarningButton) {
                selected = marker.isSeverityWarning();
            } else if (buttons[i] == severityInfoButton) {
                selected = marker.isSeverityInfo();
            } else if (buttons[i] == priorityHighButton) {
                selected = marker.isPriorityHigh();
            } else if (buttons[i] == priorityNormalButton) {
                selected = marker.isPriorityNormal();
            } else if (buttons[i] == priorityLowButton) {
                selected = marker.isPriorityLow();
            }

            buttons[i].setSelection(selected);
        }
    }

    private void setButtons(final Button[] buttons, final boolean selected) {
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setSelection(selected);
        }
    }
}
