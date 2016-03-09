/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/util/TimeoutController.java,v 1.6
 * 2004/04/18 23:51:38 jsdever Exp $ $Revision: 480424 $ $Date: 2006-11-29
 * 06:56:49 +0100 (Wed, 29 Nov 2006) $
 *
 * ====================================================================
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */

package com.microsoft.tfs.core.httpclient.util;

/**
 * <p>
 * Executes a task with a specified timeout.
 * </p>
 *
 * @author Ortwin Glueck
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @version $Revision: 480424 $
 * @since 2.0
 */
public final class TimeoutController {

    /**
     * Do not instantiate objects of this class. Methods are static.
     */
    private TimeoutController() {
    }

    /**
     * Executes <code>task</code>. Waits for <code>timeout</code> milliseconds
     * for the task to end and returns. If the task does not return in time, the
     * thread is interrupted and an Exception is thrown. The caller should
     * override the Thread.interrupt() method to something that quickly makes
     * the thread die or use Thread.isInterrupted().
     *
     * @param task
     *        The thread to execute
     * @param timeout
     *        The timeout in milliseconds. 0 means to wait forever.
     * @throws TimeoutException
     *         if the timeout passes and the thread does not return.
     */
    public static void execute(final Thread task, final long timeout) throws TimeoutException {
        task.start();
        try {
            task.join(timeout);
        } catch (final InterruptedException e) {
            /* if somebody interrupts us he knows what he is doing */
        }
        if (task.isAlive()) {
            task.interrupt();
            throw new TimeoutException();
        }
    }

    /**
     * Executes <code>task</code> in a new deamon Thread and waits for the
     * timeout.
     *
     * @param task
     *        The task to execute
     * @param timeout
     *        The timeout in milliseconds. 0 means to wait forever.
     * @throws TimeoutException
     *         if the timeout passes and the thread does not return.
     */
    public static void execute(final Runnable task, final long timeout) throws TimeoutException {
        final Thread t = new Thread(task, "Timeout guard");
        t.setDaemon(true);
        execute(t, timeout);
    }

    /**
     * Signals that the task timed out.
     */
    public static class TimeoutException extends Exception {
        /** Create an instance */
        public TimeoutException() {
        }
    }
}
