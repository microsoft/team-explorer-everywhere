// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.wizard;

import java.text.MessageFormat;

import javax.naming.ldap.Control;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;

/**
 * {@link ExtendedWizardPage} is an abstract {@link WizardPage} extension that
 * implements a somewhat different model for creating wizards than the standard
 * {@link Wizard} / {@link WizardPage} approach. It is intended to be used with
 * {@link ExtendedWizard}.
 *
 * @see ExtendedWizard
 */
public abstract class ExtendedWizardPage extends BaseWizardPage {
    /**
     * Creates a new {@link ExtendedWizardPage}.
     *
     * @param pageName
     *        the page name used to identify this page internally (must not be
     *        <code>null</code>)
     * @param title
     *        the page title, or <code>null</code> for no title
     * @param description
     *        the page description text, or <code>null</code> for no description
     *        text
     */
    protected ExtendedWizardPage(final String pageName, final String title, final String description) {
        super(pageName);
        setTitle(title);
        setDescription(description);
    }

    /**
     * <p>
     * {@link ExtendedWizardPage} overrides this {@link IWizardPage} method. It
     * returns <code>true</code> if this page is complete (
     * {@link #isPageComplete()}) and this page's {@link ExtendedWizardPage}
     * returns <code>true</code> from
     * {@link ExtendedWizard#enableNext(IWizardPage)}.
     * </p>
     *
     * <p>
     * {@link ExtendedWizardPage} subclasses typically will not override this
     * method.
     * </p>
     */
    @Override
    public boolean canFlipToNextPage() {
        /*
         * Called by the container to enable the next button. The WizardPage
         * implementation checks for page completeness (isPageComplete()) and
         * then calls Wizard#getNextPage() to see if the result is non-null. In
         * the extended wizard framework, we don't want to call getNextPage() on
         * the wizard until we need the next page for displaying. See the
         * getNextPage() method on ExtendedWizard for more explanation.
         */

        return isPageComplete() && getExtendedWizard().enableNext(this);
    }

    /**
     * <p>
     * {@link ExtendedWizardPage} overrides this {@link IWizardPage} method. It
     * first performs page-finished callback on both the page (
     * {@link #onPageFinished()}) and on the page's {@link ExtendedWizard} (
     * {@link ExtendedWizard#onPageFinished(IWizardPage)}). If both of the
     * callback succeed, it then obtains the next page from the wizard (
     * {@link IWizard#getNextPage(IWizardPage)}).
     * </p>
     *
     * <p>
     * This method is normally only called by the wizard container. It may also
     * be called by the page itself, in response to some auto-next-page action
     * like a double-click. An example of such an operation:
     *
     * <pre>
     * if (getExtendedWizard().enableNext(this)) {
     *     myControl.addDoubleClickListener(new IDoubleClickListener() {
     *         public void doubleClick(DoubleClickEvent event) {
     *             IWizardPage nextPage = getNextPage();
     *             if (nextPage != null) {
     *                 getContainer().showPage(nextPage);
     *             }
     *         }
     *     });
     * }
     * </pre>
     *
     * </p>
     *
     * <p>
     * {@link ExtendedWizardPage} subclasses typically will not override this
     * method.
     * </p>
     */
    @Override
    public IWizardPage getNextPage() {
        /*
         * Called by the container when the "next" button is pressed. Could also
         * be called by the page itself in response to some auto-next-page
         * action, like a double-click on an element. The WizardPage
         * implementation also calls this method from canFlipToNextPage, but the
         * extended wizard framework changes that behavior.
         */

        try {
            if (!onPageFinished()) {
                return null;
            }

            if (!getExtendedWizard().onPageFinished(this)) {
                return null;
            }

            return getWizard().getNextPage(this);
        }
        /*
         * Handle errors that occurred - the Eclipse wizard framework suppresses
         * these from the user when they occur.
         */
        catch (final RuntimeException e) {
            final String titleFormat = Messages.getString("ExtendedWizardPage.ErrorDialogTitleFormat"); //$NON-NLS-1$
            final String title = MessageFormat.format(titleFormat, getName());
            final String messageFormat = Messages.getString("ExtendedWizardPage.ErrorDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getName());

            ErrorDialog.openError(
                getShell(),
                title,
                null,
                new Status(Status.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, e));

            throw e;
        }
    }

    /**
     * <p>
     * This override of the {@link IWizardPage} method ensures that the
     * specified wizard is an {@link ExtendedWizard}. If it is not, an
     * {@link IllegalArgumentException} is thrown. If it is, the
     * {@link WizardPage} super method is called with the argument.
     * </p>
     *
     * <p>
     * {@link ExtendedWizardPage} subclasses typically will not override this
     * method.
     * </p>
     */
    @Override
    public void setWizard(final IWizard newWizard) {
        if (!(newWizard instanceof ExtendedWizard)) {
            final String messageFormat = "the page [{0}] must be used with an ExtendedWizard"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getClass().getName());
            throw new IllegalArgumentException(message);
        }
        super.setWizard(newWizard);
    }

    /**
     * <p>
     * {@link ExtendedWizardPage} overrides this {@link IWizardPage} method. It
     * uses <code>setVisible()</code> as a hook point to perform some additional
     * tasks when the page is shown or hidden. When the page is shown,
     * {@link #refresh()} is called to give the {@link ExtendedWizardPage}
     * subclass a chance to refresh page state. When the page is hidden,
     * {@link #onMovingToPreviousPage()} is called if navigating to the previous
     * page, and {@link #saveState(IDialogSettings)} is called to give the
     * subclass a chance to persist state.
     * </p>
     *
     * <p>
     * {@link ExtendedWizardPage} subclasses typically will not override this
     * method.
     * </p>
     */
    @Override
    public void setVisible(final boolean visible) {
        /*
         * The DialogPage setVisible implementation just calls setVisible on the
         * control (getControl()) with the argument.
         */
        super.setVisible(visible);

        if (visible) {
            final IWizardPage previousPage = getPreviousPage();
            if (previousPage != null && previousPage.getControl() != null) {
                previousPage.setVisible(false);
            }
            refresh();
        } else {
            final IWizardPage curPage = getContainer().getCurrentPage();
            final IWizardPage prevPage = getPreviousPage();

            if (curPage == prevPage) {
                onMovingToPreviousPage();
            }

            saveState(getDialogSettings());
        }
    }

    /**
     * <p>
     * {@link ExtendedWizardPage} overrides this {@link IDialogPage} method. It
     * creates a page control with up to two areas - one area for the page
     * itself and one area for an {@link ExtendedWizard} extension control. The
     * {@link ExtendedWizardPage} subclass will add the page control in the
     * {@link #doCreateControl(Composite, IDialogSettings)} method.
     * </p>
     *
     * <p>
     * {@link ExtendedWizardPage} subclasses typically will not override this
     * method.
     * </p>
     */
    @Override
    public void createControl(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);
        SWTUtil.gridLayout(composite, 1, false, 0, 0);

        final Composite pageControlComposite = SWTUtil.createComposite(composite);
        SWTUtil.fillLayout(pageControlComposite);
        GridDataBuilder.newInstance().grab().fill().applyTo(pageControlComposite);

        doCreateControl(pageControlComposite, getDialogSettings());

        if (getExtendedWizard().hasExtensionControl()) {
            final Composite wizardExtensionComposite = SWTUtil.createComposite(composite);
            SWTUtil.fillLayout(wizardExtensionComposite);
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(wizardExtensionComposite);

            getExtendedWizard().createExtensionControl(this, wizardExtensionComposite);
        }

        setControl(composite);
    }

    @Override
    public void setPreviousPage(IWizardPage page) {
        super.setPreviousPage(page);
        this.getExtendedWizard().previousPageSet(this, page);
    }

    /**
     * Called once, to create this page's control. The
     * {@link ExtendedWizardPage} subclass should create a single
     * {@link Control} that is a child of the specified parent {@link Composite}
     * . No layout data should be set on the single child control. The subclass
     * should not call {@link #setControl(Control)} with the control that it
     * creates.
     *
     * @param parent
     *        the parent {@link Composite} (must not be <code>null</code>)
     * @param dialogSettings
     *        an {@link IDialogSettings} that can be used to restore previously
     *        persisted page state (must not be <code>null</code>)
     */
    protected abstract void doCreateControl(Composite parent, IDialogSettings dialogSettings);

    /**
     * Called every time this {@link ExtendedWizardPage} is hidden.
     * {@link ExtendedWizardPage} subclasses should override this method if
     * there is persistent (across wizard invocations) state that needs to be
     * saved. Data that only needs to be saved for a single wizard invocation
     * should not be persisted here - such data is normally held as instance
     * data on a page or inside a page's control.
     *
     * @param dialogSettings
     *        an {@link IDialogSettings} object that can be used to save state
     *        (must not be <code>null</code>)
     */
    protected void saveState(final IDialogSettings dialogSettings) {

    }

    /**
     * Called every time this {@link ExtendedWizardPage} becomes visible.
     * {@link ExtendedWizardPage} subclasses should override this method and
     * consider performing the following tasks:
     * <ul>
     * <li>Remove any page data objects from the page data store that this page
     * may have previously added. Doing this gives the expected navigation
     * behavior.</li>
     * <li>Perform any operations needed to refresh the page state. Often this
     * will involve retrieving page data object from the page data store. If
     * these operations are long-running, they should be performed as a command
     * executed by the executor available by calling
     * {@link #getCommandExecutor()}.</li>
     * </ul>
     */
    protected void refresh() {

    }

    /**
     * Called when the wizard is moving to the previous page. Typically,
     * {@link ExtendedWizardPage} subclasses do not need to override this
     * method.
     */
    protected void onMovingToPreviousPage() {

    }

    /**
     * A callback that is invoked when the page is finished - typically, because
     * next or finish was clicked. This is the point at which the
     * {@link ExtendedWizardPage} subclass should perform any long-running
     * operations before navigating to the next page or finishing the wizard.
     * Any such operations should be performed using the
     * {@link ICommandExecutor} available from {@link #getCommandExecutor()}. If
     * the operations fail, the subclass can return <code>false</code> to stay
     * on the current page. The subclass should also add to the page data store
     * any page data objects produced by the page.
     *
     * @return <code>true</code> to continue the navigation, or
     *         <code>false</code> to stay on the current page
     */
    protected boolean onPageFinished() {
        return true;
    }

    /**
     * @return this {@link ExtendedWizardPage}'s {@link ExtendedWizard} (never
     *         <code>null</code>)
     */
    protected ExtendedWizard getExtendedWizard() {
        return (ExtendedWizard) getWizard();
    }

    /**
     * @return an {@link ICommandExecutor} suitable for use by this
     *         {@link ExtendedWizardPage} (never <code>null</code>)
     */
    protected ICommandExecutor getCommandExecutor() {
        return getExtendedWizard().getCommandExecutor();
    }
}
