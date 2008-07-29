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
package org.jboss.ejb3.tx.test.sessionsynchronization.unit;

import static org.junit.Assert.fail;

import java.net.URL;

import javax.ejb.EJBTransactionRolledbackException;

import org.jboss.ejb3.test.tx.common.StatefulContainer;
import org.jboss.ejb3.test.tx.mc.UnitTestBootstrap;
import org.jboss.ejb3.tx.test.sessionsynchronization.SessionSyncTest;
import org.jboss.ejb3.tx.test.sessionsynchronization.SessionSyncTestBean;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class SessionSyncTestCase
{
   private static UnitTestBootstrap bootstrap;
   private static StatefulContainer<SessionSyncTestBean> container;
   
   private static URL getResource(String name)
   {
      return Thread.currentThread().getContextClassLoader().getResource(name);
   }
   
   @BeforeClass
   public static void setUpBeforeClass() throws Throwable
   {
      bootstrap = new UnitTestBootstrap();
      bootstrap.deploy(getResource("instance/beans.xml"));
      
      container = new StatefulContainer<SessionSyncTestBean>("SessionSyncTest", "Stateful Container", SessionSyncTestBean.class);
   }

   @AfterClass
   public static void tearDownAfterClass() throws Exception
   {
      if(bootstrap != null)
         bootstrap.shutdown();
   }

   @Test
   public void test1() throws Throwable
   {
      SessionSyncTest bean = container.constructProxy(SessionSyncTest.class);
      
      try
      {
         bean.sayHi("me");
         fail("Should have thrown an EJBTransactionRolledbackException");
      }
      catch(EJBTransactionRolledbackException e)
      {
         // whoopie
      }
   }
}
