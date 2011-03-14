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

import org.jboss.logging.Logger;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * Suspend an incoming tx.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class BMTInterceptor
{
   private static final Logger log = Logger.getLogger(BMTInterceptor.class);

   protected abstract Object handleInvocation(InvocationContext invocation) throws Exception;

   @AroundInvoke
   public Object invoke(InvocationContext invocation) throws Exception
   {
      TransactionManager tm = this.getTransactionManager();
      Transaction oldTx = tm.suspend();
      try
      {
         return handleInvocation(invocation);
      }
      finally
      {
         if (oldTx != null)
         {
            tm.resume(oldTx);
         }
      }
   }

   protected abstract TransactionManager getTransactionManager();
}
