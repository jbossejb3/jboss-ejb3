/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.ejb3.tx2.impl;

import org.jboss.ejb3.tx2.spi.TransactionalComponent;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.ejb.ApplicationException;
import javax.ejb.EJBException;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.InvocationContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulBMTInterceptorTestCase
{
   private static final Logger logger = Logger.getLogger(StatefulBMTInterceptorTestCase.class);

   private TransactionManager transactionManager;

   @Before
   public void beforeTest()
   {
      this.transactionManager = new MockTransactionManager();
   }

   @Test
   public void test1() throws Exception
   {
      final String componentName = "Test";

      StatefulBMTInterceptor interceptor = new StatefulBMTInterceptor()
      {
         @Override
         protected TransactionalComponent getTransactionalComponent()
         {
            return new DummyTransactionalComponent(componentName);
         }

         @Override
         protected TransactionManager getTransactionManager()
         {
            return transactionManager;
         }
      };

      InvocationContext invocation = mock(InvocationContext.class);
      when(invocation.proceed()).thenAnswer(new Answer<Object>()
      {
         @Override
         public Object answer(InvocationOnMock invocation) throws Throwable
         {
            transactionManager.begin();
            return transactionManager.getTransaction();
         }
      });
      Transaction expected = (Transaction) interceptor.invoke(invocation);

      assertNotNull(expected);
      assertSame(expected, interceptor.getTransaction());
   }

   @Test
   public void testApplicationException() throws Exception
   {
      // create a application exceptions map
      final Map<Class<?>, ApplicationException> appExceptions = new HashMap();
      ApplicationException ae = new ApplicationException()
      {
         @Override
         public boolean rollback()
         {
            return false;
         }

         @Override
         public Class<? extends Annotation> annotationType()
         {
            return ApplicationException.class;
         }
      };
      appExceptions.put(SimpleApplicationException.class, ae);

      StatefulBMTInterceptor statefulBMTInterceptor = new StatefulBMTInterceptor()
      {
         @Override
         protected TransactionalComponent getTransactionalComponent()
         {
            // create the transactional component and pass it the application exceptions map
            return new DummyTransactionalComponent("appexception-test", appExceptions);
         }

         @Override
         protected TransactionManager getTransactionManager()
         {
            return transactionManager;
         }
      };
      InvocationContext invocation = mock(InvocationContext.class);
      when(invocation.proceed()).thenAnswer(new Answer<Object>()
      {
         @Override
         public Object answer(InvocationOnMock invocation) throws Throwable
         {
            // throw the application exception
            throw new SimpleApplicationException();
         }
      });

      try
      {
         // invoke on the interceptor
         statefulBMTInterceptor.invoke(invocation);
      }
      catch (SimpleApplicationException sae)
      {
         // expected
         logger.debug("Got the expected " + SimpleApplicationException.class.getName() + " exception: ", sae);
      }

   }

   @Test
   public void testSystemException() throws Exception
   {
      StatefulBMTInterceptor statefulBMTInterceptor = new StatefulBMTInterceptor()
      {
         @Override
         protected TransactionalComponent getTransactionalComponent()
         {
            return new DummyTransactionalComponent("appexception-test");
         }

         @Override
         protected TransactionManager getTransactionManager()
         {
            return transactionManager;
         }
      };
      InvocationContext invocation = mock(InvocationContext.class);
      when(invocation.proceed()).thenAnswer(new Answer<Object>()
      {
         @Override
         public Object answer(InvocationOnMock invocation) throws Throwable
         {
            // throw the system exception
            throw new SimpleSystemException();
         }
      });

      try
      {
         // invoke on the interceptor
         statefulBMTInterceptor.invoke(invocation);
      }
      catch (EJBException ejbe)
      {
         // expected
         logger.debug("Got the expected " + EJBException.class.getName() + " exception: ", ejbe);
         Assert.assertNotNull("No cause found in EJBException", ejbe.getCause());
         Assert.assertEquals("Unexpected cause in EJBException", SimpleSystemException.class, ejbe.getCause().getClass());
      }

   }

   private class DummyTransactionalComponent implements TransactionalComponent
   {

      private String componentName;

      private Map<Class<?>, ApplicationException> applicationExceptions;

      DummyTransactionalComponent(String componentName)
      {
         this.componentName = componentName;
      }

      DummyTransactionalComponent(String componentName, Map<Class<?>, ApplicationException> appExceptions)
      {
         this.componentName = componentName;
         this.applicationExceptions = appExceptions;
      }

      @Override
      public String getComponentName()
      {
         return componentName;
      }

      @Override
      public TransactionAttributeType getTransactionAttributeType(Method method)
      {
         return TransactionAttributeType.REQUIRED;
      }

      @Override
      public ApplicationException getApplicationException(Class<?> exceptionClass)
      {
         if (this.applicationExceptions == null)
         {
            return null;
         }
         return this.applicationExceptions.get(exceptionClass);
      }

      @Override
      public int getTransactionTimeout(Method method)
      {
         return 0;
      }

      @Override
      public TransactionManager getTransactionManager()
      {
         return StatefulBMTInterceptorTestCase.this.transactionManager;
      }
   }

   private class SimpleApplicationException extends Exception
   {

   }

   private class SimpleSystemException extends Exception
   {

   }
}
