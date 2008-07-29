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

import java.rmi.RemoteException;

import javax.ejb.ApplicationException;
import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRequiredException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.transaction.RollbackException;
import javax.transaction.Transaction;

import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision$
 */
public class Ejb3TxPolicy extends org.jboss.aspects.tx.TxPolicy
{
   public void throwMandatory(Invocation invocation)
   {
      throw new EJBTransactionRequiredException(((MethodInvocation) invocation).getActualMethod().toString());
   }

   @Override
   public void handleEndTransactionException(Exception e)
   {
      if(e instanceof RollbackException)
         throw new EJBTransactionRolledbackException("Transaction rolled back", e);
      super.handleEndTransactionException(e);
   }
   
   public void handleExceptionInOurTx(Invocation invocation, Throwable t, Transaction tx) throws Throwable
   {
      ApplicationException ae = TxUtil.getApplicationException(t.getClass(), invocation);
      if (ae != null)
      {
         if (ae.rollback()) setRollbackOnly(tx);
         throw t;
      }

      // if it's neither EJBException nor RemoteException
      if(!(t instanceof EJBException || t instanceof RemoteException))
      {
         // errors and unchecked are wrapped into EJBException
         if(t instanceof Error)
         {
            t = new EJBException(formatException("Unexpected Error", t));
         }
         else if (t instanceof RuntimeException)
         {
            t = new EJBException((Exception)t);
         }
         else
         {
            // an application exception
            throw t;
         }
      }

      setRollbackOnly(tx);
      throw t;
   }

   public void handleInCallerTx(Invocation invocation, Throwable t, Transaction tx) throws Throwable
   {
      ApplicationException ae = TxUtil.getApplicationException(t.getClass(), invocation);
   
      if (ae != null)
      {
         if (ae.rollback()) setRollbackOnly(tx);
         throw t;
      }
      
      // if it's not EJBTransactionRolledbackException
      if(!(t instanceof EJBTransactionRolledbackException))
      {
         if(t instanceof Error)
         {
            t = new EJBTransactionRolledbackException(formatException("Unexpected Error", t));
         }
         else if(t instanceof RuntimeException || t instanceof RemoteException)
         {
            t = new EJBTransactionRolledbackException(t.getMessage(), (Exception) t);
         }
         else // application exception
         {
            throw t;
         }
      }
      
      setRollbackOnly(tx);
      log.error(t);
      throw t;
   }

   private String formatException(String msg, Throwable t)
   {
      java.io.StringWriter sw = new java.io.StringWriter();
      java.io.PrintWriter pw = new java.io.PrintWriter(sw);
      if (msg != null)
         pw.println(msg);
      if (t != null)
      {
         t.printStackTrace(pw);
      } // end of if ()
      return sw.toString();
   }
}
