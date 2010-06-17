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
package org.jboss.ejb3.timerservice.integration.test.disallowed.unit;

import java.io.File;
import java.net.URL;

import javax.ejb.TimerService;

import junit.framework.Assert;

import org.jboss.ejb3.timerservice.integration.test.common.AbstractTimerServiceTestCase;
import org.jboss.ejb3.timerservice.integration.test.disallowed.Echo;
import org.jboss.ejb3.timerservice.integration.test.disallowed.SimpleSLSB;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link TimerService} method invocations isn't allowed during lifecycle callbacks of SLSB
 * and MDB. This testcase tests that these rules are obeyed 
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class TimerServiceDisallowedMethodInvocationTestCase extends AbstractTimerServiceTestCase
{
   private static Logger logger = Logger.getLogger(TimerServiceDisallowedMethodInvocationTestCase.class);

   private URL deployment;

   /**
    * 
    * @return
    * @throws Exception
    */
   @Before
   public void before() throws Exception
   {
      String jarName = "timerservice-disallowed-methods-test.jar";
      File jar = buildSimpleJar(jarName, Echo.class.getPackage());
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
    * Tests that a SLSB ({@link SimpleSLSB}) can't invoke {@link TimerService} methods
    * in its postconstruct method
    *  
    * @throws Exception
    */
   @Test
   public void testTimerServiceDisallowedMethodInvocationForSLSB() throws Exception
   {
      Echo bean = (Echo) this.getInitialContext().lookup(SimpleSLSB.JNDI_NAME);
      // The SimpleSLSB invokes various dis-allowed timerservice methods *and catches* the
      // expected exceptions. If such exceptions weren't thrown by the timerservice, then
      // the postconstruct of the SimpleSLSB is implemented to throw an exception to prevent
      // the bean from being constructed.

      // So just testing a simple invocation on the bean is enough. The mere possibility of invoking on the bean,
      // is an indication that the bean deployment did not fail.
      String msg = "hello";
      String returnedMsg = bean.echo(msg);
      Assert.assertEquals("Unexpected echo from bean " + SimpleSLSB.class.getSimpleName(), msg, returnedMsg);
   }
}
