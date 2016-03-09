// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import org.eclipse.jface.action.IAction;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.AbstractValidatorBinding;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.ValidatorBinding;

/**
 * {@link ActionValidatorBinding} is a {@link ValidatorBinding} implementation
 * that binds a {@link Validator} to one or more {@link IAction}s. The action
 * enablement is bound to the {@link Validator}'s validation state.
 *
 * @see ValidatorBinding
 * @see Validator
 * @see IAction
 */
public class ActionValidatorBinding extends AbstractValidatorBinding {
    private final IAction[] actions;

    /**
     * Creates a new {@link ActionValidatorBinding} that binds the specified
     * {@link IAction}'s enablement to a {@link Validator}'s validation state.
     *
     * @param action
     *        the {@link IAction} to bind (must not be <code>null</code>)
     */
    public ActionValidatorBinding(final IAction action) {
        Check.notNull(action, "action"); //$NON-NLS-1$

        actions = new IAction[] {
            action
        };
    }

    /**
     * Creates a new {@link ActionValidatorBinding} that binds the specified
     * {@link IAction}'s enablement to a {@link Validator}'s validation state.
     *
     * @param actions
     *        the {@link IAction}s to bind (must not be <code>null</code> or
     *        contain <code>null</code> elements)
     */
    public ActionValidatorBinding(final IAction[] actions) {
        Check.notNull(actions, "actions"); //$NON-NLS-1$

        this.actions = actions;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.AbstractValidatorBinding#update(com.
     * microsoft .tfs.util.valid.IValidity)
     */
    @Override
    protected void update(final IValidity validity) {
        final boolean enable = validity == null || validity.isValid();

        for (int i = 0; i < actions.length; i++) {
            actions[i].setEnabled(enable);
        }
    }
}
