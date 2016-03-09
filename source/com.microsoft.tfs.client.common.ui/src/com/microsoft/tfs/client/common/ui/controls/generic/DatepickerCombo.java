// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.controls.generic.datepicker.Datepicker;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.datetime.LenientDateTimeParser;

/**
 * <p>
 * This control presents a combo style interface for input of a date. The date
 * can be manually entered into a text control, or can be selected by using a
 * popup Datepicker control.
 * </p>
 * <p>
 * The entered Date can be retrieved by calling getText() or getDate(). See the
 * javadoc on those methods for subtleties.
 * </p>
 * <p>
 * A Date value can be set on this control by calling setDate(). By setting a
 * Date value in this way, any currently held value is lost, and the getDate(),
 * getText(), and isValid() methods will all reflect the new value.
 * </p>
 * <p>
 * If getDate() returns null, it is possible that the user entered an unparsable
 * value. In this case, isValid() will return false, and the unparsable value
 * can be obtained by calling getText().
 * </p>
 * <p>
 * If, however, getDate() returns null and isValid() returns true, it simply
 * indicates that the user cleared the date value. In this case getText() will
 * return either an empty string or a string consisting entirely of whitespace.
 * </p>
 * <p>
 * Clients interested in receiving notifications when the selected Date has
 * changed (including when the value entered by the user has changed to an
 * invalid value) may do so by adding a ModifyListener to this control. Inside
 * the ModifyListner, clients will typically call some combination of getDate(),
 * getText(), and isValid().
 * </p>
 *
 */
public class DatepickerCombo extends AbstractCombo {
    private static final Log log = LogFactory.getLog(DatepickerCombo.class);

    /*
     * The DateFormat that is used when the user selects a date using the popup
     * Datepicker. In this case, the selected date is formatted using this
     * DateFormat and the resultant String is set into the text control.
     */
    private DateFormat formatter;

    /*
     * This field holds the value returned from the getDate() method.
     *
     * When this field is non-null, it holds a value that either a) was selected
     * by the user by using the popup Datepicker b) was parsed from the user's
     * input in the text control c) came from an external client of this class
     * calling setDate()
     *
     * In any case whenever this field is non-null then the "valid" field will
     * always hold Boolean.TRUE.
     *
     * When this field is null, that means that either we haven't yet parsed a
     * user-entered date value (valid is NULL) or we have attempted to parse a
     * user-entered value and the value was unparsable (valid is Boolean.FALSE).
     */
    private Date dateValue;

    /*
     * The value that corresponds to the text held in the text control. This
     * field is always kept in-sync with the current value in the text control.
     * In addition, no trimming is done of the text control's value before
     * setting it into this field.
     */
    private String textValue;

    /*
     * This field tracks validity status for this control. When it is null that
     * means that we need to perform a parse of the value in the "textValue"
     * field in order to determine validity. Otherwise, Boolean.TRUE means that
     * the value held in the "dateValue" field (null or non-null) is a valid
     * value. When this field is set to Boolean.FALSE it means that an
     * unparsable date value was entered. In this case, the "dateValue" field
     * will be NULL and the "textValue" field will hold the unparsable value.
     */
    private Boolean valid;

    /*
     * A flag that tracks whether an internal ModifyListener on the text control
     * should be enabled.
     *
     * This mechanism is used instead of removing and re-adding the internal
     * ModifyListener to ensure that the listener is always the first
     * ModifyListener the text control has. This condition is important for the
     * correct operation of this control.
     */
    private boolean enableInternalTextModifyListener = true;

    /*
     * A lenient string-to-Calendar parser used to convert user text typed
     * directly in the control into an internal Date.
     */
    private final LenientDateTimeParser lenientDateTimeParser = new LenientDateTimeParser();

    /**
     * Creates a new DatepickerCombo, using the default Locale and default
     * DateFormat to format dates that are selected using the popup Datepicker
     * control.
     */
    public DatepickerCombo(final Composite parent, final int style) {
        this(parent, style, DateFormat.getDateTimeInstance());
    }

    /**
     * Creates a new DatepickerCombo and specifies the DateFormat that should be
     * used to format dates that are selected using the popup Datepicker
     * control.
     */
    public DatepickerCombo(final Composite parent, final int style, final DateFormat formatter) {
        super(parent, style);

        Check.notNull(formatter, "formatter"); //$NON-NLS-1$

        this.formatter = formatter;

        /*
         * Set the initial (default) state.
         */
        dateValue = new Date();
        textValue = formatter.format(dateValue);
        valid = Boolean.TRUE;
        text.setText(textValue);

        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format("created a new DatepickerCombo: {0}", toString())); //$NON-NLS-1$
        }

        /*
         * Create a ModifyListener to respond to changes made to the text
         * control.
         */
        text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                /*
                 * check an internal flag that if not set, means we need to
                 * "disable" this modify listener
                 */
                if (!enableInternalTextModifyListener) {
                    return;
                }

                /*
                 * reset any previously held Date value - the Date value will
                 * need to be recomputed by parsing the text value
                 */
                dateValue = null;

                /*
                 * reset the validity state to not computed
                 */
                valid = null;

                /*
                 * store the current value held in the text control - this value
                 * will be parsed when the getDate() method is called
                 */
                textValue = text.getText();

                if (log.isTraceEnabled()) {
                    log.trace(MessageFormat.format("DatepickerCombo text modified: [{0}]", textValue)); //$NON-NLS-1$
                }
            }
        });
    }

    /**
     * <p>
     * Obtains the text value currently held in the text control portion of this
     * DatepickerCombo control.
     * </p>
     * <p>
     * This text value does not neccessarily correspond to a value that is
     * parsable into a Date. In particular, if the user enters an unparsable
     * value, isValid() will return false, getDate() will return null, and
     * getText() will return the unparsable value.
     * </p>
     * <p>
     * If this method returns an empty string or a string consisting entirely of
     * whitespace, that indicates that either the user cleared the value in the
     * text box or a client of this class called setDate() and passed a null
     * Date. In this situation isValid() will return true and getDate() will
     * return null.
     * </p>
     *
     * @return the text value described above
     */
    public String getText() {
        return textValue;
    }

    /**
     * <p>
     * This method is used to check whether this DatepickerCombo currently holds
     * a valid Date value.
     * </p>
     * <p>
     * Normally this is done to disambiguate the situation in which getDate()
     * returns null, which can indicate that either the user cleared the value
     * or entered an unparsable value.
     * </p>
     * <p>
     * If isValid() returns false, then the unparsable value entered by the user
     * is available by calling getText().
     * </p>
     *
     * @return the validity state of this control as described above
     */
    public boolean isValid() {
        if (valid == null) {
            /*
             * call getDate() to force a validity computation
             */
            getDate();
        }

        return valid.booleanValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Widget#toString()
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();

        sb.append("isValid=["); //$NON-NLS-1$
        sb.append(valid);
        sb.append("]"); //$NON-NLS-1$

        sb.append(" dateValue=["); //$NON-NLS-1$
        sb.append(dateValue);
        sb.append("]"); //$NON-NLS-1$

        sb.append(" textValue=["); //$NON-NLS-1$
        sb.append(textValue);
        sb.append("]"); //$NON-NLS-1$

        sb.append(" format=["); //$NON-NLS-1$
        if (formatter instanceof SimpleDateFormat) {
            sb.append(((SimpleDateFormat) formatter).toPattern());
        } else {
            sb.append(formatter.toString());
        }
        sb.append("]"); //$NON-NLS-1$

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.shared.widgets.AbstractCombo#getPopup
     * (org.eclipse.swt.widgets.Composite)
     */
    @Override
    public Composite getPopup(final Composite parent) {
        final Composite container = new Composite(parent, SWT.NONE);

        final FillLayout containerLayout = new FillLayout();
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            containerLayout.marginWidth = 4;
            containerLayout.marginHeight = 4;
        }
        container.setLayout(containerLayout);

        final Datepicker datepicker = new Datepicker(container, SWT.NONE);

        container.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));

        /*
         * Initially select the currently held Date in the Datepicker, if there
         * is a currently held Date
         */
        final Date datepickerInitialDate = getDate();
        if (datepickerInitialDate != null) {
            datepicker.setSelection(datepickerInitialDate);
            if (log.isDebugEnabled()) {
                log.debug(MessageFormat.format(
                    "showing Datepicker popup, set initial date to [{0}]", //$NON-NLS-1$
                    datepickerInitialDate));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("showing Datepicker popup, did not set initial date"); //$NON-NLS-1$
            }
        }

        /*
         * Add a selection listener to the Datepicker, to pick up selected dates
         * and store them into this control
         */
        datepicker.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (log.isDebugEnabled()) {
                    log.debug("setting date from datepicker selection"); //$NON-NLS-1$
                }
                setDate(datepicker.getSelection());

                closePopup();
            }
        });

        datepicker.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
                if (e.keyCode == SWT.ESC) {
                    closePopup();
                }
            }
        });

        return container;
    }

    /**
     * <p>
     * Set the Date value held and displayed in this control. Calling this
     * method will overwrite any previously held Date value.
     * </p>
     * <p>
     * After calling this method, getDate() will return the given Date,
     * getText() will return a text representation of that Date (the same as the
     * text held in the text control), and isValid() will return true.
     * </p>
     *
     * @param dateToSet
     *        the Date to set in this control
     */
    public void setDate(final Date dateToSet) {
        /*
         * Set our Date to the input Date. If the input Date is null, our Date
         * will be null. Otherwise, a copy is made of the input Date.
         */
        dateValue = (dateToSet == null ? null : new Date(dateToSet.getTime()));
        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("inside setDate(), set date value to [{0}]", dateValue)); //$NON-NLS-1$
        }

        /*
         * set validity state
         */
        valid = Boolean.TRUE;

        /*
         * Remove the ModifyListener so we can alter the text control without
         * triggering the ModifyListener
         */
        enableInternalTextModifyListener = false;

        /*
         * Set the text control to hold either the empty string (if the date is
         * null), or a formatted representation of the date.
         *
         * IMPORTANT note: make sure that this is the last state-based thing we
         * do in this method. When we set text into the text control, that may
         * fire external listeners who are interested in the state of this
         * control.
         */
        textValue = (dateValue == null ? "" : formatter.format(dateValue)); //$NON-NLS-1$
        text.setText(textValue);
        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("inside setDate(), set text value and text box to [{0}]", textValue)); //$NON-NLS-1$
        }

        /*
         * Add our ModifyListener back in to the text control
         */
        enableInternalTextModifyListener = true;
    }

    /**
     * Gets the Date value held in this control. If this method returns null,
     * the caller may need to call isValid() to determine whether the null value
     * indicates that an unparsable value was entered by the user.
     *
     * @return the Date value described above
     */
    public Date getDate() {
        final boolean needToParse = (dateValue == null && valid == null);

        if (needToParse) {
            valid = Boolean.TRUE;
            final String valueToParse = textValue.trim();
            if (valueToParse.length() > 0) {
                try {
                    final Calendar parsedCal = lenientDateTimeParser.parse(valueToParse, true, true);

                    if (parsedCal != null) {
                        dateValue = parsedCal.getTime();
                        if (log.isDebugEnabled()) {
                            log.debug(MessageFormat.format("getDate(), parsed [{0}] to [{1}]", textValue, dateValue)); //$NON-NLS-1$
                        }
                    }
                } catch (final ParseException e) {
                    valid = Boolean.FALSE;

                    if (log.isDebugEnabled()) {
                        log.debug(MessageFormat.format(
                            "getDate(), couldn''t parse [{0}] ({1})", //$NON-NLS-1$
                            textValue,
                            e.getMessage()));
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("getDate(), value to parse was empty or whitespace"); //$NON-NLS-1$
                }
            }
        }

        return dateValue;
    }
}
