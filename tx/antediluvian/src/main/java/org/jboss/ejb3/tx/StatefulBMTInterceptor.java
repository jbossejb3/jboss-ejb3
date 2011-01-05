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
package org.jboss.ejb3.tx;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.util.PayloadKey;
import org.jboss.ejb3.interceptors.container.ContainerMethodInvocation;
import org.jboss.ejb3.tx.container.StatefulBeanContext;
import org.jboss.logging.Logger;
import org.jboss.tm.TxUtils;

/**
 * EJB 3 13.6.1:
 * In the case of a stateful session bean, it is possible that the business method that started a transaction
 * completes without committing or rolling back the transaction. In such a case, the container must retain
 * the association between the transaction and the instance across multiple client calls until the instance
 * commits or rolls back the transaction. When the client invokes the next business method, the container
 * must invoke the business method (and any applicable interceptor methods for the bean) in this transac-
 * tion context.
 * 
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class StatefulBMTInterceptor extends BMTInterceptor
{
   private static final Logger log = Logger.getLogger(StatefulBMTInterceptor.class);
   
   /**
    * @param tm
    */
   public StatefulBMTInterceptor(TransactionManager tm)
   {
      super(tm);
   }

   private void checkBadStateful(String ejbName)
   {
      int status = Status.STATUS_NO_TRANSACTION;

      try
      {
         status = tm.getStatus();
      }
      catch (SystemException ex)
      {
         log.error("Failed to get status", ex);
      }

      switch (status)
      {
         case Status.STATUS_COMMITTING :
         case Status.STATUS_MARKED_ROLLBACK :
         case Status.STATUS_PREPARING :
         case Status.STATUS_ROLLING_BACK :
            try
            {
               tm.rollback();
            }
            catch (Exception ex)
            {
               log.error("Failed to rollback", ex);
            }
            String msg = "BMT stateful bean '" + ejbName
                         + "' did not complete user transaction properly status=" + TxUtils.getStatusAsString(status);
            log.error(msg);
      }
   }

   protected Object handleInvocation(Invocation invocation) throws Throwable
   {
      assert tm.getTransaction() == null : "can't handle BMT transaction, there is a transaction active";
      
      StatefulBeanContext<?> ctx = (StatefulBeanContext<?>) ContainerMethodInvocation.getContainerMethodInvocation(invocation).getBeanContext();
      String ejbName = invocation.getAdvisor().getName();
      
      // Is the instance already associated with a transaction?
      Transaction tx = (Transaction)ctx.getMetaData().getMetaData("TX", "TX");
      if (tx != null)
      {
         ctx.getMetaData().addMetaData("TX", "TX", null, PayloadKey.TRANSIENT);
         // then resume that transaction.
         tm.resume(tx);
      }
      try
      {
         return invocation.invokeNext();
      }
      finally
      {
         checkBadStateful(ejbName);
         // Is the instance finished with the transaction?
         Transaction newTx = tm.getTransaction();
         if (newTx != null)
         {
            // remember the association
            ctx.getMetaData().addMetaData("TX", "TX", newTx, PayloadKey.TRANSIENT);
            // and suspend it.
            tm.suspend();
         }
         else
         {
            // forget any previous associated transaction 
            ctx.getMetaData().addMetaData("TX", "TX", null, PayloadKey.TRANSIENT);
         }
      }
   }
}
