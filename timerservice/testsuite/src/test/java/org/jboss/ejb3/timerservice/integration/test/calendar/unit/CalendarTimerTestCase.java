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
package org.jboss.ejb3.timerservice.integration.test.calendar.unit;

import junit.framework.Assert;
import org.jboss.ejb3.timerservice.integration.test.calendar.CalendarScheduler;
import org.jboss.ejb3.timerservice.integration.test.calendar.CalendarSchedulerImpl;
import org.jboss.ejb3.timerservice.integration.test.common.AbstractTimerServiceTestCase;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.ScheduleExpression;
import java.io.File;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * CalendarTimerTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class CalendarTimerTestCase extends AbstractTimerServiceTestCase {

    private static Logger logger = Logger.getLogger(CalendarTimerTestCase.class);

    private URL deployment;

    /**
     * @return
     * @throws Exception
     */
    @Before
    public void before() throws Exception {
        String jarName = "calendar-timer-test.jar";
        File jar = buildSimpleJar(jarName, CalendarSchedulerImpl.class.getPackage());
        this.deployment = jar.toURI().toURL();
        this.redeploy(deployment);
    }

    @After
    public void after() throws Exception {
        if (this.deployment != null) {
            this.undeploy(deployment);
        }
    }

    @Test
    public void testEvery10SecondsTimeout() throws Exception {
        CalendarScheduler calendarScheduler = (CalendarScheduler) this.getInitialContext().lookup(
                CalendarSchedulerImpl.JNDI_NAME);
        ScheduleExpression schedule = new ScheduleExpression();
        Date twentySecondsFromNow = new Date(System.currentTimeMillis() + 20000);
        schedule.start(twentySecondsFromNow);
        schedule.second("*/10");
        schedule.minute("*");
        schedule.hour("*");
        int threeTimes = 3;
        calendarScheduler.schedule(schedule, threeTimes);

        // wait for the timeouts to complete
        // 20 sec from now is the start, then 10 sec * 3 timeouts and a 2 second
        // grace period
        Thread.sleep(20000 + 30000 + 2000);

        Assert.assertEquals("Unexpected number of timeouts", threeTimes, calendarScheduler.getTimeoutCount());
        List<Date> timeouts = calendarScheduler.getTimeouts();
        for (Date timeout : timeouts) {
            logger.debug("Timeout was tracked at " + timeout);
            Calendar cal = new GregorianCalendar();
            cal.setTime(timeout);
            int second = cal.get(Calendar.SECOND);
            Assert.assertEquals("Timeout " + timeout + " happened at an unexpected second " + second, 0, second % 10);
        }
    }

    @Test
    public void testTimeoutOnRedeploy() throws Exception {
        CalendarScheduler calendarScheduler = (CalendarScheduler) this.getInitialContext().lookup(
                CalendarSchedulerImpl.JNDI_NAME);
        ScheduleExpression schedule = new ScheduleExpression();
        Date tenSecondsFromNow = new Date(System.currentTimeMillis() + 10000);
        schedule.start(tenSecondsFromNow);
        schedule.second("*/5");
        schedule.minute("*");
        schedule.hour("*");
        int twice = 2;
        calendarScheduler.schedule(schedule, twice);

        // redeploy
        this.redeploy(this.deployment);

        // wait for the timeouts to complete
        // 10 sec from now is the start, then 5 sec * 2 timeouts and a 2 second
        // grace period
        Thread.sleep(10000 + 10000 + 2000);

        Assert.assertEquals("Unexpected number of timeouts", twice, calendarScheduler.getTimeoutCount());
        List<Date> timeouts = calendarScheduler.getTimeouts();
        for (Date timeout : timeouts) {
            logger.debug("Timeout was tracked at " + timeout);
            Calendar cal = new GregorianCalendar();
            cal.setTime(timeout);
            int second = cal.get(Calendar.SECOND);
            Assert.assertEquals("Timeout " + timeout + " happened at an unexpected second " + second, 0, second % 5);
        }
    }
}
