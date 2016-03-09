// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A {@link JobOptions} is a simple data holder class that holds a set of
 * configuration options used to create Eclipse {@link Job}s. A
 * {@link JobOptions}s instance is passed to a {@link JobCommandExecutor} during
 * construction, and that executor uses the option values when creating new
 * {@link Job}s.
 * </p>
 *
 * <p>
 * To configure {@link Job} attributes, call one of the public setter methods on
 * an instance of this class. If an attribute is not set, it holds a default
 * value that is usually appropriate for most {@link Job}s. The default values
 * are public API documented in this class.
 * </p>
 *
 * @see Job
 * @see JobCommandExecutor
 */
public class JobOptions {
    /**
     * The default priority {@link Job} priority, equal to {@link Job#LONG}.
     */
    public static final int DEFAULT_PRIORITY = Job.LONG;

    /**
     * The default {@link Job} scheduling delay, equal to <code>0</code> (no
     * delay).
     */
    public static final long DEFAULT_DELAY = 0;

    /**
     * The default {@link Job} <i>system</i> attribute, equal to
     * <code>false</code> (not a system job).
     */
    public static final boolean DEFAULT_SYSTEM = false;

    /**
     * The default {@link Job} <i>user</i> attribute, equal to
     * <code>false</code> (not a user job).
     */
    public static final boolean DEFAULT_USER = false;

    /**
     * The default {@link ICommandJobFactory}, which makes use of the
     * {@link JobCommandAdapter} class to create new {@link Job} instances.
     */
    public static final ICommandJobFactory DEFAULT_COMMAND_JOB_FACTORY = new DefaultCommandJobFactory();

    /**
     * The default {@link ISchedulingRule}, equal to <code>null</code>.
     */
    public static final ISchedulingRule DEFAULT_SCHEDULING_RULE = null;

    private int priority = DEFAULT_PRIORITY;
    private long delay = DEFAULT_DELAY;
    private boolean system = DEFAULT_SYSTEM;
    private boolean user = DEFAULT_USER;
    private ISchedulingRule schedulingRule = DEFAULT_SCHEDULING_RULE;
    private ICommandJobFactory commandJobFactory = DEFAULT_COMMAND_JOB_FACTORY;
    private final Map properties = new HashMap();

    /**
     * Creates a new {@link JobOptions} that holds default values for all
     * configuration data.
     */
    public JobOptions() {

    }

    /**
     * Creates a new {@link JobOptions}. If the specified {@link JobOptions}
     * instance is not <code>null</code>, all of the other instance's
     * configuration data will be copied into this instance.
     *
     * @param other
     *        another {@link JobOptions} instance to copy configuration data
     *        from, or <code>null</code>
     */
    public JobOptions(final JobOptions other) {
        if (other == null) {
            return;
        }

        priority = other.priority;
        delay = other.delay;
        system = other.system;
        user = other.user;
        schedulingRule = other.schedulingRule;
        commandJobFactory = other.commandJobFactory;
        properties.putAll(other.properties);
    }

    /**
     * Creates a new {@link Job} instance, using the {@link ICommandJobFactory}
     * held by this {@link JobOptions}.
     *
     * @param command
     *        an {@link ICommand} (must not be <code>null</code>)
     * @param commandStartedCallback
     *        an {@link ICommandStartedCallback} (may be <code>null</code>)
     * @param commandFinishedCallback
     *        an {@link ICommandFinishedCallback} (may be <code>null</code>)
     * @return a new {@link Job} instance as per the {@link ICommandJobFactory}
     *         contract
     */
    public Job createJobFor(
        final ICommand command,
        final ICommandStartedCallback commandStartedCallback,
        final ICommandFinishedCallback commandFinishedCallback) {
        return commandJobFactory.newJobFor(command, commandStartedCallback, commandFinishedCallback);
    }

    /**
     * Configures a {@link Job} instance using the configuration data held in
     * this {@link JobOptions}.
     *
     * @param job
     *        a {@link Job} to configure (must not be <code>null</code>)
     */
    public void configure(final Job job) {
        Check.notNull(job, "job"); //$NON-NLS-1$

        job.setPriority(priority);
        job.setSystem(system);
        job.setUser(user);
        job.setRule(schedulingRule);

        for (final Iterator it = properties.keySet().iterator(); it.hasNext();) {
            final QualifiedName key = (QualifiedName) it.next();
            final Object value = properties.get(key);

            job.setProperty(key, value);
        }
    }

    /**
     * Schedules a {@link Job} to run using the configuration data held in this
     * {@link JobOptions}.
     *
     * @param job
     *        a {@link Job} to schedule (must not be <code>null</code>)
     */
    public void schedule(final Job job) {
        job.schedule(delay);
    }

    /**
     * @return the priority value currently held by this {@link JobOptions}
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Sets the priority value of this {@link JobOptions} instance. The default
     * value is {@link #DEFAULT_PRIORITY}. When creating new {@link Job}s, the
     * value set here will be passed to {@link Job#setPriority(int)}.
     *
     * @param priority
     *        a {@link Job} priority value
     * @return this {@link JobOptions} instance for method chaining
     */
    public JobOptions setPriority(final int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * @return the delay value currently held by this {@link JobOptions}
     */
    public long getDelay() {
        return delay;
    }

    /**
     * Sets the scheduling delay value of this {@link JobOptions} instance. The
     * default value is {@link #DEFAULT_DELAY}. When scheduling new {@link Job}
     * s, the value set here will be passed to {@link Job#schedule(long)}.
     *
     * @param priority
     *        a {@link Job} scheduling delay value
     * @return this {@link JobOptions} instance for method chaining
     */
    public JobOptions setDelay(final long delay) {
        this.delay = delay;
        return this;
    }

    /**
     * @return the system option value currently held by this {@link JobOptions}
     */
    public boolean isSystem() {
        return system;
    }

    /**
     * Sets the system option value of this {@link JobOptions} instance. The
     * default value is {@link #DEFAULT_SYSTEM}. When creating new {@link Job}s,
     * the value set here will be passed to {@link Job#setSystem(boolean)}.
     *
     * @param priority
     *        a {@link Job} system value
     * @return this {@link JobOptions} instance for method chaining
     */
    public JobOptions setSystem(final boolean system) {
        this.system = system;
        return this;
    }

    /**
     * @return the user option value currently held by this {@link JobOptions}
     */
    public boolean isUser() {
        return user;
    }

    /**
     * Sets the user option value of this {@link JobOptions} instance. The
     * default value is {@link #DEFAULT_USER}. When creating new {@link Job}s,
     * the value set here will be passed to {@link Job#setUser(boolean)}.
     *
     * @param priority
     *        a {@link Job} user value
     * @return this {@link JobOptions} instance for method chaining
     */
    public JobOptions setUser(final boolean user) {
        this.user = user;
        return this;
    }

    /**
     * @return the {@link ISchedulingRule} currently held by this
     *         {@link JobOptions}
     */
    public ISchedulingRule getSchedulingRule() {
        return schedulingRule;
    }

    /**
     * Sets the scheduling rule of this {@link JobOptions} instance. The default
     * value is {@link #DEFAULT_SCHEDULING_RULE}. When creating new {@link Job}
     * s, the value set here will be passed to
     * {@link Job#setRule(ISchedulingRule)}.
     *
     * @param schedulingRule
     *        a {@link Job} scheduling rule, or <code>null</code> if the
     *        {@link Job} should not have a scheduling rule
     * @return this {@link JobOptions} instance for method chaining
     */
    public JobOptions setSchedulingRule(final ISchedulingRule schedulingRule) {
        this.schedulingRule = schedulingRule;
        return this;
    }

    /**
     * @return the {@link ICommandJobFactory} currently held by this
     *         {@link JobOptions}
     */
    public ICommandJobFactory getCommandJobFactory() {
        return commandJobFactory;
    }

    /**
     * Sets the command job factory of this {@link JobOptions} instance. The
     * default value is {@link #DEFAULT_COMMAND_JOB_FACTORY}. When creating new
     * {@link Job}s, the factory is used to obtain a {@link Job} instance from
     * an {@link ICommand}.
     *
     * @param commandJobFactory
     *        an {@link ICommandJobFactory} (must not be <code>null</code>)
     * @return this {@link JobOptions} instance for method chaining
     */
    public JobOptions setCommandJobFactory(final ICommandJobFactory commandJobFactory) {
        Check.notNull(commandJobFactory, "commandJobFactory"); //$NON-NLS-1$

        this.commandJobFactory = commandJobFactory;
        return this;
    }

    /**
     * Sets a {@link Job} property that will be set on new {@link Job}s created
     * using this {@link JobOptions} by calling
     * {@link Job#setProperty(QualifiedName, Object)}. The default is to have no
     * properties set.
     *
     * @param key
     *        the property key (must not be <code>null</code>)
     * @param value
     *        the property value
     */
    public void setProperty(final QualifiedName key, final Object value) {
        Check.notNull(key, "key"); //$NON-NLS-1$

        properties.put(key, value);
    }

    /**
     * A default implementation of {@link ICommandJobFactory}, which creates new
     * {@link JobCommandAdapter} instances to satisfy the
     * {@link #newJobFor(ICommand, ICommandFinishedCallback)} method.
     */
    private static class DefaultCommandJobFactory implements ICommandJobFactory {
        /*
         * (non-Javadoc)
         *
         * @see
         * com.microsoft.tfs.client.common.shared.command.ICommandJobFactory
         * #newJobFor(com.microsoft.tfs.client.common.shared.command.ICommand,
         * com
         * .microsoft.tfs.client.common.shared.command.ICommandFinishedCallback)
         */
        @Override
        public Job newJobFor(
            final ICommand command,
            final ICommandStartedCallback commandStartedCallback,
            final ICommandFinishedCallback commandFinishedCallback) {
            return new JobCommandAdapter(command, commandStartedCallback, commandFinishedCallback);
        }
    }
}
