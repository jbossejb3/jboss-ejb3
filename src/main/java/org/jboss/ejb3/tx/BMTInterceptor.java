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
package org.jboss.ejb3.tx;

import javax.ejb.EJBException;
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
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 *  @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision$
 */
public class BMTInterceptor extends AbstractInterceptor
{
   private TransactionManager tm;
   private boolean isStateless;
   protected static Logger log = Logger.getLogger(BMTInterceptor.class);

   public BMTInterceptor(TransactionManager tm, boolean stateless)
   {
      this.tm = tm;
      isStateless = stateless;
   }

   public Object handleStateless(Invocation invocation) throws Throwable
   {
      String ejbName = invocation.getAdvisor().getName();
      boolean exceptionThrown = false;
      try
      {
         return invocation.invokeNext();
      }
      catch (Exception ex)
      {
         exceptionThrown = true;
         checkStatelessDone(ejbName, ex);
         throw ex;
      }
      finally
      {
         try
         {
            if (!exceptionThrown) checkStatelessDone(ejbName, null);
         }
         finally
         {
            tm.suspend();
         }
      }
   }

   public Object handleStateful(Invocation invocation) throws Throwable
   {
      StatefulBeanContext<?> ctx = (StatefulBeanContext<?>) ContainerMethodInvocation.getContainerMethodInvocation(invocation).getBeanContext();
      String ejbName = invocation.getAdvisor().getName();

      Transaction tx = (Transaction)ctx.getMetaData().getMetaData("TX", "TX");
      if (tx != null)
      {
         ctx.getMetaData().addMetaData("TX", "TX", null, PayloadKey.TRANSIENT);
         tm.resume(tx);
      }
      try
      {
         return invocation.invokeNext();
      }
      finally
      {
         checkBadStateful(ejbName);
         Transaction newTx = tm.getTransaction();
         if (newTx != null)
         {
            ctx.getMetaData().addMetaData("TX", "TX", newTx, PayloadKey.TRANSIENT);
            tm.suspend();
         }
         else
         {
            ctx.getMetaData().addMetaData("TX", "TX", null, PayloadKey.TRANSIENT);
         }
      }
   }

   public Object invoke(Invocation invocation) throws Throwable
   {
      Transaction oldTx = tm.getTransaction();
      if (oldTx != null) tm.suspend();

      try
      {
         if (isStateless) return handleStateless(invocation);
         else return handleStateful(invocation);
      }
      finally
      {
         if (oldTx != null) tm.resume(oldTx);
      }


   }

   private void checkStatelessDone(String ejbName, Exception ex)
   {
      int status = Status.STATUS_NO_TRANSACTION;

      try
      {
         status = tm.getStatus();
      }
      catch (SystemException sex)
      {
         log.error("Failed to get status", sex);
      }

      switch (status)
      {
         case Status.STATUS_ACTIVE :
         case Status.STATUS_COMMITTING :
         case Status.STATUS_MARKED_ROLLBACK :
         case Status.STATUS_PREPARING :
         case Status.STATUS_ROLLING_BACK :
            try
            {
               tm.rollback();
            }
            catch (Exception sex)
            {
               log.error("Failed to rollback", sex);
            }
         // fall through...
         case Status.STATUS_PREPARED :
            String msg = "Application error: BMT stateless bean " + ejbName
                         + " should complete transactions before" + " returning (ejb1.1 spec, 11.6.1)";
            log.error(msg);

            // the instance interceptor will discard the instance
            if (ex != null)
            {
               if (ex instanceof EJBException)
                  throw (EJBException)ex;
               else
                  throw new EJBException(msg, ex);
            }
            else throw new EJBException(msg);
      }
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


}
