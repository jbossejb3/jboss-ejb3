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
package org.jboss.ejb3.concurrency;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.LockType;

import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class ContainerManagedConcurrencyInterceptor implements Interceptor
{
   private ReadWriteLock readWriteLock = new EJBReadWriteLock();
   
   private AccessTimeout getAccessTimeout(Invocation invocation)
   {
      AccessTimeout timeout = (AccessTimeout) invocation.resolveAnnotation(AccessTimeout.class);
      if(timeout == null)
         timeout = (AccessTimeout) invocation.resolveClassAnnotation(AccessTimeout.class);
      return timeout;
   }
   
   private Lock getLock(Invocation invocation)
   {
      LockType lockType = getLockType(invocation);
      switch(lockType)
      {
         case READ:
            return readWriteLock.readLock();
         case WRITE:
            return readWriteLock.writeLock();
      }
      throw new IllegalStateException("Illegal lock type " + lockType + " on " + invocation);
   }
   
   private LockType getLockType(Invocation invocation)
   {
      javax.ejb.Lock lock = (javax.ejb.Lock) invocation.resolveAnnotation(javax.ejb.Lock.class);
      if(lock == null)
         lock = (javax.ejb.Lock) invocation.resolveClassAnnotation(javax.ejb.Lock.class);
      // 4.8.5.4 By default, the value of the lock associated with a
      // method of a bean with container managed concurrency demarcation is Write(exclusive), and the concur-
      // rency lock attribute does not need to be explicitly specified in this case.
      if(lock == null)
         return LockType.WRITE;
      return lock.value();
   }
   
   // TODO: this is a no-brainer and should be part of an AbstractInterceptor
   public String getName()
   {
      return getClass().getName();
   }

   @Override
   public Object invoke(Invocation invocation) throws Throwable
   {
      Lock lock = getLock(invocation);
      // TODO: these should come from somewhere else
      // Note that in violation of spec, we'll never wait indefinitely!
      long time = 5;
      TimeUnit unit = TimeUnit.MINUTES;
      AccessTimeout timeout = getAccessTimeout(invocation);
      if(timeout != null)
      {
         time = timeout.value();
         unit = timeout.unit();
      }
      boolean success = lock.tryLock(time, unit);
      if(!success)
         throw new ConcurrentAccessTimeoutException("EJB 3.1 PFD2 4.8.5.5.1 concurrent access timeout on " + invocation);
      try
      {
         return invocation.invokeNext();
      }
      finally
      {
         lock.unlock();
      }
   }
}
