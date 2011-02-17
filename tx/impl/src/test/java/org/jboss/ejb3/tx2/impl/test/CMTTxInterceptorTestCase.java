/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.ejb3.tx2.impl.test;

import junit.framework.Assert;
import org.jboss.ejb3.tx2.impl.CMTTxInterceptor;
import org.jboss.ejb3.tx2.spi.TransactionalComponent;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ejb.EJBException;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.InvocationContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests the {@link CMTTxInterceptor}
 * <p/>
 * Author : Jaikiran Pai
 */
public class CMTTxInterceptorTestCase
{

   private static final Logger logger = Logger.getLogger(CMTTxInterceptorTestCase.class);

   /**
    * EJB3.1 spec, section 14.2.1
    * <p/>
    * Test that the exception listed in the throws clause of a method is considered as
    * application exception and is thrown as-is back to the user.
    * <p/>
    *
    * @throws Exception
    */
   @Test
   public void testThrowsClauseApplicationException() throws Exception
   {
      TransactionalComponent component = this.createTransactionalComponent();
      CMTTxInterceptor cmtTxInterceptor = new MockCMTTxInterceptor(component);
      final SimpleBean target = new SimpleBean();
      final Method targetMethod = SimpleLocal.class.getMethod("methodWithExceptionInThrowsClause");
      InvocationContext invocationContext = new SimpleInvocationContext(target, targetMethod, null);
      try
      {
         cmtTxInterceptor.invoke(invocationContext);
      }
      catch (ArithmeticException ae)
      {
         // expected
         logger.debug("Got the expected application exception: " + ae.getClass().getName());

      }
   }

   /**
    * Tests that exceptions which are *not* application exceptions are wrapped into
    * {@link EJBException} before being thrown back to the user.
    *
    * @throws Exception
    */
   @Test
   public void testNonApplicationException() throws Exception
   {
      TransactionalComponent component = this.createTransactionalComponent();
      CMTTxInterceptor cmtTxInterceptor = new MockCMTTxInterceptor(component);
      final SimpleBean target = new SimpleBean();
      final Method targetMethod = SimpleLocal.class.getMethod("methodThrowingUnexpectedException");
      InvocationContext invocationContext = new SimpleInvocationContext(target, targetMethod, null);
      try
      {
         cmtTxInterceptor.invoke(invocationContext);
      }
      catch (EJBException ejbe)
      {
         // expected
         logger.debug("Got the expected EJBException: " + ejbe);

         // also check the cause of the EJBException
         Assert.assertEquals("Unexpected exception thrown", NumberFormatException.class, ejbe.getCause().getClass());
      }
   }

   /**
    * Create and return a {@link TransactionalComponent} to be used in the test
    *
    * @return
    * @throws Exception
    */
   private TransactionalComponent createTransactionalComponent() throws Exception
   {
      TransactionalComponent component = Mockito.mock(TransactionalComponent.class);
      TransactionManager mockTxManager = Mockito.mock(TransactionManager.class);
      Transaction mockTx = Mockito.mock(Transaction.class);
      Mockito.when(mockTxManager.getTransaction()).thenReturn(mockTx);
      Mockito.when(component.getTransactionManager()).thenReturn(mockTxManager);
      Mockito.when(component.getTransactionAttributeType(Mockito.any(Method.class))).thenReturn(TransactionAttributeType.REQUIRED);

      return component;
   }


   /**
    * A simple implementation of the {@link InvocationContext}
    */
   private class SimpleInvocationContext implements InvocationContext
   {
      private Object target;

      private Method targetMethod;

      private Object[] methodParams;

      SimpleInvocationContext(Object target, Method method, Object[] methodParams)
      {
         this.target = target;
         this.methodParams = methodParams;
         this.targetMethod = method;
      }

      @Override
      public Object getTarget()
      {
         return target;
      }

      @Override
      public Method getMethod()
      {
         return targetMethod;
      }

      @Override
      public Object[] getParameters()
      {
         return new Object[0];
      }

      @Override
      public void setParameters(Object[] params)
      {

      }

      @Override
      public Map<String, Object> getContextData()
      {
         return new HashMap();
      }

      @Override
      public Object proceed() throws Exception
      {
         try
         {
            return targetMethod.invoke(target, this.getParameters());
         }
         catch (InvocationTargetException ite)
         {
            // unwrap any application exception
            if (ite.getCause() instanceof Exception)
            {
               throw (Exception) ite.getCause();
            }
            throw ite;
         }
      }
   }

}
