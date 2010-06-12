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
package org.jboss.ejb3.timerservice.integration.test.jndi.unit;

import java.io.File;
import java.net.URL;

import org.jboss.ejb3.timerservice.integration.test.common.AbstractTimerServiceTestCase;
import org.jboss.ejb3.timerservice.integration.test.jndi.SimpleSingleton;
import org.jboss.ejb3.timerservice.integration.test.jndi.TimerServiceJNDIAccessTester;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * TimerServiceJNDITestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class TimerServiceJNDITestCase extends AbstractTimerServiceTestCase
{
   private static Logger logger = Logger.getLogger(TimerServiceJNDITestCase.class);

   private URL deployment;

   /**
    * 
    * @return
    * @throws Exception
    */
   @Before
   public void before() throws Exception
   {
      String jarName = "timerservice-jndi-test.jar";
      File jar = buildSimpleJar(jarName, TimerServiceJNDIAccessTester.class.getPackage());
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
   public void testTimerServiceAvailibilityInPostConstruct() throws Exception
   {
      TimerServiceJNDIAccessTester bean = (TimerServiceJNDIAccessTester) this.getInitialContext().lookup(
            SimpleSingleton.JNDI_NAME);
      Assert.assertTrue("Timer service was not available in postconstruct of " + SimpleSingleton.class.getName()
            + " bean ", bean.wasTimerServiceAvailableInPostConstruct());
   }

   @Test
   public void testTimerServiceInjection() throws Exception
   {
      TimerServiceJNDIAccessTester bean = (TimerServiceJNDIAccessTester) this.getInitialContext().lookup(
            SimpleSingleton.JNDI_NAME);
      Assert.assertTrue("Timer service was not injected in " + SimpleSingleton.class.getName() + " bean ", bean
            .isTimerServiceInjected());

   }

   @Test
   public void testTimerServiceAvailibilityThroughEJBContext() throws Exception
   {
      TimerServiceJNDIAccessTester bean = (TimerServiceJNDIAccessTester) this.getInitialContext().lookup(
            SimpleSingleton.JNDI_NAME);
      Assert.assertTrue("Timer service was not available through EJBContext of " + SimpleSingleton.class.getName()
            + " bean ", bean.isTimerServiceAvailableThroughEJBContext());
   }

   @Test
   public void testTimerServiceAvailibilityAtCustomENCName() throws Exception
   {
      TimerServiceJNDIAccessTester bean = (TimerServiceJNDIAccessTester) this.getInitialContext().lookup(
            SimpleSingleton.JNDI_NAME);
      Assert.assertTrue("Timer service was not bound to custom ENC name for bean " + SimpleSingleton.class.getName()
            + " bean ", bean.isTimerServiceAvailableInENCAtCustomName());
   }

}
