/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.test.tx.bmt.unit;


import java.net.URL;

import org.jboss.ejb3.test.tx.bmt.StatefulBMTBean;
import org.jboss.ejb3.test.tx.common.StatefulContainer;
import org.jboss.ejb3.test.tx.mc.UnitTestBootstrap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class BMTUnitTestCase
{
   private static UnitTestBootstrap bootstrap;
   
   private static URL getResource(String name)
   {
      return Thread.currentThread().getContextClassLoader().getResource(name);
   }
   
   @BeforeClass
   public static void setUpBeforeClass() throws Throwable
   {
      bootstrap = new UnitTestBootstrap();
      bootstrap.deploy(getResource("instance/beans.xml"));
   }

   @AfterClass
   public static void tearDownAfterClass() throws Exception
   {
      if(bootstrap != null)
         bootstrap.shutdown();
   }

   @Before
   public void setUp() throws Exception
   {
   }

   @After
   public void tearDown() throws Exception
   {
   }
   
   @Test
   public void testIsGetUserTransactionAllowed() throws Throwable
   {
      StatefulContainer<StatefulBMTBean> container = new StatefulContainer<StatefulBMTBean>("StatefulBMTBean", "Stateful Container", StatefulBMTBean.class);
      
      Object id = container.construct();
      
      Boolean isAllowed = container.invoke(id, "isGetUserTransactionAllowed");
      Assert.assertFalse(isAllowed);
   }
   
   @Test
   public void testNormal() throws Throwable
   {
      StatefulContainer<StatefulBMTBean> container = new StatefulContainer<StatefulBMTBean>("StatefulBMTBean", "Stateful Container", StatefulBMTBean.class);
      
      Object id = container.construct();
      
      container.invoke(id, "normal");
   }
}
