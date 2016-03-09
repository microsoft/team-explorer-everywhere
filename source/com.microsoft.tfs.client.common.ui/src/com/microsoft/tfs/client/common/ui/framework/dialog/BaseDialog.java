// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.dialog;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.sizing.ShellMinimumSizeEnforcer;
import com.microsoft.tfs.client.common.ui.framework.sizing.ShellResizeEnforcer;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;

/**
 * A base dialog class. All dialogs should extend this class instead of
 * extending the class org.eclipse.jface.dialogs.Dialog.
 *
 * <p>
 * The following behavior is provided "for free" when using this base dialog
 * class:
 * <ul>
 * <li>Dialog title setting</li>
 * <li>Resizability</li>
 * <li>Minimum size enforcement</li>
 * <li>Automatic save and restore of dialog size and position</li>
 * <li>Convenience methods for common dialog needs</li>
 * </ul>
 *
 * <p>
 * Since there are a lot of protected methods in this base class that subclasses
 * can interact with, the following naming conventions are used to help clarify
 * the intention of each protected method:
 * <ul>
 * <li>methods beginning with <b>hook</b> (hookXXX()) can be optionally
 * overriden by subclasses to allow for custom processing at various points in
 * the Dialog's lifecycle. These methods are always optional to override. Each
 * method has javadoc describing why you might want to override the method, and
 * when the hook will be called by the base class. NOTE: when overriding a
 * hook() method, the subclass <b>does not</b> have to call super().</li>
 * <li>methods beginning with <b>setOption</b> (setOptionXXX()) are provided so
 * that subclasses can change the default behavior of the base class. These
 * methods are always optional to call. It's recommended that subclasses call
 * them from the subclass constructor. Each method has javadoc that describes
 * the option and why subclasses might want to change the default behavior.</li>
 * <li>methods beginning with <b>provide</b> (provideXXX()) are abstract and
 * must be implemented by base classes. These methods provide critical data or
 * behavior that the base class needs. Each method has javadoc describing the
 * data or behavior the method provides and when it will be called by the base
 * class.</li>
 * <li>methods beginning with <b>default</b> (defaultXXX()) contain default
 * implementations of some tasks. Subclasses can optionally override these
 * default implementations when appropriate. Each method has javadoc that
 * describes the task the method does and when a subclass would want to override
 * it.</li>
 * <li>all other protected methods are not normally overriden by subclasses.
 * However, unless a method is marked with the final keyword, it is safe to
 * override. Subclasses that override one of these methods are responsible for
 * not violating the contract of other methods in this base class.</li>
 */
public abstract class BaseDialog extends Dialog {
    public static final String BASE_DIALOG_BUTTON_ID_PREFIX = "BaseDialogButton_"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(BaseDialog.class);

    /*
     * resizable dialog option (default is resizable)
     */
    private boolean resizable = true;

    /*
     * min size enforcement option (default is min size is enforced)
     */
    private boolean enforceMinimumSize = true;

    /*
     * Directions to allow resizing in. Allows one to allow resizing in x or y
     * only.
     */
    private int resizableDirections = SWT.HORIZONTAL | SWT.VERTICAL;

    /*
     * option to include default buttons in the button bar (default is true)
     */
    private boolean includeDefaultButtons = true;

    /*
     * We don't always want to persist geometry. It's now optional.
     */
    private boolean persistGeometry = true;

    /*
     * We may want to constrain the size of a window. Note that this only
     * applies to initial computation, not persisted sizes.
     */
    private Point constrainedSize = null;

    /*
     * An internal collection of button descriptions. When subclasses call
     * addButtonDescription(), the description is stored in the collection, and
     * when the dialog UI is being created, the description is used to create
     * the button widget.
     *
     * Note: a List is used to preseve the order that button descriptions are
     * added to the collection
     */
    private final List buttonDescriptions = new ArrayList();

    /*
     * the dialog settings key is used to map persisted dialog settings like
     * size and location to a IDialogSettings object. The default is to use the
     * concrete classes's fully qualified class name.
     */
    private String dialogSettingsKey = getClass().getName();

    /*
     * Used to determine when to display the statistics menu on the button bar
     * buttons (see createButton())
     */
    private boolean displayStatisticsMenuItem = false;

    /*
     * Keeps track of the time the dialog was last opened, used for statistical
     * purposes
     */
    private long lastOpenedTime = -1;

    /*
     * We need to cache the results of the first call to super.getInitialSize(),
     * because after the shell is setup, its results change.
     */
    private Point initialSize = null;

    /*
     * Used to enforce a minimum size on the shell (if enabled)
     */
    private ShellMinimumSizeEnforcer minimumSizeEnforcer;

    /*
     * Used to enforce resizing directions (if enabled)
     */
    private ShellResizeEnforcer resizeEnforcer;

    /*
     * This needs to be removed. \n is a newline on every platform in SWT.
     */
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    /**
     * Create a new BaseDialog. The constructor does nothing but call the super
     * constructor on the JFace Dialog class.
     *
     * @see org.eclipse.jface.dialogs.Dialog
     *
     * @param parentShell
     *        the parent Shell, or null to create a top-level shell
     */
    public BaseDialog(final Shell parentShell) {
        super(ShellUtils.getBestParent(parentShell));
    }

    /**
     * This base class overrides configureShell() so that the dialog's title can
     * be set on the dialog's Shell. Subclasses provide the title by
     * implementing the abstract method provideDialogTitle().
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);

        /*
         * Call abstract method on subclass to provide the (localized) title
         */
        final String title = provideDialogTitle();

        if (title == null) {
            final String messageFormat = "provideDialogTitle returned null for class [{0}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getClass().getName());
            throw new IllegalStateException(message);
        }

        newShell.setText(title);
    }

    /**
     * Override createButton() to attach a menu to each button. If a button in
     * the button bar is shift-right-clicked, it will display a menu with a
     * "statistics" option. Choosing this option raises a message box with the
     * dialog statistics.
     */
    @Override
    protected Button createButton(
        final Composite parent,
        final int id,
        final String label,
        final boolean defaultButton) {
        final Button button = super.createButton(parent, id, label, defaultButton);

        AutomationIDHelper.setWidgetID(button, BASE_DIALOG_BUTTON_ID_PREFIX + id);

        final Menu menu = new Menu(button);
        button.setMenu(menu);

        menu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuShown(final MenuEvent e) {
                final MenuItem[] items = menu.getItems();
                for (int i = 0; i < items.length; i++) {
                    items[i].dispose();
                }

                if (displayStatisticsMenuItem) {
                    final MenuItem item = new MenuItem(menu, SWT.NONE);
                    item.setText(Messages.getString("BaseDialog.StatisticsMenuItemText")); //$NON-NLS-1$
                    item.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(final SelectionEvent e) {
                            showStatistics();
                        }
                    });
                }
            }
        });

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(final MouseEvent e) {
                // right mouse button and shift key pressed
                displayStatisticsMenuItem = (e.button == 3 && (e.stateMask & SWT.SHIFT) > 0);
            }
        });
        return button;
    }

    /**
     * Display a message box containing dialog statistics.
     */
    private void showStatistics() {
        String message;
        final Point location = getShell().getLocation();
        final Point size = getShell().getSize();
        final String debugInfo = DialogSettingsHelper.getDebugInfo(dialogSettingsKey);

        if (minimumSizeEnforcer == null) {
            final String messageFormat = "current origin ({0},{1})\ncurrent size ({2},{3}\nmin size not enabled\n\n{4}"; //$NON-NLS-1$
            message = MessageFormat.format(
                messageFormat,
                Integer.toString(location.x),
                Integer.toString(location.y),
                Integer.toString(size.x),
                Integer.toString(size.y),
                debugInfo);
        } else {
            final String messageFormat = "current origin ({0},{1})\ncurrent size ({2},{3}\nmin size ({4},{5})\n\n{6}"; //$NON-NLS-1$
            message = MessageFormat.format(
                messageFormat,
                Integer.toString(location.x),
                Integer.toString(location.y),
                Integer.toString(size.x),
                Integer.toString(size.y),
                Integer.toString(minimumSizeEnforcer.getMinimumWidth()),
                Integer.toString(minimumSizeEnforcer.getMinimumHeight()),
                debugInfo);
        }

        final String titleFormat = "Statistics for dialog: {0}"; //$NON-NLS-1$
        final String title = MessageFormat.format(titleFormat, dialogSettingsKey);
        MessageBoxHelpers.messageBox(getShell(), title, message);
    }

    /**
     * Override open to record that an open happened, then call super. We count
     * the number of opens for statistics.
     */
    @Override
    public int open() {
        DialogSettingsHelper.recordDialogOpened(dialogSettingsKey);
        lastOpenedTime = System.currentTimeMillis();

        ClientTelemetryHelper.sendDialogOpened(this);

        return super.open();
    }

    /**
     * The base class overrides getShellStyle() to support setting the resizable
     * bit on the Shell style. Subclasses can modify the resizable behavior by
     * calling setOptionResizable().
     */
    @Override
    protected int getShellStyle() {
        int style = super.getShellStyle();

        /*
         * We override getShellStyle so we can tweak the Shell style each time
         * it's asked for. This is a little bit safer than just calling
         * setShellStyle() at some point, since the style bits could be
         * overwritten after they were set but before they were used.
         */

        if (resizable) {
            /*
             * If resizable is true, make sure the RESIZE bit is on...
             */
            style |= SWT.RESIZE;
        } else {
            /*
             * ...otherwise, make sure the RESIZE bit is off.
             */
            style &= (~SWT.RESIZE);
        }
        return style;
    }

    /**
     * This base class overrides initializeBounds() to enforce a minimum size on
     * the dialog's Shell after the Shell has been sized and placed on the
     * screen. Subclasses can modify the minimum size behavior by calling
     * setOptionEnforceMinimumSize().
     */
    @Override
    protected void initializeBounds() {
        super.initializeBounds();

        /*
         * At this point, the Shell has been initially sized and placed on the
         * screen in its initial location.
         */

        final Shell dialogShell = getShell();

        // do not do minimum size enforcement if size is constraned
        if (enforceMinimumSize) {
            final Point computedSize = defaultComputeMinimumSize();
            final Rectangle constrainedBounds =
                getConstrainedShellBounds(new Rectangle(0, 0, computedSize.x, computedSize.y));

            /*
             * The minimum size enforcer is enabled on instantiation of a
             * ShellMinimumSizeEnforcer object
             */
            minimumSizeEnforcer =
                new ShellMinimumSizeEnforcer(dialogShell, constrainedBounds.width, constrainedBounds.height);

            final String messageFormat = "enforcing minimum size: {0},{1}"; //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                Integer.toString(constrainedBounds.width),
                Integer.toString(constrainedBounds.height));

            log.trace(message);
        }

        // enforce resizability on the shell
        if (resizableDirections != (SWT.HORIZONTAL | SWT.VERTICAL)) {
            resizeEnforcer = new ShellResizeEnforcer(dialogShell, resizableDirections);
        }
    }

    /**
     * The base class overrides createDialogArea() to first call the JFace
     * dialog implementation, which creates a Composite that controls can be
     * placed into. The base class then calls hookAddToDialogArea() to allow
     * subclasses to add controls into the Composite. Subclasses may also
     * override createDialogArea() to have complete control over the dialog
     * area.
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite composite = (Composite) super.createDialogArea(parent);
        hookAddToDialogArea(composite);

        /* Hook up our paint listener which will call our open hook */
        composite.addPaintListener(new FirstPaintListener());

        return composite;
    }

    /**
     * Subclasses may override this method to do their own button bar handling.
     * However, it is usually easier for subclasses to use the
     * setIncludeDefaultButtons() and addButtonDescription() methods instead.
     */
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        for (final Iterator it = buttonDescriptions.iterator(); it.hasNext();) {
            final ButtonDescription buttonDescription = (ButtonDescription) it.next();
            createButton(
                parent,
                buttonDescription.buttonId,
                buttonDescription.buttonLabel,
                buttonDescription.isDefault);
        }

        if (includeDefaultButtons) {
            super.createButtonsForButtonBar(parent);
        }

        hookAfterButtonsCreated();
    }

    /**
     * Subclasses may override this method to do their own handling of dialog
     * button presses. However, it is usually easier for subclasses to override
     * hookCustomButtonPressed, okPressed, or cancelPressed.
     */
    @Override
    protected void buttonPressed(final int buttonId) {
        if (buttonId != IDialogConstants.OK_ID && buttonId != IDialogConstants.CANCEL_ID) {
            hookCustomButtonPressed(buttonId);
        } else {
            /*
             * The super implementation handles the OK and CANCEL ids, but
             * nothing else.
             */
            super.buttonPressed(buttonId);
        }
    }

    /**
     * This base class overrides close() to persist the dialog settings, record
     * the elapsed open time for statistics, and to call a subclass hook.
     */
    @Override
    public boolean close() {
        /*
         * Did we record an opened time?
         */
        if (lastOpenedTime != -1) {
            final long elapsedTimeOpen = System.currentTimeMillis() - lastOpenedTime;

            DialogSettingsHelper.recordDialogClosed(dialogSettingsKey, elapsedTimeOpen);

            lastOpenedTime = -1; // reset
        }

        DialogSettingsHelper.persistShellGeometry(getShell(), dialogSettingsKey);
        hookDialogAboutToClose();
        return super.close();
    }

    /**
     * This base class overrides getInitialLocation() in order to load persisted
     * location data when the dialog is opened. Subclasses should override
     * defaultComputeInitialLocation() if they want to override the initial
     * location computation when a persisted location is not found.
     */
    @Override
    protected Point getInitialLocation(final Point initialSize) {
        Point location = defaultComputeInitialLocation(initialSize);

        if (persistGeometry) {
            location = DialogSettingsHelper.getInitialLocation(dialogSettingsKey, location);
        }

        return location;
    }

    /**
     * This base class overrides getInitialSize() in order to load persisted
     * size data when the dialog is opened. Subclasses should override
     * defaultComputeInitialSize() if they want to override the initial size
     * computation when a persisted size is not found.
     */
    @Override
    protected Point getInitialSize() {
        Point size = defaultComputeInitialSize();

        if (persistGeometry) {
            size = DialogSettingsHelper.getInitialSize(dialogSettingsKey, size);
        }

        return size;
    }

    /**
     * Computes the initial location of the dialog.
     *
     * <p>
     * The location computed by this method is only used if a persisted location
     * can not be found for the dialog.
     *
     * <p>
     * This default implementation calls getInitialLocation() on the base JFace
     * dialog class. Subclasses can override to perform a different initial
     * location computation.
     *
     * @param initialSize
     *        the initial size of the Shell
     * @return the initial location of the Shell
     */
    protected Point defaultComputeInitialLocation(final Point initialSize) {
        return super.getInitialLocation(initialSize);
    }

    /**
     * Computes the default minimum size of the dialog. By default this is the
     * same as the default initial size.
     *
     * @return
     */
    protected Point defaultComputeMinimumSize() {
        return defaultComputeInitialSize();
    }

    /**
     * Computes the initial size of the dialog. This will apply any constraints
     * provided by setOptionConstrainedSize().
     *
     * <p>
     * The size computed by this method is only used if a persisted size can not
     * be found for the dialog.
     *
     * <p>
     * This default implementation calls getInitialSize() on the base JFace
     * dialog class. Subclasses can override to perform a different initial size
     * computation.
     *
     * @return the initial size of the Shell
     */
    protected Point defaultComputeInitialSize() {
        if (initialSize != null) {
            return initialSize;
        }

        initialSize = super.getInitialSize();

        // constrain size to specific size if requested
        if (constrainedSize != null && constrainedSize.x != SWT.DEFAULT && constrainedSize.y != SWT.DEFAULT) {
            initialSize = constrainedSize;
        }
        // otherwise constrain size in one direction
        else if (constrainedSize != null) {
            final Point constraints = new Point(constrainedSize.x, constrainedSize.y);

            // noop if the computed size is actually less than our constraint
            // otherwise, set the size to the constrained value and the hint
            // (we adjust later)
            if (constraints.x != SWT.DEFAULT) {
                constraints.x = Math.min(initialSize.x, constraints.x);
                initialSize = new Point(constraints.x, initialSize.y);
            }

            if (constraints.y != SWT.DEFAULT) {
                constraints.y = Math.min(constraints.y, initialSize.y);
                initialSize = new Point(initialSize.x, constraints.y);
            }

            // get the constrained size for each control - if constraining a
            // control in one dimension resizes it in another, add that to the
            // computed size
            final Control[] controls = ((Composite) getDialogArea()).getChildren();
            for (int i = 0; i < controls.length; i++) {
                final Point defaultSize = controls[i].computeSize(SWT.DEFAULT, SWT.DEFAULT);
                final Point actualSize = controls[i].computeSize(constraints.x, constraints.y);

                if (constraints.y != SWT.DEFAULT) {
                    initialSize.x += (actualSize.x - defaultSize.x);
                }

                if (constraints.x != SWT.DEFAULT) {
                    initialSize.y += (actualSize.y - defaultSize.y);
                }
            }
        }

        if (log.isTraceEnabled()) {
            final String messageFormat =
                "defaultComputeInitialSize in BaseDialog class - Dialog.getInitialSize={0},{1}"; //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, Integer.toString(initialSize.x), Integer.toString(initialSize.y));
            log.trace(message);
        }

        return initialSize;
    }

    /**
     * Subclasses can override hookCustomButtonPressed() to perform custom
     * processing when a custom button ID is pressed.
     *
     * <p>
     * A custom button ID is an ID that is not OK_ID or CANCEL_ID. The button
     * must have been added to the dialog in the standard way (either through
     * addButtonDescription() or createButton()).
     *
     *
     * <p>
     * To perform custom handling of OK or CANCEL button clicks, override
     * okPressed() or cancelPressed() instead of hookCustomButtonPressed().
     *
     *
     * <p>
     * hookCustomButtonPressed() is called by the base class from
     * buttonPressed().
     *
     *
     * @param buttonId
     *        the custom button ID of the button that was pressed
     */
    protected void hookCustomButtonPressed(final int buttonId) {

    }

    /**
     * Subclasses can override hookDialogIsOpen() to perform custom processing
     * when the dialog is open.
     *
     * hookDialogIsOpen() is called immediately after the dialog is raised, so
     * it is the first chance to interact with the widgets after they have been
     * painted. It's a good place to set focus (which usually depends on
     * visibility.)
     */
    protected void hookDialogIsOpen() {

    }

    /**
     * Subclasses can override hookDialogAboutToClose() to perform custom
     * processing when the dialog is about to close.
     *
     * <p>
     * hookDialogAboutToClose() is called just before the dialog's widgets have
     * been disposed, so it is the last change to interact with the widgets
     * before they are cleaned up. It's a good place to save any widget settings
     * or pull data out of widgets.
     */
    protected void hookDialogAboutToClose() {

    }

    /**
     * Subclasses can override hookAddToDialogArea() to add controls into the
     * dialog area Composite created by the JFace dialog base class.
     *
     * <p>
     * hookAddToDialogArea() is called by this base class from
     * createDialogArea().
     *
     * <p>
     * This hook method is optional. A subclass can gain complete control over
     * the dialog area by overriding createDialogArea().
     *
     * <p>
     * If a subclass overrides this method, it <b>should not</b> override the
     * createDialogArea() method.
     *
     * @param dialogArea
     *        the Composite to add controls into
     */
    protected void hookAddToDialogArea(final Composite dialogArea) {

    }

    /**
     * Subclasses can override hookAfterButtonsCreated() to perform custom
     * processing after the buttons have been created in the button bar.
     *
     * <p>
     * For instance, subclasses may wish to access the buttons so they can be
     * enabled and disabled for validation purposes. This method provides
     * subclasses with a chance to do that.
     *
     * <p>
     * The buttons can be obtained by calling the getButton() method.
     *
     * <p>
     * If a subclass overrides this method, it <b>should not</b> override the
     * createButtonsForButtonBar() method.
     */
    protected void hookAfterButtonsCreated() {

    }

    /**
     * Adds a button description to this dialog. The button description is used
     * when the dialog's UI is being created to create a button widget in the
     * button bar.
     *
     * <p>
     * This method is for the benefit of subclasses. This base class does not
     * call it internally.
     *
     * <p>
     * If this method is called multiple times, the sequence is preserved, and
     * the buttons will be added in that sequence.
     *
     * <p>
     * If the default buttons are being included (see
     * setOptionIncludeDefaultButtons()), the buttons added because of calling
     * addButtonDescription are added <b>before</b> the default buttons.
     *
     * <p>
     * By calling setOptionIncludeDefaultButtons() and addButtonDescription()
     * subclasses can have complete control over the buttons in the button bar.
     *
     * @param buttonId
     * @param buttonLabel
     * @param isDefaultButton
     */
    protected void addButtonDescription(final int buttonId, final String buttonLabel, final boolean isDefaultButton) {
        buttonDescriptions.add(new ButtonDescription(buttonId, buttonLabel, isDefaultButton));
    }

    /**
     * Set the resizability behavior of this dialog.
     *
     * <p>
     * By default, the dialog will be resizable. Subclasses can call this method
     * to change the default behavior.
     *
     * <p>
     * The recommended place to call this method is from the subclass
     * constructor.
     *
     * @param resizable
     *        true if the dialog should be resizeable (default true)
     */
    protected void setOptionResizable(final boolean resizable) {
        /*
         * Just store the resizable option. We tweak the Shell style bits in the
         * getShellStyle override.
         */
        this.resizable = resizable;
    }

    /**
     * Set the minimum size behavior of this dialog.
     *
     * <p>
     * By default, the dialog enforces a minimum size - it will not allow the
     * user to resize it smaller than the minimum size. Subclasses can call this
     * method to change the default behavior.
     *
     * <p>
     * The recommended place to call this method is from the subclass
     * constructor.
     *
     * @param enforceMinimumSize
     *        true to enforce min size for the dialog (default true)
     */
    protected void setOptionEnforceMinimumSize(final boolean enforceMinimumSize) {
        /*
         * Store the option. We use it in the override of initializeBounds().
         */
        this.enforceMinimumSize = enforceMinimumSize;
    }

    /**
     * Set the behavior of this dialog with regard to the default buttons (the
     * standard OK and Cancel buttons provided by the base JFace dialog class).
     *
     * <p>
     * By default, these standard buttons are included in the button bar.
     * Subclasses can call this method to change the default behavior.
     *
     * <p>
     * The recommended place to call this method is from the subclass
     * constructor.
     *
     * @param includeDefaultButtons
     *        true to include the default OK and cancel buttons (default true)
     */
    protected void setOptionIncludeDefaultButtons(final boolean includeDefaultButtons) {
        this.includeDefaultButtons = includeDefaultButtons;
    }

    /**
     * Set the dialog settings key for this dialog. The dialog settings key is
     * used to match persisted settings like size and location to this dialog.
     *
     * <p>
     * The default is to use the dialog's fully qualified class name as the
     * settings key. However, if the same dialog class is used differently in
     * different situations, it may be desirable to use a different settings key
     * in each situation. Subclasses can call this method to set a new dialog
     * settings key.
     *
     * <p>
     * The recommended place to call this method is from the subclass
     * constructor.
     *
     * @param dialogSettingsKey
     *        the settings key for this dialog
     */
    protected void setOptionDialogSettingsKey(final String dialogSettingsKey) {
        this.dialogSettingsKey = dialogSettingsKey;
    }

    /**
     * Set the behavior of this dialog with regard to the persistent size and
     * location. If this is true, we save the size and position of this dialog
     * each time it's raised, and restore the size and position when we open a
     * new dialog of this type. Thus, if a user resizes a dialog, this will open
     * at their "preferred" size.
     *
     * <p>
     * It is recommended that you disable persistentGeometry if you have a
     * dialog which changes its content (and thus its size) frequently, and just
     * use the defaults, which are computed by content size.
     *
     * <p>
     * The recommended place to call this method is from the subclass
     * constructor.
     *
     * @param persistGeometry
     *        persistGeometry true to reuse the size and location of this dialog
     *        the last time it was raised. (default true)
     */
    protected void setOptionPersistGeometry(final boolean persistGeometry) {
        this.persistGeometry = persistGeometry;
    }

    /**
     * Sets the maximum size of the dialog. You may pass a Point with two int
     * values and the size of the dialog will be constrained to exactly <x, y>,
     * or you may pass a Point with either x or y values set to SWT.DEFAULT and
     * the size of the dialog will be constrained width-wise or height-wise,
     * respectively.
     * <p>
     * The recommended place to call this method is from the subclass
     * constructor.
     *
     * @param constrainedSize
     *        a Point representing the maximum size of the dialog (either x or y
     *        may be SWT.DEFAULT)
     */
    protected void setOptionConstrainSize(final Point constrainedSize) {
        this.constrainedSize = constrainedSize;
    }

    protected void setOptionResizableDirections(final int directions) {
        resizableDirections = (directions & (SWT.HORIZONTAL | SWT.VERTICAL));
    }

    /**
     * Subclasses must implement this method to provide a title for the dialog.
     *
     * <p>
     * The title must be localized by the subclass (localization is not built
     * into this base dialog class).
     *
     * <p>
     * This method will be called by the base class from configureShell(), which
     * means that the dialog area contents will not yet have been created.
     *
     * @return the title to use for this Dialog
     */
    protected abstract String provideDialogTitle();

    /*
     * GEOMETRY HELPER METHODS
     *
     * These methods exist to aide in cross-platform layout. These all provide
     * the standard spacing and margin sizes for the running platform. It is
     * strongly recommended that you use these methods instead of hardcoding
     * non-portable pixel sizes.
     */

    /**
     * This method will provide the horizontal spacing (in pixels) for controls
     * on this platform.
     *
     * @return Horizontal spacing of controls (in pixels)
     */
    public int getHorizontalSpacing() {
        return convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    }

    /**
     * This method will provide the vertical spacing (in pixels) for controls on
     * this platform.
     *
     * @return Vertical spacing of controls (in pixels)
     */
    public int getVerticalSpacing() {
        return convertHorizontalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    }

    /**
     * This method will provide the default control spacing (in pixels) for
     * controls on this platform. It is the maximum of horizontal and vertical
     * spacing. (Same on most platforms.)
     *
     * @return Spacing of controls (in pixels)
     */
    public int getSpacing() {
        return Math.max(getHorizontalSpacing(), getVerticalSpacing());
    }

    /**
     * This method will return the margin width or "gutter" (in pixels) for
     * dialogs on this platform.
     *
     * @return Width of the margin around dialogs and controls (in pixels)
     */
    public int getHorizontalMargin() {
        return convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    }

    /**
     * This method will return the margin height (in pixels) for dialogs on this
     * platform.
     *
     * @return Height of the margin around dialogs and controls
     */
    public int getVerticalMargin() {
        return convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    }

    /**
     * This method will return the "minimum message area width" (in pixels) for
     * dialogs on this platform. This number is typically better used as a
     * default width for shells.
     *
     * @return Width of the minimum message area in a shell
     */
    public int getMinimumMessageAreaWidth() {
        return convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
    }

    /*
     * Used internally only - do not expose to subclasses or as public API
     * Stores button data so that we can create a button widget when needed
     */
    static class ButtonDescription {
        int buttonId;
        String buttonLabel;
        boolean isDefault;

        public ButtonDescription(final int buttonId, final String buttonLabel, final boolean isDefault) {
            this.buttonId = buttonId;
            this.buttonLabel = buttonLabel;
            this.isDefault = isDefault;
        }
    }

    /*
     * Used internally only. Catches the first paint event and calls
     * hookDialogIsOpen().
     */
    private class FirstPaintListener implements PaintListener {
        @Override
        public void paintControl(final PaintEvent e) {
            ((Composite) e.widget).removePaintListener(this);
            hookDialogIsOpen();
        }
    }
}
