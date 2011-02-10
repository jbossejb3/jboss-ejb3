/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import org.jboss.ejb3.tx2.spi.TransactionalInvocationContext;
import org.jboss.logging.Logger;
import org.jboss.tm.TransactionTimeoutConfiguration;
import org.jboss.util.deadlock.ApplicationDeadlockException;

import javax.annotation.Resource;
import javax.ejb.ApplicationException;
import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRequiredException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.rmi.RemoteException;
import java.util.Random;

/**
 * Ensure the correct exceptions are thrown based on both caller
 * transactional context and supported Transaction Attribute Type
 * <p/>
 * EJB3 13.6.2.6
 * EJB3 Core Specification 14.3.1 Table 14
 *
 * @author <a href="mailto:andrew.rubinger@redhat.com">ALR</a>
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class CMTTxInterceptor
{
   private static final Logger log = Logger.getLogger(CMTTxInterceptor.class);

   private static final int MAX_RETRIES = 5;
   private static final Random RANDOM = new Random();

   protected TransactionManager tm;

   /**
    * Individual implementations of this {@link CMTTxInterceptor} are expected to handle the
    * transactional invocation appropriately, based on the transaction attribute type.
    *
    * @param invocationContext The transactional invocation context
    * @return
    * @throws Exception
    */
   public abstract Object invoke(TransactionalInvocationContext invocationContext) throws Exception;


   /**
    * The <code>endTransaction</code> method ends a transaction and
    * translates any exceptions into
    * TransactionRolledBack[Local]Exception or SystemException.
    *
    * @param tm a <code>TransactionManager</code> value
    * @param tx a <code>Transaction</code> value
    */
   protected void endTransaction(TransactionManager tm, Transaction tx)
   {
      try
      {
         if (tx != tm.getTransaction())
         {
            throw new IllegalStateException("Wrong tx on thread: expected " + tx + ", actual " + tm.getTransaction());
         }

         if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK)
         {
            tm.rollback();
         }
         else
         {
            // Commit tx
            // This will happen if
            // a) everything goes well
            // b) app. exception was thrown
            tm.commit();
         }
      }
      catch (RollbackException e)
      {
         handleEndTransactionException(e);
      }
      catch (HeuristicMixedException e)
      {
         handleEndTransactionException(e);
      }
      catch (HeuristicRollbackException e)
      {
         handleEndTransactionException(e);
      }
      catch (SystemException e)
      {
         handleEndTransactionException(e);
      }
   }

   protected int getCurrentTransactionTimeout() throws SystemException
   {
      if (tm instanceof TransactionTimeoutConfiguration)
      {
         return ((TransactionTimeoutConfiguration) tm).getTransactionTimeout();
      }
      return 0;
   }

   protected void handleEndTransactionException(Exception e)
   {
      if (e instanceof RollbackException)
      {
         throw new EJBTransactionRolledbackException("Transaction rolled back", e);
      }
      throw new EJBException(e);
   }

   protected void handleInCallerTx(TransactionalInvocationContext invocation, Throwable t, Transaction tx) throws Exception
   {
      ApplicationException ae = invocation.getApplicationException(t.getClass());

      if (ae != null)
      {
         if (ae.rollback())
         {
            setRollbackOnly(tx);
         }
         // an app exception can never be an Error
         throw (Exception) t;
      }

      // if it's not EJBTransactionRolledbackException
      if (!(t instanceof EJBTransactionRolledbackException))
      {
         if (t instanceof Error)
         {
            //t = new EJBTransactionRolledbackException(formatException("Unexpected Error", t));
            Throwable cause = t;
            t = new EJBTransactionRolledbackException("Unexpected Error");
            t.initCause(cause);
         }
         // If this is an EJBException, pass through to the caller
         else if (t instanceof EJBException || t instanceof RemoteException)
         {
            // Leave Exception as-is (this is in place to handle specifically, and not
            // as a generic RuntimeException
         }
         else if (t instanceof RuntimeException)
         {
            t = new EJBTransactionRolledbackException(t.getMessage(), (Exception) t);
         }
         else // application exception
         {
            throw (Exception) t;
         }
      }

      setRollbackOnly(tx);
      log.error(t);
      throw (Exception) t;
   }

   public void handleExceptionInOurTx(TransactionalInvocationContext invocation, Throwable t, Transaction tx) throws Exception
   {
      ApplicationException ae = invocation.getApplicationException(t.getClass());
      if (ae != null)
      {
         if (ae.rollback())
         {
            setRollbackOnly(tx);
         }
         throw (Exception) t;
      }

      // if it's neither EJBException nor RemoteException
      if (!(t instanceof EJBException || t instanceof RemoteException))
      {
         // errors and unchecked are wrapped into EJBException
         if (t instanceof Error)
         {
            //t = new EJBException(formatException("Unexpected Error", t));
            Throwable cause = t;
            t = new EJBException("Unexpected Error");
            t.initCause(cause);
         }
         else if (t instanceof RuntimeException)
         {
            t = new EJBException((Exception) t);
         }
         else
         {
            // an application exception
            throw (Exception) t;
         }
      }

      setRollbackOnly(tx);
      throw (Exception) t;
   }


   protected Object invokeInCallerTx(TransactionalInvocationContext invocation, Transaction tx) throws Exception
   {
      try
      {
         return invocation.proceed();
      }
      catch (Throwable t)
      {
         handleInCallerTx(invocation, t, tx);
      }
      throw new RuntimeException("UNREACHABLE");
   }

   protected Object invokeInNoTx(TransactionalInvocationContext invocation) throws Exception
   {
      return invocation.proceed();
   }

   protected Object invokeInOurTx(TransactionalInvocationContext invocation, TransactionManager tm) throws Exception
   {
      for (int i = 0; i < MAX_RETRIES; i++)
      {
         tm.begin();
         Transaction tx = tm.getTransaction();
         try
         {
            try
            {
               return invocation.proceed();
            }
            catch (Throwable t)
            {
               handleExceptionInOurTx(invocation, t, tx);
            }
            finally
            {
               endTransaction(tm, tx);
            }
         }
         catch (Exception ex)
         {
            ApplicationDeadlockException deadlock = ApplicationDeadlockException.isADE(ex);
            if (deadlock != null)
            {
               if (!deadlock.retryable() || i + 1 >= MAX_RETRIES)
               {
                  throw deadlock;
               }
               log.warn(deadlock.getMessage() + " retrying " + (i + 1));

               Thread.sleep(RANDOM.nextInt(1 + i), RANDOM.nextInt(1000));
            }
            else
            {
               throw ex;
            }
         }
      }
      throw new RuntimeException("UNREACHABLE");
   }

   /**
    * The <code>setRollbackOnly</code> method calls setRollbackOnly()
    * on the invocation's transaction and logs any exceptions than may
    * occur.
    *
    * @param tx the transaction
    */
   protected void setRollbackOnly(Transaction tx)
   {
      try
      {
         tx.setRollbackOnly();
      }
      catch (SystemException ex)
      {
         log.error("SystemException while setting transaction for rollback only", ex);
      }
      catch (IllegalStateException ex)
      {
         log.error("IllegalStateException while setting transaction for rollback only", ex);
      }
   }

   @Resource
   public void setTransactionManager(TransactionManager tm)
   {
      this.tm = tm;
   }

}
