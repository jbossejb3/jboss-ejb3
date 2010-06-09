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
package org.jboss.ejb3.timerservice.integration.test.autotimer.unit;

import java.io.File;
import java.net.URL;

import junit.framework.Assert;

import org.jboss.ejb3.timerservice.integration.test.autotimer.AutoTimerTacker;
import org.jboss.ejb3.timerservice.integration.test.autotimer.SLSBAutoTimer;
import org.jboss.ejb3.timerservice.integration.test.autotimer.SimpleSLSBAutoTimerBean;
import org.jboss.ejb3.timerservice.integration.test.autotimer.SingletonAutoTimerBean;
import org.jboss.ejb3.timerservice.integration.test.common.AbstractTimerServiceTestCase;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * AutoTimerTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class AutoTimerTestCase extends AbstractTimerServiceTestCase
{

   private static Logger logger = Logger.getLogger(AutoTimerTestCase.class);

   private URL deployment;

   /**
    * 
    * @return
    * @throws Exception
    */
   @Before
   public void before() throws Exception
   {
      String jarName = "auto-timer-test.jar";
      File jar = buildSimpleJar(jarName, SingletonAutoTimerBean.class.getPackage());
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

   @Test
   public void testAutoTimerOnSingletonBean() throws Exception
   {
      AutoTimerTacker autoTimerTracker = (AutoTimerTacker) this.getInitialContext().lookup(SingletonAutoTimerBean.JNDI_NAME);
      
      // wait for just more than 1 minute
      logger.info("Sleeping for 70 seconds to wait for timeouts on the " + SingletonAutoTimerBean.class.getName() + " bean");
      Thread.sleep(70000);
      
      int numTimeoutsForEvery10SecTimer = autoTimerTracker.getNumTimeoutsForEvery10SecTimer();
      int numTimeoutsForEveryMinuteTimer = autoTimerTracker.getNumTimeoutsForEveryMinuteTimer();
      
      Assert.assertEquals("Unexpected number of timeouts on every 10 sec auto-timer", 3, numTimeoutsForEvery10SecTimer);
      Assert.assertEquals("Unexpected number of timeouts on every minute auto-timer", 1, numTimeoutsForEveryMinuteTimer);
   }
   
   @Test
   public void testAutoTimerOnSLSB() throws Exception
   {
      SLSBAutoTimer slsbAutoTimer = (SLSBAutoTimer) this.getInitialContext().lookup(SimpleSLSBAutoTimerBean.JNDI_NAME);
      
      // wait a few seconds
      logger.info("Sleeping for 10 seconds to wait for timeouts on the " + SimpleSLSBAutoTimerBean.class.getName() + " bean");
      Thread.sleep(10000);
      
      int numTimeoutsForEvery4SecTimer = slsbAutoTimer.getNumTimeoutsForEvery4SecTimer();
      int numTimeoutsForEvery5SecTimer = slsbAutoTimer.getNumTimeoutsForEvery5SecTimer();
      
      Assert.assertEquals("Unexpected number of timeouts on every 4 sec auto-timer", 2, numTimeoutsForEvery4SecTimer);
      Assert.assertTrue("Unexpected number of timeouts on every minute auto-timer", numTimeoutsForEvery5SecTimer > 1);
   }
}
