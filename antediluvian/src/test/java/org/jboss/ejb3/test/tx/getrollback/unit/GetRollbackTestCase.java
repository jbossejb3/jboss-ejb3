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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;

import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.PrePassivate;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;

import org.jboss.aop.Advisor;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.ConstructionInvocation;
import org.jboss.ejb3.interceptors.aop.LifecycleCallbacks;
import org.jboss.ejb3.interceptors.container.BeanContext;
import org.jboss.ejb3.interceptors.direct.DirectContainer;
import org.jboss.ejb3.test.tx.common.AbstractTxTestCase;
import org.jboss.ejb3.test.tx.common.MockSessionContext;
import org.jboss.ejb3.test.tx.getrollback.GetRollbackTestBean;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * EJB 3 13.6.2.9
 * 
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class GetRollbackTestCase extends AbstractTxTestCase
{
   private static TestContainer<GetRollbackTestBean> container;
   private BeanContext<GetRollbackTestBean> instance;
   
   private static class TestContainer<T> extends DirectContainer<T>
   {
      public TestContainer(String name, String domainName, Class<? extends T> beanClass)
      {
         super(name, domainName, beanClass);
      }
      
      /**
       * Emulate a callback invocation. The real thing is still in ejb3-core.
       * 
       * @deprecated ejb3-interceptors 1.0.5 introduces a method with the same signature and similar functionality (TODO: refactor this class)
       * @param component
       * @param lifecycleAnnotationType
       * @throws Throwable
       */
      @Deprecated
      protected void invokeCallbackDeprecated(BeanContext<?> component, Class<? extends Annotation> lifecycleAnnotationType) throws Throwable
      {
         List<Class<?>> lifecycleInterceptorClasses = getInterceptorRegistry().getLifecycleInterceptorClasses();
         Advisor advisor = getAdvisor();
         Interceptor interceptors[] = LifecycleCallbacks.createLifecycleCallbackInterceptors(advisor, lifecycleInterceptorClasses, component, lifecycleAnnotationType);
         
         Constructor<?> constructor = getBeanClass().getConstructor();
         Object initargs[] = null;
         ConstructionInvocation invocation = new ConstructionInvocation(interceptors, constructor, initargs);
         invocation.setAdvisor(advisor);
         invocation.setTargetObject(component.getInstance());
         invocation.invokeNext();
      }
   }
   
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
   
   @BeforeClass
   public static void setUpBeforeClass() throws Throwable
   {
      AbstractTxTestCase.beforeClass();
      
      // TODO: should not use Stateful Container
      container = new TestContainer<GetRollbackTestBean>("GetRollbackTest", "Stateless Container", GetRollbackTestBean.class);
   }

   @Before
   public void setUp() throws Exception
   {
      instance = container.construct();
   }

   @After
   public void tearDown() throws Exception
   {
      if(instance != null)
         container.destroy(instance);
   }
   
   @Test
   public void testInjection() throws Throwable
   {
      SessionContext ctx = new MockSessionContext();
      
      try
      {
         instance.getInstance().setSessionContext(ctx);
         
         fail("EJB3 4.5.2 getRollbackOnly during injection is not allowed");
      }
      catch(IllegalStateException e)
      {
         // Good
      }
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
    * This test is to make sure there is no ClassCastException in TxUtil.getTxType.
    * It's actually not allowed by spec to do PrePassivate on a Stateless and it's
    * also not allowed to do getRollbackOnly within a lifecycle method.
    */
   @Test
   public void testPrePassivate() throws Throwable
   {
      InitialContext ctx = new InitialContext();
      TransactionManager tm = (TransactionManager) ctx.lookup("java:/TransactionManager");
      tm.begin();
      try
      {
         container.invokeCallbackDeprecated(instance, PrePassivate.class);
         
         assertTrue(instance.getInstance().prePassivateRan);
      }
      catch(IllegalStateException e)
      {
         // The original intent of the test is document above, but
         // since we're now following spec you get an IllegalStateException
      }
      finally
      {
         tm.rollback();
      }
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
