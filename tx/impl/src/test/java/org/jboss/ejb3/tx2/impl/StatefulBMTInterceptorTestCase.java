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

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.interceptor.InvocationContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulBMTInterceptorTestCase
{
   @Test
   public void test1() throws Exception
   {
      final String componentName = "Test";
      final TransactionManager transactionManager = new MockTransactionManager();
      
      StatefulBMTInterceptor interceptor = new StatefulBMTInterceptor()
      {
         @Override
         protected String getComponentName()
         {
            return componentName;
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
}
