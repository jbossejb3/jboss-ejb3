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
package org.jboss.ejb3.timerservice.integration.test.simple.unit;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.jboss.ejb3.timerservice.integration.test.common.AbstractTimerServiceTestCase;
import org.jboss.ejb3.timerservice.integration.test.simple.TimeoutTracker;
import org.jboss.ejb3.timerservice.integration.test.simple.TimerSingleton;
import org.jboss.ejb3.timerservice.integration.test.simple.TimerUtil;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SimpleTimerTestCase extends AbstractTimerServiceTestCase
{

   private static Logger logger = Logger.getLogger(SimpleTimerTestCase.class);

   private URL deployment;

   /**
    * 
    * @return
    * @throws Exception
    */
   @Before
   public void before() throws Exception
   {
      String jarName = "simple-timer-test.jar";
      File jar = buildSimpleJar(jarName, TimerSingleton.class.getPackage());
      this.deployment = jar.toURI().toURL();
      this.redeploy(deployment);
   }

   @After
   public void after() throws Exception
   {
      if (this.deployment != null)
      {
         this.undeploy(deployment);
      }
   }

   /**
    * Test that the timeout method on a singleton bean, which creates a timer, is invoked
    * at appropriate intervals. 
    * @throws Exception
    */
   @Test
   public void testTimeout() throws Exception
   {
      TimerUtil timerUtil = (TimerUtil) this.getInitialContext().lookup(TimerSingleton.JNDI_NAME);
      Date fiveSecondsFromNow = new Date(System.currentTimeMillis() + 5000);
      long everyTwoSeconds = 2000;
      int fiveTimes = 5;
      timerUtil.createTimer(fiveSecondsFromNow, 0, fiveTimes);
      
//       wait for the timers to be invoked
      Thread.sleep(5000 + 10000 + 5000);
//      TimeoutTracker timeoutTracker = timerUtil.getTimeoutTracker();
//      
//      Assert.assertNotNull("Could not get the timeout tracker" , timeoutTracker);
//      Assert.assertEquals("Unexpected number of timeouts",fiveTimes, timeoutTracker.getTimeoutCount());
//      List<Date> timeouts = timeoutTracker.getTimeouts();
//      // 1 second grace period 
//      Date lastExpectedTimeout = new Date(fiveSecondsFromNow.getTime() + (everyTwoSeconds * fiveTimes) + 1000);
//      for (Date timeout : timeouts)
//      {
//         logger.debug("Timeout was tracked at " + timeout);
//         Assert.assertFalse("Timeout " + timeout + " happened before the first timeout " + fiveSecondsFromNow, timeout.before(fiveSecondsFromNow));
//         Assert.assertTrue("Timeout " + timeout + " happened after the last expected timeout " + lastExpectedTimeout, timeout.before(lastExpectedTimeout));
//      }
   }
   
//   @Test
//   public void testTimerInvocationOnRedeploy() throws Exception
//   {
//      TimerUtil timerUtil = (TimerUtil) this.getInitialContext().lookup(TimerSingleton.JNDI_NAME);
//      Date tenSecondsFromNow = new Date(System.currentTimeMillis() + 10000);
//      long everySecond = 1000;
//      int twoTimes = 2;
//      timerUtil.createTimer(tenSecondsFromNow, everySecond, twoTimes);
//      
//      // redeploy the bean
//      this.redeploy(this.deployment);
//      
//      // wait for the timers to be invoked
//      Thread.sleep(10000 + 3000);
//      TimeoutTracker timeoutTracker = timerUtil.getTimeoutTracker();
//      
//      Assert.assertNotNull("Could not get the timeout tracker" , timeoutTracker);
//      Assert.assertEquals("Unexpected number of timeouts",twoTimes, timeoutTracker.getTimeoutCount());
//      List<Date> timeouts = timeoutTracker.getTimeouts();
//      // 1 second grace period 
//      Date lastExpectedTimeout = new Date(tenSecondsFromNow.getTime() + (everySecond * twoTimes) + 1000);
//      for (Date timeout : timeouts)
//      {
//         logger.debug("Timeout was tracked at " + timeout);
//         Assert.assertFalse("Timeout " + timeout + " happened before the first timeout " + tenSecondsFromNow, timeout.before(tenSecondsFromNow));
//         Assert.assertTrue("Timeout " + timeout + " happened after the last expected timeout " + lastExpectedTimeout, timeout.before(lastExpectedTimeout));
//      }
   //}
//   
//   public static void main(String args[]) throws Exception
//   {
//      TimerUtil timerUtil = (TimerUtil) new InitialContext().lookup(TimerSingleton.JNDI_NAME);
//      Date tenSecondsFromNow = new Date(System.currentTimeMillis() + 10000);
//      long everySecond = 1000;
//      int twoTimes = 2;
//      timerUtil.createTimer(tenSecondsFromNow, everySecond, twoTimes);
//      
////      // redeploy the bean
////      this.redeploy(this.deployment);
////      
////      // wait for the timers to be invoked
////      Thread.sleep(10000 + 3000);
////      TimeoutTracker timeoutTracker = timerUtil.getTimeoutTracker();
////      
////      Assert.assertNotNull("Could not get the timeout tracker" , timeoutTracker);
////      Assert.assertEquals("Unexpected number of timeouts",twoTimes, timeoutTracker.getTimeoutCount());
////      List<Date> timeouts = timeoutTracker.getTimeouts();
////      // 1 second grace period 
////      Date lastExpectedTimeout = new Date(tenSecondsFromNow.getTime() + (everySecond * twoTimes) + 1000);
////      for (Date timeout : timeouts)
////      {
////         logger.debug("Timeout was tracked at " + timeout);
////         Assert.assertFalse("Timeout " + timeout + " happened before the first timeout " + tenSecondsFromNow, timeout.before(tenSecondsFromNow));
////         Assert.assertTrue("Timeout " + timeout + " happened after the last expected timeout " + lastExpectedTimeout, timeout.before(lastExpectedTimeout));
////      }
//   }
}
