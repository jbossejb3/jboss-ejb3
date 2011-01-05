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
package org.jboss.ejb3.test.tx.common;

import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import org.jboss.aop.joinpoint.Invocation;
import org.jboss.ejb3.tx.AbstractInterceptor;
import org.jboss.tm.TransactionLocal;

/**
 * Allows a custom transaction synchronization.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class SynchronizationActionInterceptor extends AbstractInterceptor
{
   private static TransactionLocal local = new TransactionLocal();
   private static Synchronization sync;
   
   private static Transaction getCurrentTransaction()
   {
      return local.getTransaction();
   }
   
   public Object invoke(Invocation invocation) throws Throwable
   {
      if(local.get() == null && sync != null)
      {
         Transaction tx = getCurrentTransaction();
         tx.registerSynchronization(sync);
         local.set(sync);
      }
      return invocation.invokeNext();
   }
   
   public static void setSynchronization(Synchronization s)
   {
      sync = s;
   }
}
