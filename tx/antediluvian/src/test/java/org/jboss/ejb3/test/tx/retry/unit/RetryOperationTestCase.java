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
package org.jboss.ejb3.test.tx.retry.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.ejb.EJBTransactionRolledbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.jboss.ejb3.interceptors.container.BeanContext;
import org.jboss.ejb3.interceptors.direct.DirectContainer;
import org.jboss.ejb3.test.tx.common.AbstractTxTestCase;
import org.jboss.ejb3.test.tx.common.SynchronizationActionInterceptor;
import org.jboss.ejb3.test.tx.retry.NoopStatelessBean;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class RetryOperationTestCase extends AbstractTxTestCase
{
   private static TransactionManager tm ;
   
   @BeforeClass
   public static void beforeClass() throws Throwable
   {
      AbstractTxTestCase.beforeClass();
      
      tm = bootstrap.lookup("RealTransactionManager", TransactionManager.class);
   }
   
   @Test
   public void testBeforeCompletionFailure() throws Throwable
   {
      SynchronizationActionInterceptor.setSynchronization(new Synchronization() {
         public void afterCompletion(int status)
         {
         }

         public void beforeCompletion()
         {
            throw new RuntimeException("fail on purpose");
         }
      });
      DirectContainer<NoopStatelessBean> container = new DirectContainer<NoopStatelessBean>("NoopStatelessBean", "Retrying Stateless Container", NoopStatelessBean.class);
      
      BeanContext<NoopStatelessBean> bean = container.construct();
      
      try
      {
         container.invoke(bean, "noop");
         fail("Should have caught an EJBTransactionRolledbackException");
      }
      catch(EJBTransactionRolledbackException e)
      {
         // in the end it'll still throw this one
      }
      
      assertEquals("Should have retried 4 times", 4, bean.getInstance().operations);
   }

   @Test
   public void testBeforeCompletionRollback() throws Throwable
   {
      SynchronizationActionInterceptor.setSynchronization(new Synchronization() {
         public void afterCompletion(int status)
         {
         }

         public void beforeCompletion()
         {
            try
            {
               tm.setRollbackOnly();
            }
            catch(SystemException e)
            {
               throw new RuntimeException(e);
            }
         }
      });
      DirectContainer<NoopStatelessBean> container = new DirectContainer<NoopStatelessBean>("NoopStatelessBean", "Retrying Stateless Container", NoopStatelessBean.class);
      
      BeanContext<NoopStatelessBean> bean = container.construct();
      
      try
      {
         container.invoke(bean, "noop");
         fail("Should have caught an EJBTransactionRolledbackException");
      }
      catch(EJBTransactionRolledbackException e)
      {
         // in the end it'll still throw this one
      }
      
      assertEquals("Should have retried 4 times", 4, bean.getInstance().operations);
   }
}
