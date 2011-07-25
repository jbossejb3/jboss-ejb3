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
package org.jboss.ejb3.timerservice.integration.test.ejbthree2220.unit;

import junit.framework.Assert;
import org.jboss.ejb3.timerservice.integration.test.common.AbstractTimerServiceTestCase;
import org.jboss.ejb3.timerservice.integration.test.ejbthree2220.TimerBean;
import org.jboss.ejb3.timerservice.integration.test.ejbthree2220.TimerTester;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.TimerService;
import java.io.File;
import java.net.URL;
import java.util.Date;

/**
 * Tests {@link TimerService#createIntervalTimer(long, long, javax.ejb.TimerConfig)}
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 * @see https://issues.jboss.org/browse/EJBTHREE-2220
 */
public class IntervalTimerTestCase extends AbstractTimerServiceTestCase {

    private static Logger logger = Logger.getLogger(IntervalTimerTestCase.class);

    private URL deployment;

    /**
     * @return
     * @throws Exception
     */
    @Before
    public void before() throws Exception {
        String jarName = "ejbthree-2220.jar";
        File jar = buildSimpleJar(jarName, TimerBean.class.getPackage());
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
     * Tests that a timer created through a call to {@link TimerService#createIntervalTimer(long, long, javax.ejb.TimerConfig)}
     * doesn't timeout before the <code>initialDuration</code>
     *
     * @throws Exception
     * @see https://issues.jboss.org/browse/EJBTHREE-2220
     */
    @Test
    public void testIntervalTimer() throws Exception {
        TimerTester timerBean = (TimerTester) this.getInitialContext().lookup(TimerBean.JNDI_NAME);
        long fiveSecondsFromNow = 5000;
        long everyHour = 60 * 60 * 1000;
        Date expectedFirstTimeout = new Date(System.currentTimeMillis() + fiveSecondsFromNow);
        timerBean.createIntervalTimer(fiveSecondsFromNow, everyHour, null);
        logger.debug("Created interval timer with initialDuration = " + fiveSecondsFromNow + " milli sec. and intervalDuration = " + everyHour + " milli sec.");

        logger.info("Sleeping for 7 seconds to wait for the timeout to happen");
        Thread.sleep(7000);

        Date firstTimeout = timerBean.getFirstTimeout();

        Assert.assertNotNull("Timeout was not invoked on TimerBean", firstTimeout);
        Assert.assertFalse("First timeout " + firstTimeout + " happened before the expected time " + expectedFirstTimeout, firstTimeout.before(expectedFirstTimeout));
    }
}
