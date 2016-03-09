// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.tasks;

import java.util.LinkedList;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link TaskMonitorService} is a service that is used to manage
 * {@link TaskMonitor} instances. Upper layer code uses
 * {@link TaskMonitorService} to make {@link TaskMonitor}s available to lower
 * layer code. Lower layer code that implements a long-running task uses
 * {@link TaskMonitorService} to obtain a {@link TaskMonitor} instance to use
 * for the task.
 * </p>
 *
 * <p>
 * {@link TaskMonitorService} exists for two reasons. First, it ensures that
 * {@link TaskMonitor}s can be passed across layers without having to explicitly
 * pass them by method arguments from layer to layer. This is accomplished
 * through the use of thread local storage. The upper layer that makes a task
 * monitor available must run on the same thread as the lower layer that
 * retrieves the {@link TaskMonitor}.
 * </p>
 *
 * <p>
 * The second reason the {@link TaskMonitorService} exists is to decouple the
 * layer that produces a {@link TaskMonitor} from the layer that consumes it.
 * Either layer may be made {@link TaskMonitor}-aware without the other layer
 * being {@link TaskMonitor} aware. If the upper layer produces a
 * {@link TaskMonitor} and nothing consumes it, no progress monitoring or
 * cancelation will occur but everything will still work. If the lower layer
 * asks for a {@link TaskMonitor} and one has not been set, a dummy
 * implementation is returned.
 * </p>
 *
 * <p>
 * Within each thread, the {@link TaskMonitorService} is stack based. Each
 * {@link TaskMonitor} producer calls {@link #pushTaskMonitor(TaskMonitor)} to
 * add a {@link TaskMonitor} to the service. This call <b>must</b> be matched by
 * a call to {@link #popTaskMonitor()}, which should occur in the same layer
 * that pushed it. The stack-based implementation is intended mostly to support
 * sub-tasking in which sub-{@link TaskMonitor}s are pushed on top of their
 * parents.
 * </p>
 *
 * @see TaskMonitor
 */
public class TaskMonitorService {
    /**
     * The {@link ThreadLocal} object that stores a {@link TaskMonitorStack} for
     * each thread.
     */
    private static final TaskMonitorThreadLocal taskMonitorStorage = new TaskMonitorThreadLocal();

    /**
     * Called to retrieve a {@link TaskMonitor} from this
     * {@link TaskMonitorService}. This method will always return a usable
     * {@link TaskMonitor}, even if one has not been set into the service.
     *
     * @return a {@link TaskMonitor} to use (never <code>null</code>)
     */
    public static TaskMonitor getTaskMonitor() {
        final TaskMonitor taskMonitor = taskMonitorStorage.getStack().peek();
        if (taskMonitor == null) {
            return NullTaskMonitor.INSTANCE;
        }
        return taskMonitor;
    }

    /**
     * Called to push a {@link TaskMonitor} onto this thread's
     * {@link TaskMonitor} stack. This call <b>must</b> be matched by a call to
     * {@link #popTaskMonitor()}. This requirement implies the use of
     * <code>try</code>/<code>finally</code> to satisfy it.
     *
     * @param taskMonitor
     *        a {@link TaskMonitor} to push (must not be <code>null</code>)
     */
    public static void pushTaskMonitor(final TaskMonitor taskMonitor) {
        Check.notNull(taskMonitor, "taskMonitor"); //$NON-NLS-1$

        taskMonitorStorage.getStack().push(taskMonitor);
    }

    /**
     * <p>
     * Called to pop a {@link TaskMonitor} from this thread's
     * {@link TaskMonitor} stack. This call <b>must</b> be matched with a
     * previous call to {@link #pushTaskMonitor(TaskMonitor)}. This requirement
     * implies the use of <code>try</code>/<code>finally</code> to satisfy it.
     * </p>
     *
     * <p>
     * Before this method returns, {@link TaskMonitor#done()} is called on the
     * popped {@link TaskMonitor}. This is almost always safe and the right
     * thing to do (<code>done()</code> can be safely called multiple times). In
     * the rare case that this is not desired, call
     * {@link #popTaskMonitor(boolean)} instead.
     * </p>
     *
     * @return the {@link TaskMonitor} that was popped off the stack
     */
    public static TaskMonitor popTaskMonitor() {
        return popTaskMonitor(true);
    }

    /**
     * Called to pop a {@link TaskMonitor} from this thread's
     * {@link TaskMonitor} stack. This call <b>must</b> be matched with a
     * previous call to {@link #pushTaskMonitor(TaskMonitor)}. This requirement
     * implies the use of <code>try</code>/<code>finally</code> to satisfy it.
     *
     * @param callDone
     *        if <code>true</code>, {@link TaskMonitor#done()} will be called on
     *        the popped {@link TaskMonitor} before it is returned
     * @return the {@link TaskMonitor} that was popped off the stack
     */
    public static TaskMonitor popTaskMonitor(final boolean callDone) {
        final TaskMonitor taskMonitor = taskMonitorStorage.getStack().pop();
        if (taskMonitor == null) {
            throw new IllegalStateException("there are no TaskMonitors on the stack"); //$NON-NLS-1$
        }

        if (callDone) {
            taskMonitor.done();
        }

        return taskMonitor;
    }

    /**
     * A private subclass of {@link ThreadLocal} that sets each thread's initial
     * value to a new instance of {@link TaskMonitorStack}.
     */
    private static class TaskMonitorThreadLocal extends ThreadLocal {
        /*
         * (non-Javadoc)
         *
         * @see java.lang.ThreadLocal#initialValue()
         */
        @Override
        protected Object initialValue() {
            return new TaskMonitorStack();
        }

        /**
         * @return the {@link TaskMonitorStack} associated with the current
         *         thread (never <code>null</code>)
         */
        public TaskMonitorStack getStack() {
            return (TaskMonitorStack) get();
        }
    }

    /**
     * A simple stack class used to store a stack of {@link TaskMonitor}
     * instances.
     */
    private static class TaskMonitorStack {
        /**
         * The stack, implemented as a {@link LinkedList}.
         */
        private final LinkedList stack = new LinkedList();

        /**
         * Pushes a new {@link TaskMonitor} instance onto the stack. No checking
         * of the argument is done.
         *
         * @param taskMonitor
         *        the {@link TaskMonitor} instance to push
         */
        public void push(final TaskMonitor taskMonitor) {
            stack.addFirst(taskMonitor);
        }

        /**
         * Pops the top {@link TaskMonitor} off of the stack and returns it. If
         * the stack is empty, returns <code>null</code>.
         *
         * @return the top {@link TaskMonitor} or <code>null</code>
         */
        public TaskMonitor pop() {
            if (stack.size() == 0) {
                return null;
            }
            final TaskMonitor taskMonitor = (TaskMonitor) stack.removeFirst();
            return taskMonitor;
        }

        /**
         * Obtains the top {@link TaskMonitor} on the stack. If the stack is
         * empty, returns <code>null</code>.
         *
         * @return the top {@link TaskMonitor} or <code>null</code>
         */
        public TaskMonitor peek() {
            if (stack.size() == 0) {
                return null;
            }
            return (TaskMonitor) stack.getFirst();
        }
    }

    private TaskMonitorService() {
    }
}
