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
package org.jboss.ejb3.timerservice.integration.test.ejbthree2209.unit;

import java.io.File;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.ejb3.timerservice.integration.test.common.AbstractTimerServiceTestCase;
import org.jboss.ejb3.timerservice.integration.test.ejbthree2209.bad.BadMDB;
import org.jboss.ejb3.timerservice.integration.test.ejbthree2209.good.GoodMDB;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that a redeployment of a previously failed deployment doesn't 
 * result in "TimerService already registered" error.
 * 
 * @see https://issues.jboss.org/browse/EJBTHREE-2209
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class RedeploymentTestCase extends AbstractTimerServiceTestCase
{

   private static Logger logger = Logger.getLogger(RedeploymentTestCase.class);
   
   private static final String JAR_NAME = "ejbthree-2209.jar";
   
   /**
    * First deploys a jar containing the MDB which has an incorrect configuration and expects 
    * the deployment to fail. Then deploys another jar with the same name containing a good MDB (but with the same
    * name as the previous MDB) and expects the deployment of this new good jar to succeed.
    * 
    * @see https://issues.jboss.org/browse/EJBTHREE-2209
    * 
    * @throws Exception
    */
   @Test
   public void testRedeployment() throws Exception
   {
      File badJar = buildSimpleJar(JAR_NAME, BadMDB.class.getPackage());
      // deploy the bad jar
      try
      {
         this.redeploy(badJar.toURI().toURL());
         Assert.fail("Bad jar deployment was expected to fail");
      }
      catch (DeploymentException de)
      {
         logger.info("Got the expected deployment exception while deploying bad jar");
      }
      // now deploy the good jar
      File goodJar = buildSimpleJar(JAR_NAME, GoodMDB.class.getPackage());
      try
      {
         this.redeploy(goodJar.toURI().toURL());
         logger.info("Successfully deployed the good jar");
      }
      finally
      {
         this.undeploy(goodJar.toURI().toURL());
      }
      
   }
}
