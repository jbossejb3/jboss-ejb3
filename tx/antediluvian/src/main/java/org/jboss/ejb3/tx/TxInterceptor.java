/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
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
package org.jboss.ejb3.tx;

import javax.ejb.ApplicationException;
import javax.ejb.EJBException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aspects.tx.TxPolicy;
import org.jboss.logging.Logger;

/**
 * Ensure the correct exceptions are thrown based on both caller
 * transactional context and supported Transaction Attribute Type
 * 
 * EJB3 13.6.2.6
 * EJB3 Core Specification 14.3.1 Table 14
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @author <a href="mailto:andrew.rubinger@redhat.com">ALR</a>
 * @version $Revision: $
 */
public class TxInterceptor extends org.jboss.aspects.tx.TxInterceptor
{
   private static final Logger log = Logger.getLogger(TxInterceptor.class);
   
   public static class Never extends org.jboss.aspects.tx.TxInterceptor.Never
   {
      public Never(TransactionManager tm, TxPolicy policy)
      {
         super(tm, policy);
      }
      
      @Override
      public Object invoke(Invocation invocation) throws Throwable
      {
         if (tm.getTransaction() != null)
         {
            throw new EJBException("Transaction present on server in Never call (EJB3 13.6.2.6)");
         }
         return policy.invokeInNoTx(invocation);
      }
   }
   
   public static class NotSupported extends org.jboss.aspects.tx.TxInterceptor.NotSupported
   {
      public NotSupported(TransactionManager tm, TxPolicy policy)
      {
         super(tm, policy);
      }
      
      public NotSupported(TransactionManager tm, TxPolicy policy, int timeout)
      {
         super(tm, policy, timeout);
      }

      /**
       * EJBTHREE-1082
       * EJB3 Core Specification 14.3.1 Table 14
       */
      @Override
      public Object invoke(Invocation invocation) throws Throwable
      {
         Transaction tx = tm.getTransaction();
         if (tx != null)
         {
            tm.suspend();
            try
            {
               return policy.invokeInNoTx(invocation);
            }
            catch (Exception e)
            {
               // If application exception was thrown, rethrow
               if (e.getClass().getAnnotation(ApplicationException.class) != null)
               {
                  throw e;
               }
               // Otherwise wrap in EJBException
               else
               {
                  throw new EJBException(e);
               }
            }
            finally
            {
               tm.resume(tx);
            }
         }
         else
         {
            return policy.invokeInNoTx(invocation);
         }
      }
   }

   public static class Mandatory extends org.jboss.aspects.tx.TxInterceptor.Mandatory
   {
      public Mandatory(TransactionManager tm, TxPolicy policy)
      {
         this(tm, policy, -1);
      }

      public Mandatory(TransactionManager tm, TxPolicy policy, int timeout)
      {
         super(tm, policy, timeout);
      }

      public String getName()
      {
         return this.getClass().getName();
      }

      public Object invoke(Invocation invocation) throws Throwable
      {
         Transaction tx = tm.getTransaction();
         if (tx == null)
         {
            policy.throwMandatory(invocation);
         }
         return policy.invokeInCallerTx(invocation, tx);
      }

   }
}
