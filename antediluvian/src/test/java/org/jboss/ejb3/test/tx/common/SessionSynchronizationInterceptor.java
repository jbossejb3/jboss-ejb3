/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.test.tx.common;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.SessionSynchronization;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.ejb3.interceptors.container.ContainerMethodInvocation;
import org.jboss.ejb3.tx.TxUtil;
import org.jboss.logging.Logger;

/**
 * Registers a bean which implements SessionSynchronization with the transaction.
 * 
 * EJB 3 4.3.7: The Optional SessionSynchronization Interface for Stateful Session Beans
 * 
 * FIXME: Since this class depends on internals of stateful container it can't be separated
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 71139 $
 */
public class SessionSynchronizationInterceptor implements Interceptor
{
   private static final Logger log = Logger.getLogger(SessionSynchronizationInterceptor.class);
   
   private TransactionManager tm;

   public SessionSynchronizationInterceptor()
   {
      this.tm = TxUtil.getTransactionManager();
   }

   public String getName()
   {
      return null;
   }

   protected static class SFSBSessionSynchronization<T> implements Synchronization
   {
      private StatefulBeanContext<T> ctx;

      public SFSBSessionSynchronization(StatefulBeanContext<T> ctx)
      {
         this.ctx = ctx;
      }

      public void beforeCompletion()
      {
         SessionSynchronization bean = (SessionSynchronization) ctx.getInstance();
         try
         {
            bean.beforeCompletion();
         }
         catch (RemoteException e)
         {
            throw new RuntimeException(e);
         }
      }

      public void afterCompletion(int status)
      {
         ctx.setTxSynchronized(false);
         SessionSynchronization bean = (SessionSynchronization) ctx.getInstance();
         try
         {
            if (status == Status.STATUS_COMMITTED)
            {
               bean.afterCompletion(true);
            }
            else
            {
               bean.afterCompletion(false);
            }
         }
         catch (RemoteException ignore)
         {
         }
         finally
         {
            StatefulContainer<T> container = ctx.getContainer();
            container.getCache().release(ctx);
         }
      }
   }

   protected <T> void registerSessionSynchronization(StatefulBeanContext<T> ctx) throws RemoteException, SystemException
   {
      if (ctx.isTxSynchronized()) return;
      Transaction tx = tm.getTransaction();
      if (tx == null) return;
      // tx.registerSynchronization will throw RollbackException, so no go
      if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK) return;
      SFSBSessionSynchronization<T> synch = new SFSBSessionSynchronization<T>(ctx);
      try
      {
         tx.registerSynchronization(synch);
      }
      catch(RollbackException e)
      {
         log.warn("Unexpected RollbackException from tx " + tx + " with status " + tx.getStatus());
         throw new EJBException(e);
      }
      // Notify StatefulInstanceInterceptor that the synch will take care of the release.
      ctx.setTxSynchronized(true);
      SessionSynchronization bean = (SessionSynchronization) ctx.getInstance();
      // EJB 3 4.3.7 paragraph 2
      bean.afterBegin();
   }

   public Object invoke(Invocation invocation) throws Throwable
   {
      StatefulBeanContext<?> target = (StatefulBeanContext<?>) ContainerMethodInvocation.getContainerMethodInvocation(invocation).getBeanContext();
      if (target.getInstance() instanceof SessionSynchronization)
      {
         registerSessionSynchronization(target);
      }
      return invocation.invokeNext();
   }
}
