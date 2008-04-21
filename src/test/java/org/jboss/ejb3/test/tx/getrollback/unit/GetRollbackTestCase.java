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
package org.jboss.ejb3.test.tx.getrollback.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.net.URL;

import javax.ejb.EJBTransactionRolledbackException;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;

import org.jboss.ejb3.interceptors.container.BeanContext;
import org.jboss.ejb3.interceptors.direct.DirectContainer;
import org.jboss.ejb3.test.tx.getrollback.GetRollbackTestBean;
import org.jboss.ejb3.test.tx.mc.UnitTestBootstrap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * EJB 3 13.6.2.9
 * 
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class GetRollbackTestCase
{
   private static UnitTestBootstrap bootstrap;
   
   private static DirectContainer<GetRollbackTestBean> container;
   private BeanContext<GetRollbackTestBean> instance;
   
   private void expectException(String methodName, Class<? extends Exception> exceptionClass) throws Throwable
   {
      try
      {
         container.invoke(instance, methodName);
         fail("Expecting an IllegalStateException on " + methodName);
      }
      catch(Exception e)
      {
         assertEquals(exceptionClass, e.getClass());
      }
   }
   
   private void expectFalse(String methodName) throws Throwable
   {
      Boolean actual = container.invoke(instance, methodName);
      assertFalse(actual);
   }
   
   private void expectIllegalState(String methodName) throws Throwable
   {
      try
      {
         container.invoke(instance, methodName);
         fail("Expecting an IllegalStateException on " + methodName);
      }
      catch(IllegalStateException e)
      {
         // Happy, happy, joy, joy
      }
   }
   
   private static URL getResource(String name)
   {
      return Thread.currentThread().getContextClassLoader().getResource(name);
   }
   
   @BeforeClass
   public static void setUpBeforeClass() throws Throwable
   {
      bootstrap = new UnitTestBootstrap();
      bootstrap.deploy(getResource("instance/beans.xml"));
      
      // TODO: should not use Stateful Container
      container = new DirectContainer<GetRollbackTestBean>("GetRollbackTest", "Stateless Container", GetRollbackTestBean.class);
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
      instance = container.construct();
   }

   @After
   public void tearDown() throws Exception
   {
      container.destroy(instance);
   }
   

   /**
    * Test method for {@link org.jboss.ejb3.test.tx.getrollback.GetRollbackTestBean#mandatory()}.
    * @throws Throwable 
    */
   @Test
   public void testMandatory() throws Throwable
   {
      InitialContext ctx = new InitialContext();
      TransactionManager tm = (TransactionManager) ctx.lookup("java:/TransactionManager");
      tm.begin();
      try
      {
         expectFalse("mandatory");
      }
      finally
      {
         tm.rollback();
      }
   }

   /**
    * Test method for {@link org.jboss.ejb3.test.tx.getrollback.GetRollbackTestBean#never()}.
    * @throws Throwable 
    */
   @Test
   public void testNever() throws Throwable
   {
      expectIllegalState("never");
   }

   /**
    * Test method for {@link org.jboss.ejb3.test.tx.getrollback.GetRollbackTestBean#notSupported()}.
    * @throws Throwable 
    */
   @Test
   public void testNotSupported() throws Throwable
   {
      expectIllegalState("notSupported");
   }

   /**
    * Test method for {@link org.jboss.ejb3.test.tx.getrollback.GetRollbackTestBean#required()}.
    * @throws Throwable 
    */
   @Test
   public void testRequired() throws Throwable
   {
      expectFalse("required");
   }

   /**
    * Test method for {@link org.jboss.ejb3.test.tx.getrollback.GetRollbackTestBean#requiresNew()}.
    * @throws Throwable 
    */
   @Test
   public void testRequiresNew() throws Throwable
   {
      expectFalse("requiresNew");
   }

   /**
    * Test method for {@link org.jboss.ejb3.test.tx.getrollback.GetRollbackTestBean#supports()}.
    * @throws Throwable 
    */
   @Test
   public void testSupports() throws Throwable
   {
      expectIllegalState("supports");
   }
   
   @Test
   public void testSupportsWithTransaction() throws Throwable
   {
      InitialContext ctx = new InitialContext();
      TransactionManager tm = (TransactionManager) ctx.lookup("java:/TransactionManager");
      tm.begin();
      try
      {
         expectException("supports", EJBTransactionRolledbackException.class);
      }
      finally
      {
         tm.rollback();
      }
   }
}
