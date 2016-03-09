// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies;

import com.microsoft.tfs.core.checkinpolicies.PolicyDefinition;
import com.microsoft.tfs.core.checkinpolicies.PolicyEditArgs;
import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;
import com.microsoft.tfs.core.checkinpolicies.PolicyType;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.util.Check;

public abstract class PolicyConfiguration {
    public static PolicyConfiguration configurationFor(
        final PolicyInstance instance,
        final boolean enabled,
        final int priority,
        final String[] scopeExpressions) {
        Check.notNull(instance, "instance"); //$NON-NLS-1$

        return new PolicyInstanceConfiguration(instance, enabled, priority, scopeExpressions);
    }

    public static PolicyConfiguration configurationFor(final PolicyDefinition definition) {
        Check.notNull(definition, "definition"); //$NON-NLS-1$

        return new PolicyDefinitionConfiguration(
            definition.getConfigurationMemento(),
            definition.getType(),
            definition.isEnabled(),
            definition.getPriority(),
            definition.getScopeExpressions());
    }

    private boolean enabled;
    private int priority;
    private String[] scopeExpressions;

    private PolicyConfiguration(final boolean enabled, final int priority, final String[] scopeExpressions) {
        Check.notNull(scopeExpressions, "scopeExpressions"); //$NON-NLS-1$

        this.enabled = enabled;
        this.priority = priority;
        this.scopeExpressions = scopeExpressions;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
    }

    public String[] getScopeExpressions() {
        return scopeExpressions;
    }

    public void setScopeExpressions(final String[] scopeExpressions) {
        Check.notNull(scopeExpressions, "scopeExpressions"); //$NON-NLS-1$
        this.scopeExpressions = scopeExpressions;
    }

    public abstract PolicyType getType();

    public abstract boolean canEdit();

    public abstract boolean edit(PolicyEditArgs policyEditArgs);

    public abstract PolicyDefinition toDefinition();

    public abstract boolean hasInstance();

    private static class PolicyInstanceConfiguration extends PolicyConfiguration {
        private final PolicyInstance instance;

        public PolicyInstanceConfiguration(
            final PolicyInstance instance,
            final boolean enabled,
            final int priority,
            final String[] scopeExpressions) {
            super(enabled, priority, scopeExpressions);
            this.instance = instance;
        }

        @Override
        public boolean canEdit() {
            return instance.canEdit();
        }

        @Override
        public boolean edit(final PolicyEditArgs policyEditArgs) {
            return instance.edit(policyEditArgs);
        }

        @Override
        public PolicyType getType() {
            return instance.getPolicyType();
        }

        @Override
        public PolicyDefinition toDefinition() {
            return new PolicyDefinition(instance, isEnabled(), getPriority(), getScopeExpressions());
        }

        @Override
        public boolean hasInstance() {
            return true;
        }
    }

    private static class PolicyDefinitionConfiguration extends PolicyConfiguration {
        private final Memento memento;
        private final PolicyType type;

        public PolicyDefinitionConfiguration(
            final Memento memento,
            final PolicyType type,
            final boolean enabled,
            final int priority,
            final String[] scopeExpressions) {
            super(enabled, priority, scopeExpressions);
            this.memento = memento;
            this.type = type;
        }

        @Override
        public boolean canEdit() {
            return false;
        }

        @Override
        public boolean edit(final PolicyEditArgs policyEditArgs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PolicyType getType() {
            return type;
        }

        @Override
        public PolicyDefinition toDefinition() {
            return new PolicyDefinition(type, memento, isEnabled(), getPriority(), getScopeExpressions());
        }

        @Override
        public boolean hasInstance() {
            return false;
        }
    }
}
