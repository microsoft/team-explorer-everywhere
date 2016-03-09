// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.wizard;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ldap.Control;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link ExtendedWizard} is an abstract {@link Wizard} extension that
 * implements a somewhat different model for creating wizards than the standard
 * {@link Wizard} / {@link WizardPage} approach. It is intended to be used with
 * {@link ExtendedWizardPage}.
 * </p>
 *
 * <p>
 * A central concept of the extended wizard framework is page data. Page data is
 * keyed data objects that pages and the wizard use to communicate in a
 * loosely-coupled way with each other. For instance, consider two pages A and
 * B. Page B needs some data that page A can produce. When page A finishes, it
 * places the needed data into the page data store by calling
 * {@link #setPageData(Object, Object)}. When page B displays and refreshes, it
 * retrieves the needed data from the page data store by calling
 * {@link #getPageData(Object)}. Pages must agree upon the keys that will be
 * used. Often, the keys will be the Java class type of the data needed. For
 * instance, if the needed data is a <code>Example</code> object, the key could
 * be <code>Example.class</code>.
 * </p>
 *
 * @see ExtendedWizardPage
 */
public abstract class ExtendedWizard extends Wizard {
    /**
     * The page data store (never <code>null</code>).
     */
    private final Map pageData = new HashMap();

    /**
     * <p>
     * Creates a new {@link ExtendedWizard} with the specified window title and
     * specified default page image. A default dialog settings key is generated
     * using the runtime type of this class.
     * </p>
     *
     * <p>
     * In the {@link ExtendedWizard} subclass constructor that calls this
     * constructor, subclasses should add in all of the pages they could
     * potentially use (by calling {@link #addPage(IWizardPage)}).
     * </p>
     *
     * @param windowTitle
     *        the window title to set, or <code>null</code> to not set a window
     *        title
     * @param defaultPageImageDescriptor
     *        the default page image to set, or <code>null</code> to not set a
     *        default page image
     */
    protected ExtendedWizard(final String windowTitle, final ImageDescriptor defaultPageImageDescriptor) {
        this(windowTitle, defaultPageImageDescriptor, null);
    }

    /**
     * <p>
     * Creates a new {@link ExtendedWizard} with the specified window title,
     * specified default page image, and specified dialog settings key.
     * </p>
     *
     * <p>
     * In the {@link ExtendedWizard} subclass constructor that calls this
     * constructor, subclasses should add in all of the pages they could
     * potentially use (by calling {@link #addPage(IWizardPage)}).
     * </p>
     *
     * @param windowTitle
     *        the window title to set, or <code>null</code> to not set a window
     *        title
     * @param defaultPageImageDescriptor
     *        the default page image to set, or <code>null</code> to not set a
     *        default page image
     * @param dialogSettingsKey
     *        the dialog settings key to use, or <code>null</code> to generate a
     *        default dialog settings key
     */
    protected ExtendedWizard(
        final String windowTitle,
        final ImageDescriptor defaultPageImageDescriptor,
        String dialogSettingsKey) {
        /*
         * Create the IDialogSettings for this wizard
         */
        if (dialogSettingsKey == null) {
            dialogSettingsKey = getClass().getName();
            if (dialogSettingsKey.indexOf(".") != -1) //$NON-NLS-1$
            {
                dialogSettingsKey = dialogSettingsKey.substring(dialogSettingsKey.lastIndexOf(".") + 1); //$NON-NLS-1$
            }
            dialogSettingsKey = "wizard-" + dialogSettingsKey; //$NON-NLS-1$
        }
        final IDialogSettings pluginSettings = TFSCommonUIClientPlugin.getDefault().getDialogSettings();
        IDialogSettings wizardSettings = pluginSettings.getSection(dialogSettingsKey);
        if (wizardSettings == null) {
            pluginSettings.addNewSection(dialogSettingsKey);
            wizardSettings = pluginSettings.getSection(dialogSettingsKey);
        }
        setDialogSettings(wizardSettings);

        /*
         * Set up cosmetics
         */
        setNeedsProgressMonitor(true);
        if (windowTitle != null) {
            setWindowTitle(windowTitle);
        }
        if (defaultPageImageDescriptor != null) {
            setDefaultPageImageDescriptor(defaultPageImageDescriptor);
        }

        ClientTelemetryHelper.sendWizardOpened(this);

        /*
         * Subclasses should add in all pages they might use (addPage()) in the
         * subclass constructor.
         */
    }

    /**
     * This {@link IWizardPage} method is overridden by {@link ExtendedWizard}
     * to call {@link #getNextPage(IWizardPage)} with a <code>null</code>
     * argument. Subclasses should override if there is a potential starting
     * page that is determined separately from the page data logic in
     * {@link #getNextPage(IWizardPage)} - for example, a license key page.
     */
    @Override
    public IWizardPage getStartingPage() {
        return getNextPage(null);
    }

    /**
     * This {@link IWizard} method is overridden by {@link ExtendedWizard}. If
     * the current page is not complete, <code>false</code> is returned since
     * finishing should usually not be enabled if the current page is
     * incomplete. Otherwise, the subclass {@link #enableFinish(IWizardPage)}
     * method is called to compute finish enablement.
     */
    @Override
    public boolean canFinish() {
        final IWizardPage currentPage = getContainer().getCurrentPage();

        if (currentPage != null) {
            if (!currentPage.isPageComplete()) {
                // if we're on any page that is not complete, can't finish
                return false;
            }
        }

        return enableFinish(currentPage);
    }

    /**
     * This method is overridden by {@link ExtendedWizard} to do nothing.
     * {@link ExtendedWizard} subclasses should not typically override this
     * method. The {@link ExtendedWizard} behavior is to not pre-create any
     * pages, but instead return pages as needed from
     * {@link #getNextPage(IWizardPage)} and have them be initialized on demand.
     */
    @Override
    public void createPageControls(final Composite pageContainer) {
        /*
         * Called by the wizard container to allow the wizard to pre-create all
         * of the controls for each page. The Wizard implementation calls
         * createControl() for every page. ExtendedWizard intentionally
         * overrides to do nothing. The intent is for all of the possible pages
         * to be added in the constructor, but not all pages will be shown in a
         * given run of the wizard. We want to create the page controls lazily
         * to avoid the overhead of pre-creating controls we won't show.
         */
    }

    /**
     * <p>
     * Sets a page data object. This method can be called either by the wizard
     * itself or by a page. Any existing page data object that uses the same key
     * will be discarded.
     * </p>
     *
     * <p>
     * {@link ExtendedWizard} subclasses typically will not override this
     * method. They may override it if they want to perform on-the-fly
     * translation of certain page data values, or to add alternate mappings for
     * certain page data values. Another way (probably better) way to do these
     * kinds of tasks is to override {@link #getPageData(Object, boolean)}
     * instead.
     * </p>
     *
     * @param key
     *        the page data key (must not be <code>null</code>)
     * @param data
     *        the page data object to be associated with the key, or
     *        <code>null</code> to have no page data associated with the key
     * @return any existing page data object that was associated with the key,
     *         or <code>null</code> if the key was not mapped
     */
    public Object setPageData(final Object key, final Object data) {
        Check.notNull(key, "key"); //$NON-NLS-1$

        if (data == null) {
            return pageData.remove(key);
        } else {
            return pageData.put(key, data);
        }
    }

    /**
     * <p>
     * Obtains page data for the specified key. If the specified key maps to
     * <code>null</code>, an exception is thrown.
     * </p>
     *
     * <p>
     * {@link ExtendedWizard} subclasses that want to hook page data retrieval
     * should not override this method - instead, they should override
     * {@link #getPageData(Object, boolean)}.
     * </p>
     *
     * @param key
     *        the page data key (must not be <code>null</code>)
     * @return the page data associated with the key (never <code>null</code>)
     */
    public Object getPageData(final Object key) {
        return getPageData(key, true);
    }

    /**
     * <p>
     * Obtains page data for the specified key. If <code>mustExist</code> is
     * <code>true</code> and the key maps to <code>null</code>, an exception is
     * thrown.
     * </p>
     *
     * <p>
     * {@link ExtendedWizard} subclasses should override this method if they
     * wish to hook the page data retrieval process. For instance, they may want
     * to translate one key to another on-the-fly.
     * </p>
     *
     * @param key
     *        the page data key (must not be <code>null</code>)
     * @param mustExist
     *        <code>true</code> if the page data must exist - if the key is not
     *        mapped to a non-<code>null</code> object, an exception is thrown
     * @return the page data object for the specified key, or <code>null</code>
     *         if the key is not mapped and <code>mustExist</code> is
     *         <code>false</code>
     */
    public Object getPageData(final Object key, final boolean mustExist) {
        Check.notNull(key, "key"); //$NON-NLS-1$

        final Object data = pageData.get(key);

        if (mustExist && data == null) {
            final String messageFormat = "page data for key [{0}] does not exist"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, key);
            throw new IllegalStateException(message);
        }

        return data;
    }

    /**
     * Called to determine whether there is currently page data for the
     * specified key.
     *
     * @param key
     *        the page data key (must not be <code>null</code>)
     * @return <code>true</code> if there is page data for the key
     */
    public boolean hasPageData(final Object key) {
        Check.notNull(key, "key"); //$NON-NLS-1$

        return pageData.containsKey(key);
    }

    /**
     * <p>
     * Removes page data.
     * </p>
     *
     * <p>
     * {@link ExtendedWizard} subclasses typically will not override this
     * method.
     * </p>
     *
     * @param key
     *        the page data key (must not be <code>null</code>)
     * @return the page data object that was associated with the specified key,
     *         or <code>null</code> if the key was not mapped
     */
    public Object removePageData(final Object key) {
        return setPageData(key, null);
    }

    /**
     * <p>
     * Obtains an {@link ICommandExecutor} suitable for running commands in the
     * context of this {@link ExtendedWizard}.
     * </p>
     *
     * <p>
     * {@link ExtendedWizard} subclasses typically will not override this
     * method.
     * </p>
     *
     * @return an {@link ICommandExecutor} to use (never <code>null</code>)
     */
    public ICommandExecutor getCommandExecutor() {
        return UICommandExecutorFactory.newWizardCommandExecutor(getContainer());
    }

    /**
     * We override getPreviousPage in order to handle ExtendedWizard behavior
     * which adds all the pages up front. If they haven't been drawn, then we
     * return null.
     */
    @Override
    public IWizardPage getPreviousPage(final IWizardPage page) {
        final IWizardPage previousPage = super.getPreviousPage(page);

        if (previousPage == null || previousPage.getControl() == null) {
            return null;
        }

        return previousPage;
    }

    /**
     * Derived classes may want to override this method in order to properly add
     * pages dynamically. This is called from the ExtendedWizardPage class when
     * setPreviousPage is called.
     * 
     * @param currentPage
     * @param previousPage
     */
    public void previousPageSet(IWizardPage currentPage, IWizardPage previousPage) {
    }

    /**
     * Called by an {@link ExtendedWizardPage} when next is pressed. In the
     * standard wizard implementation this method is also called when enabling
     * the container's next button. {@link ExtendedWizard} handles this case
     * with the enableNext() method instead, and this method is only called when
     * a new page is needed to display. The main reason for this is that to
     * determine the next page, we often need page data that is produced when
     * the current page finishes (when next is clicked). This page data is not
     * available at the time we need to compute next button enablement.
     *
     * @param page
     *        the current page, or <code>null</code> if the first page is being
     *        requested
     * @return the next page to display, or <code>null</code> to stay on the
     *         current page
     */
    @Override
    public abstract IWizardPage getNextPage(IWizardPage page);

    /**
     * A method called by an {@link ExtendedWizardPage} that is part of this
     * wizard. This method is called when the wizard container asks the page
     * whether the next button should be enabled. This typically happens when
     * some event causes the wizard container to update its button enablement.
     * The page will only call this method if the page is complete - if the page
     * is incomplete, the page disallows the next button. This method can also
     * be called directly by the page to determine whether there is a page in
     * the wizard following the specified page. This information can be used to
     * enable an automatic next page behavior, such as double-clicking an
     * element. The wizard should usually return true, unless the specified page
     * is known to be the very last page of the wizard.
     *
     * @param page
     *        the current page (must not be <code>null</code>)
     * @return <code>true</code> if the next button should be enabled
     */
    public abstract boolean enableNext(IWizardPage page);

    /**
     * Called to determine whether the finish button should be enabled. This
     * method is only called if the current page (if there is one) is complete.
     * Typically this method should do two things. First, if the current page
     * will produce the page data needed to finish when the page itself
     * finishes, return <code>true</code>. Otherwise, test to see whether the
     * page data needed to finish is present ({@link #hasPageData(Object)}) and
     * if it is, return <code>true</code>.
     *
     * @param currentPage
     *        the complete current page if there is one, or <code>null</code> if
     *        there is not
     * @return <code>true</code> to enable the finish button
     */
    protected abstract boolean enableFinish(IWizardPage currentPage);

    /**
     * Called by the {@link #performFinish()}. This method is only invoked if
     * the current page and the wizard page finished callbacks succeed.
     * Subclasses override this method instead of overriding
     * {@link #performFinish()} for the finish logic.
     *
     * @return <code>true</code> to finish the wizard, <code>false</code> to
     *         keep the wizard open
     */
    protected abstract boolean doPerformFinish();

    /**
     * Called to determine whether this {@link ExtendedWizard} subclass supports
     * an extension control. An extension control allows the wizard to add a
     * control to the bottom of each page. If this method return
     * <code>true</code>, the
     * {@link #createExtensionControl(IWizardPage, Composite)} method will be
     * called for each page.
     *
     * @return <code>true</code> if this {@link ExtendedWizard} supports an
     *         extension control
     */
    public boolean hasExtensionControl() {
        return false;
    }

    /**
     * Called to create an extension control for the specified wizard page. This
     * method will only be called if {@link #hasExtensionControl()} returns
     * <code>true</code>. The {@link ExtendedWizard} subclass should create a
     * single {@link Control} that has the specified {@link Composite} as a
     * parent.
     *
     * @param page
     *        the page being extended (must not be <code>null</code>)
     * @param parent
     *        the parent {@link Composite} of the extension control (must not be
     *        <code>null</code>)
     */
    public void createExtensionControl(final IWizardPage page, final Composite parent) {
    }

    /**
     * <p>
     * This {@link IWizard} method is overridden to perform page and wizard page
     * finished callbacks. If these callbacks succeed, the
     * {@link #doPerformFinish()} method is called.
     * </p>
     *
     * <p>
     * {@link ExtendedWizard} subclasses typically will not override this
     * method. Instead, override {@link #doPerformFinish()} to perform wizard
     * finish logic.
     * </p>
     */
    @Override
    public boolean performFinish() {
        final IWizardPage currentPage = getContainer().getCurrentPage();

        try {
            if (currentPage instanceof ExtendedWizardPage) {
                if (!((ExtendedWizardPage) currentPage).onPageFinished()) {
                    return false;
                }
            }

            if (!onPageFinished(currentPage)) {
                return false;
            }
        } catch (final RuntimeException e) {
            final String titleFormat = Messages.getString("ExtendedWizard.ErrorDialogTitleFormat"); //$NON-NLS-1$
            final String title = MessageFormat.format(titleFormat, currentPage.getName());
            final String messageFormat = Messages.getString("ExtendedWizard.ErrorDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, currentPage.getName());

            ErrorDialog.openError(
                getShell(),
                title,
                null,
                new Status(Status.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, e));

            throw e;
        }

        try {
            return doPerformFinish();
        } catch (final RuntimeException e) {
            ErrorDialog.openError(
                getShell(),
                Messages.getString("ExtendedWizard.ErrorDialogTitle"), //$NON-NLS-1$
                null,
                new Status(
                    Status.ERROR,
                    TFSCommonUIClientPlugin.PLUGIN_ID,
                    0,
                    Messages.getString("ExtendedWizard.ErrorDialogText"), //$NON-NLS-1$
                    e));

            throw e;
        }
    }

    /*
     * This exists only so that we can provide a {@link #doPerformCancel} to
     * correspond to our sort of messy overriding of finished handlers.
     *
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.Wizard#performCancel()
     */
    @Override
    public boolean performCancel() {
        return doPerformCancel();
    }

    /**
     * Subclasses may override to perform cancellation handling
     *
     * @return true to cancel, false to block cancellation
     */
    protected boolean doPerformCancel() {
        return true;
    }

    /**
     * A callback for page finished notifications. This method is invoked when
     * next is clicked on a page. This method is also invoked when a wizard is
     * finished, but before the {@link #doPerformFinish()} method is invoked.
     *
     * @param page
     *        the page that is finished
     * @return <code>true</code> to continue navigation (or finish the wizard)
     *         or <code>false</code> to stay on the current page (or not finish
     *         the wizard)
     */
    protected boolean onPageFinished(final IWizardPage page) {
        /*
         * Called by the page when next is clicked. Return true to allow the
         * navigation to continue, or false to stay on the current page.
         */
        return true;
    }
}
