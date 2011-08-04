/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ejb3.timerservice.integration.test.cancel.unit;

import junit.framework.Assert;
import org.jboss.ejb3.timerservice.integration.test.cancel.SimpleTimer;
import org.jboss.ejb3.timerservice.integration.test.cancel.SimpleTimerSLSB;
import org.jboss.ejb3.timerservice.integration.test.common.AbstractTimerServiceTestCase;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Date;

/**
 * Tests that timers which have been cancelled will no longer fire timeouts
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 * @see https://jira.jboss.org/browse/JBAS-8232
 */
public class CancelTimerTestCase extends AbstractTimerServiceTestCase {
    private static Logger logger = Logger.getLogger(CancelTimerTestCase.class);

    private URL deployment;

    /**
     * @return
     * @throws Exception
     */
    @Before
    public void before() throws Exception {
        String jarName = "cancel-timer-test.jar";
        File jar = buildSimpleJar(jarName, SimpleTimerSLSB.class.getPackage());
        this.deployment = jar.toURI().toURL();
        this.redeploy(deployment);
    }

    @After
    public void after() throws Exception {
        if (this.deployment != null) {
            this.undeploy(deployment);
        }
    }

    /**
     * Tests that a non-calendar expression based timer, when cancelled, will no longer
     * fire timeouts
     *
     * @throws Exception
     * @see JBAS-8232 https://jira.jboss.org/browse/JBAS-8232
     */
    @Test
    public void testCancelOfSimpleTimer() throws Exception {
        SimpleTimer bean = (SimpleTimer) this.getInitialContext().lookup(SimpleTimerSLSB.JNDI_NAME);

        long twoSeconds = 2000;
        long everySecond = 1000;

        bean.createTimer(twoSeconds, everySecond);
        logger.debug("Created timer to fire every second starting at " + new Date(System.currentTimeMillis() + twoSeconds));

        // check that the timers were created
        Assert.assertTrue("No timers were created for bean " + SimpleTimerSLSB.class.getSimpleName(), bean
                .timersCreated());

        final long THREE_SECONDS = 3000;

        // now wait for atleast 1 timeout to happen
        logger.info("Sleeping for 3 seconds for timeout to happen");
        Thread.sleep(THREE_SECONDS);

        // check that atleast one timeout occured
        int timeoutCount = bean.getTimeoutCount();
        Assert.assertTrue("Not even 1 timeout occured", timeoutCount > 0);

        // now cancel the timer
        bean.stopTimers();

        int timeoutCountImmidiatelyAfterCancel = bean.getTimeoutCount();

        // check that there are no more active timers for the bean
        Assert.assertFalse("Active timers found, even after cancelling the timers, on bean "
                + SimpleTimerSLSB.class.getSimpleName(), bean.timersCreated());

        // wait for a few more seconds and check whether the timer was indeed cancelled
        // or whether it is still firing timeouts
        logger
                .info("Sleeping for 3 more seconds after cancelling the timer, to make sure the timers were really cancelled");
        Thread.sleep(THREE_SECONDS);

        int finalTimeoutCount = bean.getTimeoutCount();
        // make sure that the timeout count immidiately after cancellation of timers is the same as the latest timeout count
        Assert.assertEquals("Timers wasn't really cancelled. Timeouts are still happening",
                timeoutCountImmidiatelyAfterCancel, finalTimeoutCount);
    }
}
