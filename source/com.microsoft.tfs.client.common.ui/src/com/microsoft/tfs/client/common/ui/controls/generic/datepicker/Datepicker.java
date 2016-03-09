// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.datepicker;

import java.text.DateFormatSymbols;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TypedListener;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;

public class Datepicker extends Composite implements MouseMoveListener, MouseListener, DisposeListener, KeyListener {
    private final Calendar defaultDate = Calendar.getInstance();
    private final Calendar selectedDate = Calendar.getInstance();
    private Point spacing = new Point(0, 2);

    private final Label monthLabel = new Label(this, SWT.NONE);

    int weekdayStringLength;
    int dayAlignment;
    boolean drawOtherDays = false;

    int rows;
    int firstDayOfWeek;
    int lastDayOfWeek;

    private final Label[] weekdayLabel;
    private final DatepickerDate[][] dayControl;
    DatepickerDate focusedControl = null;

    private Font font = null;
    private Font fontBold = null;
    private Control headerBottom = null;

    boolean mouseIsDown = false;

    public Datepicker(final Composite parent, final int style) {
        super(parent, style);

        selectedDate.setTime(defaultDate.getTime());

        firstDayOfWeek = selectedDate.getActualMinimum(Calendar.DAY_OF_WEEK);
        lastDayOfWeek = selectedDate.getActualMaximum(Calendar.DAY_OF_WEEK);

        // rows is maximum days in month divided by days in week plus 1 (ie, 6
        // in us locale...)
        rows = (selectedDate.getMaximum(Calendar.DAY_OF_MONTH) / (lastDayOfWeek - firstDayOfWeek)) + 1;

        // a one dimensional array to hold the labels for the day names
        weekdayLabel = new Label[(lastDayOfWeek - firstDayOfWeek) + 1];

        // create the daycontrol array, since the array itself is static
        // dayControl is a two dimensional array, such that it's rows
        // (corresponding to
        // a calendar row) by columns (corresponding to days of the week)
        dayControl = new DatepickerDate[rows][];
        for (int i = 0; i < rows; i++) {
            dayControl[i] = new DatepickerDate[(lastDayOfWeek - firstDayOfWeek) + 1];
        }

        addMouseListener(this);
        addMouseMoveListener(this);
        addKeyListener(this);
        addDisposeListener(this);

        font = new Font(getDisplay(), getDisplay().getSystemFont().getFontData());
        setFont(font);

        final FontData fontBoldData = font.getFontData()[0];
        fontBoldData.setStyle(fontBoldData.getStyle() | SWT.BOLD);
        fontBold = new Font(getDisplay(), fontBoldData);

        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            configureMacOSX();
        } else {
            configureOther();
        }

        layoutCalendar();
        drawSelectedMonth();
    }

    private void configureMacOSX() {
        final FontData fontData = font.getFontData()[0];
        fontData.setHeight(11);
        font.dispose();
        font = new Font(getDisplay(), fontData);

        final FontData fontBoldData = fontBold.getFontData()[0];
        fontBoldData.setHeight(11);
        fontBold.dispose();
        fontBold = new Font(getDisplay(), fontBoldData);

        setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));

        spacing = new Point(0, 2);

        weekdayStringLength = 1;
        dayAlignment = SWT.RIGHT;
        drawOtherDays = false;
    }

    private void configureOther() {
        spacing = new Point(0, 0);

        weekdayStringLength = 3;
        dayAlignment = SWT.CENTER;
        drawOtherDays = true;
    }

    private void layoutCalendar() {
        final FormLayout formLayout = new FormLayout();
        formLayout.spacing = 0;
        setLayout(formLayout);

        // create the date objects
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= (lastDayOfWeek - firstDayOfWeek); j++) {
                dayControl[i][j] = new DatepickerDate(this, SWT.NONE);
            }
        }

        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            layoutHeaderMacOSX();
        } else {
            layoutHeaderOther();
        }

        /*
         * find the largest width possible for the month label and set the width
         * of the label to that.
         */
        final GC gc = new GC(getDisplay());
        gc.setFont(monthLabel.getFont());

        final DateFormatSymbols dateSymbols = new DateFormatSymbols();
        final String monthSymbols[] = dateSymbols.getMonths();
        int monthLabelWidth = 0;

        for (int i = 0; i < monthSymbols.length; i++) {
            final String monthText =
                MessageFormat.format(
                    Messages.getString("Datepicker.HeaderStringMonthSpaceYearFormat"), //$NON-NLS-1$
                    monthSymbols[i],
                    "9999"); //$NON-NLS-1$
            monthLabel.setText(monthText);

            monthLabelWidth = Math.max(monthLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, monthLabelWidth);
        }

        ((FormData) monthLabel.getLayoutData()).width = monthLabelWidth;

        // lay out the dayControls
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= (lastDayOfWeek - firstDayOfWeek); j++) {
                final FormData blankControlData = new FormData();
                blankControlData.top = (i == 0) ? new FormAttachment(headerBottom, 0, SWT.BOTTOM)
                    : new FormAttachment(dayControl[(i - 1)][0], spacing.y, SWT.BOTTOM);
                blankControlData.left = (j == 0) ? new FormAttachment(0, 0)
                    : new FormAttachment(dayControl[i][(j - 1)], spacing.x, SWT.RIGHT);
                dayControl[i][j].setLayoutData(blankControlData);
                dayControl[i][j].setFont(font);
                dayControl[i][j].setAlignment(dayAlignment);

                if (j == 0) {
                    dayControl[i][j].setShape(DatepickerDate.SHAPE_LEFT);
                } else if (j == (lastDayOfWeek - firstDayOfWeek)) {
                    dayControl[i][j].setShape(DatepickerDate.SHAPE_RIGHT);
                }

                dayControl[i][j].addMouseListener(this);
                dayControl[i][j].addMouseMoveListener(this);
                dayControl[i][j].addKeyListener(this);
            }
        }

        layout(true);
        pack();
    }

    private void layoutHeaderMacOSX() {
        final DatepickerButton prevButton = new DatepickerButton(this, SWT.NONE);
        final FormData prevButtonData = new FormData();
        prevButtonData.top = new FormAttachment(0, 0);
        prevButtonData.left = new FormAttachment(0, 0);
        prevButton.setLayoutData(prevButtonData);
        prevButton.setType(DatepickerButton.BUTTON_PREV);
        prevButton.setFont(font);
        prevButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedDate.add(Calendar.MONTH, -1);
                drawSelectedMonth();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
            }
        });

        final DatepickerButton defaultButton = new DatepickerButton(this, SWT.NONE);
        final FormData defaultButtonData = new FormData();
        defaultButtonData.top = new FormAttachment(0, 0);
        defaultButtonData.left = new FormAttachment(prevButton, 1, SWT.RIGHT);
        defaultButton.setLayoutData(defaultButtonData);
        defaultButton.setType(DatepickerButton.BUTTON_DEFAULT);
        defaultButton.setFont(font);
        defaultButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedDate.setTime(defaultDate.getTime());
                drawSelectedMonth();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
            }
        });

        final DatepickerButton nextButton = new DatepickerButton(this, SWT.NONE);
        final FormData nextButtonData = new FormData();
        nextButtonData.top = new FormAttachment(0, 0);
        nextButtonData.left = new FormAttachment(defaultButton, 1, SWT.RIGHT);
        nextButton.setLayoutData(nextButtonData);
        nextButton.setType(DatepickerButton.BUTTON_NEXT);
        nextButton.setFont(font);
        nextButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedDate.add(Calendar.MONTH, 1);
                drawSelectedMonth();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
            }
        });

        final FormData monthLabelData = new FormData();
        monthLabelData.top = new FormAttachment(0, 0);
        monthLabelData.left = new FormAttachment(nextButton, 1, SWT.RIGHT);
        monthLabelData.right = new FormAttachment(100, 0);
        monthLabel.setLayoutData(monthLabelData);
        monthLabel.setAlignment(SWT.RIGHT);
        monthLabel.setBackground(getBackground());
        monthLabel.setFont(fontBold);

        final DateFormatSymbols dateSymbols = new DateFormatSymbols();
        final String weekdaySymbols[] = dateSymbols.getShortWeekdays();

        for (int i = 0; i <= (lastDayOfWeek - firstDayOfWeek); i++) {
            // Handle locales with different first days of the week.
            int dayOfWeek = selectedDate.getFirstDayOfWeek() + i;

            if (dayOfWeek > lastDayOfWeek) {
                dayOfWeek -= (lastDayOfWeek - firstDayOfWeek) + 1;
            }

            /*
             * Mac has a 2px padding width in labels (between the text and the
             * edge of the label container.) Also add 3 pixels (datepicker
             * date's round corner.
             */
            final int rightOffset = (i == (lastDayOfWeek - firstDayOfWeek)) ? 3 : 1;

            weekdayLabel[i] = new Label(this, SWT.NONE);
            final FormData weekdayLabelData = new FormData();
            weekdayLabelData.top = new FormAttachment(monthLabel, 3, SWT.BOTTOM);
            weekdayLabelData.left = new FormAttachment(dayControl[0][i], 0, SWT.LEFT);
            weekdayLabelData.right = new FormAttachment(dayControl[0][i], (0 - rightOffset), SWT.RIGHT);
            weekdayLabel[i].setLayoutData(weekdayLabelData);
            weekdayLabel[i].setFont(fontBold);
            weekdayLabel[i].setText(weekdaySymbols[dayOfWeek].substring(0, 1));
            weekdayLabel[i].setBackground(getBackground());
            weekdayLabel[i].setAlignment(SWT.RIGHT);
        }

        headerBottom = weekdayLabel[0];
    }

    private void layoutHeaderOther() {
        /*
         * General color strategy for Windows and Unix is: don't set any
         * explicit colors; let the default widget, label, and button colors do
         * their thing.
         */

        final Button prevButton = new Button(this, SWT.ARROW | SWT.LEFT);
        final Button nextButton = new Button(this, SWT.ARROW | SWT.RIGHT);

        final Canvas spacer1 = new Canvas(this, SWT.NONE);
        final FormData spacer1Data = new FormData();
        spacer1Data.top = new FormAttachment(0, 0);
        spacer1Data.bottom = new FormAttachment(0, 7);
        spacer1Data.left = new FormAttachment(0, 0);
        spacer1Data.right = new FormAttachment(100, 0);
        spacer1.setSize(7, 7);
        spacer1.setLayoutData(spacer1Data);

        final Canvas spacer2 = new Canvas(this, SWT.NONE);
        final FormData spacer2Data = new FormData();
        spacer2Data.top = new FormAttachment(prevButton, 0, SWT.TOP);
        spacer2Data.bottom = new FormAttachment(prevButton, 0, SWT.BOTTOM);
        spacer2Data.left = new FormAttachment(0, 0);
        spacer2Data.right = new FormAttachment(0, 7);
        spacer2.setSize(7, 7);
        spacer2.setLayoutData(spacer2Data);

        // prevButton
        final FormData prevButtonData = new FormData();
        prevButtonData.top = new FormAttachment(spacer1, 0, SWT.BOTTOM);
        prevButtonData.left = new FormAttachment(spacer2, 0, SWT.RIGHT);
        prevButton.setLayoutData(prevButtonData);
        prevButton.setFont(font);
        prevButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedDate.add(Calendar.MONTH, -1);
                drawSelectedMonth();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
            }
        });

        final Canvas spacer3 = new Canvas(this, SWT.NONE);
        final FormData spacer3Data = new FormData();
        spacer3Data.top = new FormAttachment(nextButton, 0, SWT.TOP);
        spacer3Data.bottom = new FormAttachment(nextButton, 0, SWT.BOTTOM);
        spacer3Data.left = new FormAttachment(100, -7);
        spacer3Data.right = new FormAttachment(100, 0);
        spacer3.setSize(7, 7);
        spacer3.setLayoutData(spacer3Data);

        // nextButton
        final FormData nextButtonData = new FormData();
        nextButtonData.top = new FormAttachment(spacer1, 0, SWT.BOTTOM);
        nextButtonData.right = new FormAttachment(spacer3, 0, SWT.LEFT);
        nextButton.setLayoutData(nextButtonData);
        nextButton.setFont(font);
        nextButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedDate.add(Calendar.MONTH, 1);
                drawSelectedMonth();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
            }
        });

        final FormData monthLabelData = new FormData();
        monthLabelData.top = new FormAttachment(spacer1, 0, SWT.BOTTOM);
        monthLabelData.bottom = new FormAttachment(nextButton, 0, SWT.BOTTOM);
        monthLabelData.left = new FormAttachment(prevButton, 0, SWT.RIGHT);
        monthLabelData.right = new FormAttachment(nextButton, 0, SWT.LEFT);
        monthLabel.setLayoutData(monthLabelData);
        monthLabel.setAlignment(SWT.CENTER);
        monthLabel.setFont(fontBold);

        final Canvas spacer4 = new Canvas(this, SWT.NONE);
        final FormData spacer4Data = new FormData();
        spacer4Data.top = new FormAttachment(monthLabel, 0, SWT.BOTTOM);
        spacer4Data.bottom = new FormAttachment(monthLabel, 7, SWT.BOTTOM);
        spacer4Data.left = new FormAttachment(0, 0);
        spacer4Data.right = new FormAttachment(100, 0);
        spacer4.setSize(7, 7);
        spacer4.setLayoutData(spacer4Data);

        final DateFormatSymbols dateSymbols = new DateFormatSymbols();
        final String weekdaySymbols[] = dateSymbols.getShortWeekdays();

        for (int i = 0; i <= (lastDayOfWeek - firstDayOfWeek); i++) {
            // Handle locales with different first days of the week.
            int dayOfWeek = selectedDate.getFirstDayOfWeek() + i;
            if (dayOfWeek > lastDayOfWeek) {
                dayOfWeek -= (lastDayOfWeek - firstDayOfWeek) + 1;
            }

            weekdayLabel[i] = new Label(this, SWT.NONE);
            final FormData weekdayLabelData = new FormData();
            weekdayLabelData.top = new FormAttachment(spacer4, 3, SWT.BOTTOM);
            weekdayLabelData.left = new FormAttachment(dayControl[0][i], 0, SWT.LEFT);
            weekdayLabelData.right = new FormAttachment(dayControl[0][i], 0, SWT.RIGHT);
            weekdayLabel[i].setLayoutData(weekdayLabelData);
            weekdayLabel[i].setFont(font);
            weekdayLabel[i].setText(
                weekdaySymbols[dayOfWeek].substring(
                    0,
                    weekdaySymbols[dayOfWeek].length() > 3 ? 3 : weekdaySymbols[dayOfWeek].length()));
            weekdayLabel[i].setAlignment(SWT.CENTER);
        }

        final Label separator = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
        final FormData separatorData = new FormData();
        separatorData.top = new FormAttachment(weekdayLabel[0], 0, SWT.BOTTOM);
        separatorData.left = new FormAttachment(weekdayLabel[0], 3, SWT.LEFT);
        separatorData.right = new FormAttachment(weekdayLabel[6], -3, SWT.RIGHT);
        separator.setLayoutData(separatorData);

        headerBottom = separator;
    }

    private void drawSelectedMonth() {
        int row = 0;

        // set the proper date label
        final DateFormatSymbols dateSymbols = new DateFormatSymbols();
        final String monthSymbols[] = dateSymbols.getMonths();

        /**
         * Convert year to a string so format() doesn't use the default number
         * format (insert commas in the thousands, etc.).
         */
        monthLabel.setText(MessageFormat.format(
            Messages.getString("Datepicker.HeaderStringMonthSpaceYearFormat"), //$NON-NLS-1$
            monthSymbols[selectedDate.get(Calendar.MONTH)],
            Integer.toString(selectedDate.get(Calendar.YEAR))));

        // setup a calendar object at the first day of this month
        final Calendar dayIterator = Calendar.getInstance();
        dayIterator.setTime(selectedDate.getTime());
        dayIterator.set(Calendar.DAY_OF_MONTH, 1);

        // and a calendar at the last month...
        final Calendar lastMonth = Calendar.getInstance();
        lastMonth.setTime(selectedDate.getTime());
        lastMonth.add(Calendar.MONTH, -1);

        // draw in the last few days of the last month (windows)
        // on osx simply leave them blank...
        for (int i = 0; i < dayIterator.get(Calendar.DAY_OF_WEEK) - 1; i++) {
            if (drawOtherDays) {
                dayControl[0][i].setMonthOffset(-1);
                dayControl[0][i].setDay(
                    lastMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                        - (dayIterator.get(Calendar.DAY_OF_WEEK) - (2 + i)));
            } else {
                dayControl[0][i].setDay(0);
            }

            dayControl[0][i].setDefault(false);
            dayControl[0][i].setFocused(false);
            dayControl[0][i].redraw();
        }

        // and draw in the days of this month
        for (int i = selectedDate.getActualMinimum(Calendar.DAY_OF_MONTH); i <= selectedDate.getActualMaximum(
            Calendar.DAY_OF_MONTH); i++) {
            dayIterator.set(Calendar.DAY_OF_MONTH, i);

            // move on to the next row
            if (dayIterator.get(Calendar.DAY_OF_WEEK) == dayIterator.getFirstDayOfWeek()
                && i > selectedDate.getActualMinimum(Calendar.DAY_OF_MONTH)) {
                row++;
            }

            // Handle locales with different first days of the week.
            int dayOfWeek = dayIterator.get(Calendar.DAY_OF_WEEK) - dayIterator.getFirstDayOfWeek();
            if (dayOfWeek < 0) {
                dayOfWeek += (lastDayOfWeek - firstDayOfWeek) + 1;
            }

            // was getFirstDayOfWeek
            final DatepickerDate thisDayControl = dayControl[row][dayOfWeek];
            thisDayControl.setMonthOffset(0);
            thisDayControl.setDay(dayIterator.get(Calendar.DAY_OF_MONTH));
            thisDayControl.setDefault(compareDate(dayIterator, defaultDate));
            thisDayControl.setFocused(compareDate(dayIterator, selectedDate));
            thisDayControl.redraw();

            if (compareDate(dayIterator, selectedDate)) {
                focusedControl = thisDayControl;
            }
        }

        // finish off with the dayControls that are unused by this month...
        // either blank them (os x) or show the days of the next month (windows)
        int padding = 1;
        for (int i = row; i < rows; i++) {
            int start = 0;

            if (i == row) {
                // Handle locales with different first days of the week.
                start = (dayIterator.get(Calendar.DAY_OF_WEEK) + 1) - dayIterator.getFirstDayOfWeek();
                if (start < 0) {
                    start += (lastDayOfWeek - firstDayOfWeek) + 1;
                }

                // the first row may be complete... if so, skip it
                if (start == 0) {
                    continue;
                }
            }

            for (int j = start; j <= (lastDayOfWeek - firstDayOfWeek); j++) {
                if (drawOtherDays) {
                    dayControl[i][j].setMonthOffset(1);
                    dayControl[i][j].setDay(padding++);
                } else {
                    dayControl[i][j].setDay(0);
                    dayControl[i][j].setVisible(i == row);
                }

                dayControl[i][j].setDefault(false);
                dayControl[i][j].setFocused(false);
                dayControl[i][j].redraw();
            }
        }

        // force a redraw
        layout(true);
        pack();
    }

    private boolean compareDate(final Calendar date1, final Calendar date2) {
        return (date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR)
            && date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH)
            && date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH));
    }

    public void setSelection(final Date date) {
        selectedDate.setTime(date);
        drawSelectedMonth();
    }

    public Date getSelection() {
        return selectedDate.getTime();
    }

    public void setSpacing(final Point spacing) {
        this.spacing.x = spacing.x;
        this.spacing.y = spacing.y;
    }

    public void setDrawOtherDays(final boolean draw) {
        drawOtherDays = draw;
    }

    public void addSelectionListener(final SelectionListener listener) {
        addListener(SWT.Selection, new TypedListener(listener));
    }

    public void removeSelectionListener(final SelectionListener listener) {
        removeListener(SWT.Selection, listener);
    }

    @Override
    public void mouseDoubleClick(final MouseEvent e) {
    }

    @Override
    public void mouseMove(final MouseEvent e) {
        DatepickerDate newFocus = null;

        if (!mouseIsDown) {
            return;
        }

        // determine the mouse position, offset from the
        final Point mousePosition = ((Composite) e.getSource()).getLocation();
        mousePosition.x += e.x;
        mousePosition.y += e.y;

        // first try to see if we're still in the originally clicked widget
        if (e.getSource() instanceof DatepickerDate
            && ((DatepickerDate) e.getSource()).getBounds().contains(mousePosition)) {
            newFocus = (DatepickerDate) e.getSource();
        }
        // otherwise, walk each widget in this control to see if we're over
        // it...
        else {
            // if the mouse down and moved, let's walk through and try to find
            // which widget we're over
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j <= (lastDayOfWeek - firstDayOfWeek); j++) {
                    if (dayControl[i][j].getBounds().contains(mousePosition)) {
                        newFocus = dayControl[i][j];
                        break;
                    }
                }

                if (newFocus != null) {
                    break;
                }
            }
        }

        // we've left a control
        if (focusedControl != null && (newFocus == null || newFocus.getDay() == 0)) {
            focusedControl.setFocused(false);
            focusedControl = null;
        }
        // we're over a control
        else if (newFocus != null && newFocus.getDay() != 0 && newFocus != focusedControl) {
            if (focusedControl != null) {
                focusedControl.setFocused(false);
            }

            focusedControl = newFocus;
            focusedControl.setFocused(true);
        }
    }

    @Override
    public void mouseUp(final MouseEvent e) {
        if (focusedControl != null) {
            selectedDate.add(Calendar.MONTH, focusedControl.getMonthOffset());
            selectedDate.set(Calendar.DAY_OF_MONTH, focusedControl.getDay());
            notifyListeners(SWT.Selection, new Event());
        } else {
            drawSelectedMonth();
        }

        mouseIsDown = false;
    }

    @Override
    public void mouseDown(final MouseEvent e) {
        mouseIsDown = true;

        if (e.getSource() instanceof DatepickerDate && ((DatepickerDate) e.getSource()).getDay() != 0) {
            if (focusedControl != null) {
                focusedControl.setFocused(false);
            }

            focusedControl = (DatepickerDate) e.getSource();
            focusedControl.setFocused(true);
        } else {
            focusedControl = null;
        }
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        if (e.keyCode == SWT.ARROW_LEFT) {
            selectedDate.add(Calendar.DAY_OF_MONTH, -1);
            drawSelectedMonth();
        } else if (e.keyCode == SWT.ARROW_RIGHT) {
            selectedDate.add(Calendar.DAY_OF_MONTH, 1);
            drawSelectedMonth();
        } else if (e.keyCode == SWT.ARROW_UP) {
            selectedDate.add(Calendar.DAY_OF_MONTH, -7);
            drawSelectedMonth();
        } else if (e.keyCode == SWT.ARROW_DOWN) {
            selectedDate.add(Calendar.DAY_OF_MONTH, 7);
            drawSelectedMonth();
        } else if (e.keyCode == '\r') {
            notifyListeners(SWT.Selection, new Event());
        } else {
            final Event notifier = new Event();
            notifier.keyCode = e.keyCode;

            notifyListeners(SWT.KeyUp, notifier);
        }
    }

    @Override
    public void keyReleased(final KeyEvent e) {
    }

    @Override
    public void widgetDisposed(final DisposeEvent e) {
        if (font != null && !font.isDisposed()) {
            font.dispose();
        }
        if (fontBold != null && !fontBold.isDisposed()) {
            fontBold.dispose();
        }
    }
}
