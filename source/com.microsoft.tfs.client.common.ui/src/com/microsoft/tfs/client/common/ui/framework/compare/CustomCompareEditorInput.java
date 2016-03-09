// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareSaveEvent.CompareSaveNode;
import com.microsoft.tfs.client.common.ui.framework.runnable.SafeSubProgressMonitor;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * <p>
 * A custom {@link CompareEditorInput} for use with the Eclipse compare
 * framework. Normally, clients will not subclass this class.
 * </p>
 */
public class CustomCompareEditorInput extends CompareEditorInput {
    /**
     * The name of a property that can be set on the
     * {@link CompareConfiguration} used by this
     * {@link CustomCompareEditorInput} (see
     * {@link CompareConfiguration#setProperty(String, Object)}). If set, this
     * property specifies the {@link String} title used in the UI when
     * displaying this {@link CustomCompareEditorInput}. If not set, a default
     * title is computed using the differencer input objects.
     */
    public static final String TITLE_PROPERTY = "com.microsoft.tfs.client.common.ui.compare.Title"; //$NON-NLS-1$

    /**
     * The name of a property that can be set on the
     * {@link CompareConfiguration} used by this
     * {@link CustomCompareEditorInput} (see
     * {@link CompareConfiguration#setProperty(String, Object)}). If set, this
     * property specifies the {@link String} tooltip used in the UI when
     * displaying this {@link CustomCompareEditorInput}. If not set, a default
     * tooltip is computed using the differencer input objects.
     */
    public static final String TOOLTIP_PROPERTY = "com.microsoft.tfs.client.common.ui.compare.Tooltip"; //$NON-NLS-1$

    private final Object modified;
    private final Object original;
    private final Object ancestor;
    private final ContentComparator[] comparators;
    private final ExternalCompareHandler externalCompareHandler;

    private String tooltip;

    /**
     * Whether the OK button was pressed (only valid for dialog UI types).
     */
    private boolean okPressed;

    /**
     * Whether the input is always dirty (only useful for dialog UI types).
     */
    private boolean alwaysDirty;

    /**
     * A list of saved contents (valid for all UI types.)
     */
    private final List<ISaveableCompareElement> savedContents = new ArrayList<ISaveableCompareElement>();

    private final SingleListenerFacade saveListeners = new SingleListenerFacade(CompareSaveListener.class);

    /**
     * Creates a new {@link CustomCompareEditorInput} with the specified input
     * objects. Each input object should either be an appropriate differencer
     * input object or should be an object that implements
     * {@link DifferencerInputGenerator}.
     *
     * @param modified
     *        the modified input object (must not be <code>null</code>)
     * @param original
     *        the original input object (must not be <code>null</code>)
     * @param ancestor
     *        the ancestor input object, or <code>null</code> for a 2-way
     *        compare
     * @param comparators
     *        {@link ContentComparator}s to pass to a {@link CustomDifferencer}
     *        during the compare operation, or <code>null</code>
     * @param compareConfiguration
     *        the {@link CompareConfiguration} to use, or <code>null</code> to
     *        create a default {@link CompareConfiguration}
     * @param externalCompareHandler
     *        an {@link ExternalCompareHandler} to use, or <code>null</code> for
     *        no external compare callback
     */
    public CustomCompareEditorInput(
        final Object modified,
        final Object original,
        final Object ancestor,
        final ContentComparator[] comparators,
        final CompareConfiguration compareConfiguration,
        final ExternalCompareHandler externalCompareHandler) {
        super(compareConfiguration != null ? compareConfiguration : new CustomCompareConfiguration());

        Check.notNull(modified, "modified"); //$NON-NLS-1$
        Check.notNull(original, "original"); //$NON-NLS-1$

        this.modified = modified;
        this.original = original;
        this.ancestor = ancestor;
        this.comparators = (comparators != null ? comparators.clone() : null);
        this.externalCompareHandler = externalCompareHandler;
    }

    /**
     *
     *
     * @param saveAlwaysNeeded
     */
    public void setAlwaysDirty(final boolean alwaysDirty) {
        this.alwaysDirty = alwaysDirty;
    }

    @Override
    public boolean isSaveNeeded() {
        if (alwaysDirty) {
            return true;
        }

        return super.isSaveNeeded();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.compare.CompareEditorInput#prepareInput(org.eclipse.core.
     * runtime.IProgressMonitor)
     */
    @Override
    protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        monitor.beginTask(Messages.getString("CustomCompareEditorInput.ProgressText"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$

        try {
            final Object modifiedObject = getInputObject(modified, monitor);
            final Object originalObject = getInputObject(original, monitor);
            final Object ancestorObject = getInputObject(ancestor, monitor);

            final boolean threeWay = (ancestorObject != null);

            if (externalCompareHandler != null) {
                checkForCancelation(monitor);

                final IProgressMonitor externalCompareMonitor = new SafeSubProgressMonitor(monitor, 1);
                final boolean externalComparePerformed = externalCompareHandler.onCompare(
                    threeWay,
                    externalCompareMonitor,
                    modifiedObject,
                    originalObject,
                    ancestorObject);
                externalCompareMonitor.done();

                if (externalComparePerformed) {
                    /*
                     * External compare has succeeded. Simulate a user
                     * cancellation since that's the only way to stop the
                     * internal compare process without showing some sort of
                     * dialog.
                     */
                    throw new InterruptedException();
                }
            }

            checkForCancelation(monitor);

            setupTitleAndTooltip(threeWay, modifiedObject, originalObject, ancestorObject);

            hookForSaving(modifiedObject);
            hookForSaving(originalObject);

            final CustomDifferencer differencer = new CustomDifferencer(comparators);

            final IProgressMonitor differencerMonitor = new SafeSubProgressMonitor(monitor, 1);

            return differencer.findDifferences(
                threeWay,
                differencerMonitor,
                null,
                ancestorObject,
                modifiedObject,
                originalObject);
        } catch (final InterruptedException e) {
            throw e;
        } catch (final InvocationTargetException e) {
            throw e;
        } catch (final Exception e) {
            setMessage(e.getLocalizedMessage());
            return null;
        } finally {
            monitor.done();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.compare.CompareEditorInput#getToolTipText()
     */
    @Override
    public String getToolTipText() {
        if (tooltip != null) {
            return tooltip;
        }
        return super.getToolTipText();
    }

    @Override
    public boolean canRunAsJob() {
        /*
         * We may need to make this a parameter that can be passed in the
         * constructor. Original now, all known uses will be fine running as a
         * job. Note that even if canRunAsJob is false, prepareInput() will
         * still be run off the UI thread (it will be run using the workbench
         * progress service).
         */
        return true;
    }

    private void checkForCancelation(final IProgressMonitor monitor) throws InterruptedException {
        if (monitor.isCanceled()) {
            throw new InterruptedException();
        }
    }

    private void setupTitleAndTooltip(
        final boolean threeWay,
        final Object modifiedObject,
        final Object originalObject,
        final Object ancestorObject) {
        String title = (String) getCompareConfiguration().getProperty(TITLE_PROPERTY);
        if (title == null) {
            title = computeTitle(threeWay, modifiedObject, originalObject, ancestorObject);
        }
        if (title != null) {
            setTitle(title);
        }

        tooltip = (String) getCompareConfiguration().getProperty(TOOLTIP_PROPERTY);
        if (tooltip == null) {
            tooltip = computeTooltip(threeWay, modifiedObject, originalObject, ancestorObject);
        }
    }

    private String computeTooltip(
        final boolean threeWay,
        final Object modifiedObject,
        final Object originalObject,
        final Object ancestorObject) {
        final String modifiedLabel = CompareUtils.getLabel(modifiedObject);
        final String originalLabel = CompareUtils.getLabel(originalObject);
        final String ancestorLabel = threeWay ? CompareUtils.getLabel(ancestorObject) : null;

        if (threeWay) {
            if (modifiedLabel != null && originalLabel != null && ancestorLabel != null) {
                final String messageFormat = Messages.getString("CustomCompareEditorInput.ThreeWayTooltipFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, modifiedLabel, originalLabel, ancestorLabel);
            }
        } else {
            if (modifiedLabel != null && originalLabel != null) {
                final String messageFormat = Messages.getString("CustomCompareEditorInput.TwoWayTooltipFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, modifiedLabel, originalLabel);
            }
        }

        return null;
    }

    private String computeTitle(
        final boolean threeWay,
        final Object modifiedObject,
        final Object originalObject,
        final Object ancestorObject) {
        final String modifiedName =
            (modifiedObject instanceof ITypedElement) ? ((ITypedElement) modifiedObject).getName() : null;
        final String originalName =
            (originalObject instanceof ITypedElement) ? ((ITypedElement) originalObject).getName() : null;
        final String ancestorName =
            threeWay && (ancestorObject instanceof ITypedElement) ? ((ITypedElement) ancestorObject).getName() : null;

        if (threeWay) {
            if (modifiedName != null && originalName != null && ancestorName != null) {
                final String messageFormat = Messages.getString("CustomCompareEditorInput.ThreeWayTitleFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, ancestorName, modifiedName, originalName);
            }
        } else {
            if (modifiedName != null && originalName != null) {
                final String messageFormat = Messages.getString("CustomCompareEditorInput.TwoWayTitleFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, modifiedName, originalName);
            }
        }

        return null;
    }

    public void setOKPressed() {
        okPressed = true;
    }

    @Override
    public boolean okPressed() {
        setOKPressed();
        return super.okPressed();
    }

    public boolean wasOKPressed() {
        return okPressed;
    }

    private void hookForSaving(final Object inputObject) {
        if (!(inputObject instanceof ISaveableCompareElement)) {
            return;
        }

        final ISaveableCompareElement saveableCompareElement = (ISaveableCompareElement) inputObject;
        saveableCompareElement.addContentChangeListener(new IContentChangeListener() {
            @Override
            public void contentChanged(final IContentChangeNotifier source) {
                boolean success = false;

                try {
                    saveableCompareElement.save(new NullProgressMonitor());
                    success = true;
                } catch (final CoreException e) {
                    final IStatus status = new Status(
                        IStatus.ERROR,
                        TFSCommonUIClientPlugin.PLUGIN_ID,
                        0,
                        Messages.getString("CustomCompareEditorInput.UnableToSave"), //$NON-NLS-1$
                        e);

                    TFSCommonUIClientPlugin.getDefault().getLog().log(status);
                }

                if (success) {
                    synchronized (savedContents) {
                        savedContents.add(saveableCompareElement);
                    }

                    final CompareSaveNode node;

                    if (ancestor == saveableCompareElement) {
                        node = CompareSaveNode.ANCESTOR;
                    } else if (modified == saveableCompareElement) {
                        node = CompareSaveNode.MODIFIED;
                    } else if (original == saveableCompareElement) {
                        node = CompareSaveNode.ORIGINAL;
                    } else {
                        node = CompareSaveNode.UNKNOWN;
                    }

                    ((CompareSaveListener) saveListeners.getListener()).onCompareElementSaved(
                        new CompareSaveEvent(node, saveableCompareElement));
                }
            }
        });
    }

    public void addSaveListener(final CompareSaveListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        saveListeners.addListener(listener);
    }

    public void removeSaveListener(final CompareSaveListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        saveListeners.removeListener(listener);
    }

    /**
     * Returns the provided inputs that were saved during this compare.
     *
     * @return An array of {@link ISaveableCompareElement} objects that were
     *         saved during the comparison (never <code>null</code>)
     */
    public ISaveableCompareElement[] getSavedContents() {
        synchronized (savedContents) {
            return savedContents.toArray(new ISaveableCompareElement[savedContents.size()]);
        }
    }

    private Object getInputObject(final Object inputObject, final IProgressMonitor mainMonitor)
        throws InvocationTargetException,
            InterruptedException {
        if (mainMonitor.isCanceled()) {
            throw new InterruptedException();
        }

        if (!(inputObject instanceof DifferencerInputGenerator)) {
            return inputObject;
        }

        final DifferencerInputGenerator inputGenerator = (DifferencerInputGenerator) inputObject;

        final IProgressMonitor subMonitor = new SafeSubProgressMonitor(mainMonitor, 1);
        final Object input = inputGenerator.getInput(subMonitor);
        subMonitor.done();

        return input;
    }

    /**
     * Override the label on the <code>OK</code> button to use the text "Save"
     * when in editable mode. The default editable mode label is "Commit", which
     * sounds too close to a source control operation.
     */
    @Override
    public String getOKButtonLabel() {
        if (getCompareConfiguration().isLeftEditable() || getCompareConfiguration().isRightEditable()) {
            return Messages.getString("CustomCompareEditorInput.SaveButtonLabel"); //$NON-NLS-1$
        }

        return super.getOKButtonLabel();
    }
}
