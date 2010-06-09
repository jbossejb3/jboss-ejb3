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
package org.jboss.ejb3.timerservice.integration.test.timerinfo.unit;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.Date;

import junit.framework.Assert;

import org.jboss.ejb3.timerservice.integration.test.common.AbstractTimerServiceTestCase;
import org.jboss.ejb3.timerservice.integration.test.timerinfo.CustomTimerInfo;
import org.jboss.ejb3.timerservice.integration.test.timerinfo.SerializableInfoTimerBean;
import org.jboss.ejb3.timerservice.integration.test.timerinfo.SimpleTimer;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * SerializableInfoTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class SerializableInfoTestCase extends AbstractTimerServiceTestCase
{
   private static Logger logger = Logger.getLogger(SerializableInfoTestCase.class);

   private URL deployment;

   /**
    * 
    * @return
    * @throws Exception
    */
   @Before
   public void before() throws Exception
   {
      String jarName = "serializable-timer-info-test.jar";
      File jar = buildSimpleJar(jarName, SerializableInfoTimerBean.class.getPackage());
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
   public void testSerializableInfoPersistence() throws Exception
   {
      SimpleTimer timerBean = (SimpleTimer) this.getInitialContext().lookup(SerializableInfoTimerBean.JNDI_NAME);
      
      String msg = "Hello world from timer testcase";
      CustomTimerInfo info = new CustomTimerInfo(msg);
      Date tenSecondsFromNow = new Date(System.currentTimeMillis() + 10000);
      timerBean.createTimer(tenSecondsFromNow, info);
      
      this.redeploy(this.deployment);
      // wait few seconds
      Thread.sleep(11000);
      
      timerBean = (SimpleTimer) this.getInitialContext().lookup(SerializableInfoTimerBean.JNDI_NAME);
      Serializable infoFromTimer = timerBean.getInfoFromTimer();
      
      Assert.assertEquals("Unexpected info in Timer", info, infoFromTimer);
      
   }
}
