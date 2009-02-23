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

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.ejb.IllegalLoopbackException;

/**
 * Make sure we throw an IllegalLoopbackException on lock upgrade from read to write.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class EJBReadWriteLock implements ReadWriteLock, Serializable
{
   private static final long serialVersionUID = 1L;
   
   private ThreadLocal<Integer> readLockCount = new ThreadLocal<Integer>();
   
   private ReentrantReadWriteLock delegate = new ReentrantReadWriteLock();
   
   private Lock readLock = new ReadLock();
   private Lock writeLock = new WriteLock();

   public class ReadLock implements Lock, Serializable
   {
      private static final long serialVersionUID = 1L;

      @Override
      public void lock()
      {
         delegate.readLock().lock();
         incReadLockCount();
      }

      @Override
      public void lockInterruptibly() throws InterruptedException
      {
         delegate.readLock().lockInterruptibly();
         incReadLockCount();
      }

      @Override
      public Condition newCondition()
      {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean tryLock()
      {
         if(delegate.readLock().tryLock())
         {
            incReadLockCount();
            return true;
         }
         return false;
      }

      @Override
      public boolean tryLock(long time, TimeUnit unit) throws InterruptedException
      {
         if(delegate.readLock().tryLock(time, unit))
         {
            incReadLockCount();
            return true;
         }
         return false;
      }

      @Override
      public void unlock()
      {
         delegate.readLock().unlock();
         decReadLockCount();
      }
      
   }
   
   public class WriteLock implements Lock, Serializable
   {
      private static final long serialVersionUID = 1L;

      @Override
      public void lock()
      {
         checkLoopback();
         delegate.writeLock().lock();
      }

      @Override
      public void lockInterruptibly() throws InterruptedException
      {
         checkLoopback();
         delegate.writeLock().lockInterruptibly();
      }

      @Override
      public Condition newCondition()
      {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean tryLock()
      {
         checkLoopback();
         return delegate.writeLock().tryLock();
      }

      @Override
      public boolean tryLock(long time, TimeUnit unit) throws InterruptedException
      {
         checkLoopback();
         return delegate.writeLock().tryLock(time, unit);
      }

      @Override
      public void unlock()
      {
         delegate.writeLock().unlock();
      }
   }
   
   private void checkLoopback()
   {
      Integer current = readLockCount.get();
      if(current != null)
      {
         assert current.intValue() > 0 : "readLockCount is set, but to 0"; 
         throw new IllegalLoopbackException("EJB 3.1 PFD2 4.8.5.1.1 upgrading from read to write lock is not allowed");
      }
   }
   
   private void decReadLockCount()
   {
      Integer current = readLockCount.get();
      int next;
      assert current != null : "can't decrease, readLockCount is not set";
      next = current.intValue() - 1;
      if(next == 0)
         readLockCount.remove();
      else
         readLockCount.set(new Integer(next));
   }
   
   private void incReadLockCount()
   {
      Integer current = readLockCount.get();
      int next;
      if(current == null)
         next = 1;
      else
         next = current.intValue() + 1;
      readLockCount.set(new Integer(next));
   }
   
   @Override
   public Lock readLock()
   {
      return readLock;
   }

   @Override
   public Lock writeLock()
   {
      return writeLock;
   }
}
