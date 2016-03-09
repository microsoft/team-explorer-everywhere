// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.util.Check;

/**
 * This is an autocompleting Combo box.
 *
 * This extends Combo, which - although the SWT documentation says is relatively
 * unsafe - should be okay as long as you stick to calling public methods.
 */
public class AutocompleteCombo extends Combo {
    private final static Log log = LogFactory.getLog(AutocompleteCombo.class);

    /**
     * Number of items after which setItems on GTK will batch adds to the widget
     * in a background thread (which posts runnables to the UI thread)
     */
    private static final int BATCH_TRIGGER_MIN = 500;

    /**
     * Number of items to add in one batch in a UI runnable with setItems on GTK
     * with large item counts
     */
    private static final int MAX_BATCH_SIZE = 100;

    /* The autocompletion listener - deals with combo events */
    private final AutocompleteComboListener listener;

    /* Case sensitivity for autocompletion */
    private boolean caseSensitive = false;

    /* True if the supplied items are in sorted order */
    private boolean sorted = false;

    /* A lower case copy of the supplied items */
    private String[] lowerItems;

    /* A copy of the supplied items */
    private String[] items;

    /* Some events (setText(), select()) will cause modify events we ignore. */
    private int suppressAutocomplete = 0;

    /**
     * Thread allocated on demand (set to null when done) to do batch adds of
     * items items in the background on GTK.
     */
    private Thread setItemsThread;
    private final Object setItemsThreadLock = new Object();

    public AutocompleteCombo(final Composite parent, final int style) {
        super(parent, style);

        listener = new AutocompleteComboListener();
        addModifyListener(listener);
        addKeyListener(listener);
        addFocusListener(listener);
    }

    @Override
    protected void checkSubclass() {
        /* Allow ourselves to subclass */
    }

    public void setCaseSensitive(final boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    @Override
    public String[] getItems() {
        return items;
    }

    @Override
    public String getItem(final int index) {
        return items == null ? "" : items[index]; //$NON-NLS-1$
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.length;
    }

    @Override
    public void setItems(final String[] items) {
        Check.notNull(items, "items"); //$NON-NLS-1$

        /*
         * Suppress the next autocompletion, since it will be fired by the
         * setItems() call.
         */
        suppressAutocomplete++;

        sorted = true;
        lowerItems = new String[items.length];
        this.items = new String[items.length];

        for (int i = 0; i < items.length; i++) {
            this.items[i] = items[i];
            lowerItems[i] = items[i].toLowerCase();
            if (i > 0 && sorted && lowerItems[i].compareTo(lowerItems[i - 1]) < 0) {
                sorted = false;
            }
        }

        internalSetItems();
    }

    @Override
    public void setItem(final int index, final String string) {
        items[index] = string;
        internalSetItems();
    }

    @Override
    public void setText(final String text) {
        /*
         * Suppress the next autocompletion, since it will be fired by the
         * setText() call.
         */
        suppressAutocomplete++;
        super.setText(text);
    }

    @Override
    public void select(final int idx) {
        suppressAutocomplete++;
        super.select(idx);
    }

    /**
     * Sets items in the superclass. On GTK adding large numbers of items is
     * very slow (adding 4700 items can take 5 seconds on a desktop in 2010), so
     * a batched strategy is used to keep from tying up the UI thread.
     */
    private void internalSetItems() {
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.GTK) && items.length >= BATCH_TRIGGER_MIN) {
            log.debug(
                MessageFormat.format(
                    "Breaking {0} items in combo into batches of {1} to work-around slow GTK combo", //$NON-NLS-1$
                    items.length,
                    MAX_BATCH_SIZE));

            /*
             * Start a background thread which posts runnables to the UI thread
             * to add small-ish batches of items. This lets the UI thread stay
             * alive between batches.
             */
            synchronized (setItemsThreadLock) {
                cancelSetItemsThread();

                final String[] itemsClone = items.clone();
                final Shell shell = getShell();

                setItemsThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        /*
                         * Divide the list into batches and queue runnables to
                         * add a batch until all done.
                         */
                        int i = 0;
                        while (i < itemsClone.length) {
                            if (Thread.interrupted() || shell.isDisposed()) {
                                return;
                            }

                            final int batchEndIndexExclusive = Math.min(i + MAX_BATCH_SIZE, itemsClone.length);

                            UIHelpers.runOnUIThread(
                                shell,
                                false,
                                new AddListItemsRunnable(
                                    AutocompleteCombo.this,
                                    itemsClone,
                                    i,
                                    batchEndIndexExclusive));

                            i = batchEndIndexExclusive;
                        }
                    }
                });

                setItemsThread.start();
            }
        } else {
            cancelSetItemsThread();

            super.setItems(items);
        }
    }

    /**
     * Cancels the {@link #setItemsThread} if it is running and waits for it to
     * die.
     */
    private void cancelSetItemsThread() {
        synchronized (setItemsThreadLock) {
            if (setItemsThread != null) {
                setItemsThread.interrupt();

                /*
                 * Do not join the thread because it's almost impossible to
                 * avoid deadlocks. The thread's job is to post runnables on the
                 * UI thread, and those runnables can block until our join
                 * completes: deadlock. Just interrupt to let the thread wind
                 * down normally (it tests for interrupt and widget dispose).
                 */

                setItemsThread = null;
            }
        }
    }

    /**
     * Runnable used for GTK to add one batch of items to the combo. Must be run
     * on the UI thread.
     *
     * @threadsafety thread-compatible
     */
    private static class AddListItemsRunnable implements Runnable {
        private final AutocompleteCombo combo;
        private final String[] items;
        private final int startIndexInclusive;
        private final int endIndexExclusive;

        public AddListItemsRunnable(
            final AutocompleteCombo combo,
            final String[] items,
            final int startIndexInclusive,
            final int endIndexExclusive) {
            this.combo = combo;
            this.items = items;
            this.startIndexInclusive = startIndexInclusive;
            this.endIndexExclusive = endIndexExclusive;
        }

        @Override
        public void run() {
            if (combo.isDisposed()) {
                return;
            }

            for (int i = startIndexInclusive; i < endIndexExclusive; i++) {
                combo.add(items[i]);
            }
        }
    }

    private class AutocompleteComboListener implements ModifyListener, KeyListener, FocusListener {
        private int lastMatch = -1;

        @Override
        public void modifyText(final ModifyEvent e) {
            if (suppressAutocomplete > 0) {
                suppressAutocomplete--;
                return;
            }

            if (items == null) {
                return;
            }

            final String text = getText();
            if (text.length() > 0) {
                int index;
                if (caseSensitive) {
                    index = match(items, text);
                } else {
                    index = match(lowerItems, text.toLowerCase());
                }

                if (index != -1) {
                    /*
                     * Suppress one extra for GTK because there will be an
                     * additional modify event fired after the call below to
                     * setText(), and we want to ignore this. I can't find a way
                     * to keep GTK from firing this event. Probably a GTK bug.
                     */
                    if (WindowSystem.isCurrentWindowSystem(WindowSystem.GTK)) {
                        suppressAutocomplete++;
                    }

                    /*
                     * Autocomplete the text field: fill in the entirety of the
                     * first match, highlighting any portion that the user
                     * didn't type.
                     */
                    setText(items[index]);

                    if (text.length() <= items[index].length()) {
                        setSelection(new Point(text.length(), items[index].length()));
                    }
                }
            }
        }

        private int match(final String[] items, final String text) {
            if (sorted) {
                int low = 0;
                int high = items.length - 1;
                int mid = lastMatch >= 0 && lastMatch < items.length ? lastMatch : (low + high) / 2;
                boolean match = false;

                while (low <= high && !match) {
                    match = items[mid].startsWith(text);

                    if (!match) {
                        final int result = items[mid].compareTo(text);
                        if (result < 0) {
                            low = mid + 1;
                        } else {
                            high = mid - 1;
                        }

                        mid = (low + high) / 2;
                    }
                }

                if (match) {
                    int index = mid - 1;
                    while (index >= 0 && items[index].startsWith(text)) {
                        index--;
                    }

                    lastMatch = index + 1;
                    return lastMatch;
                } else {
                    lastMatch = -1;
                    return -1;
                }
            } else {
                for (int i = 0; i < items.length; i++) {
                    if (items[i].startsWith(text)) {
                        return i;
                    }
                }
            }
            return -1;
        }

        @Override
        public void keyPressed(final KeyEvent e) {
            if (e.keyCode == SWT.DEL || e.keyCode == SWT.BS || e.keyCode == SWT.ESC) {
                final int caretPosition = getSelection().x;
                final int textLength = getText().length();

                if (e.keyCode == SWT.DEL && caretPosition < textLength) {
                    suppressAutocomplete++;
                } else if (e.keyCode == SWT.BS && caretPosition > 0) {
                    suppressAutocomplete++;
                } else if (e.keyCode == SWT.ESC) {
                    suppressAutocomplete++;
                }
            }
        }

        @Override
        public void keyReleased(final KeyEvent e) {
        }

        @Override
        public void focusGained(final FocusEvent e) {
            /*
             * Select all text initially so the user can start typing.
             */
            final int textlength = getText().length();
            setSelection(new Point(0, textlength));
        }

        @Override
        public void focusLost(final FocusEvent e) {
            clearSelection();
        }
    }
}
