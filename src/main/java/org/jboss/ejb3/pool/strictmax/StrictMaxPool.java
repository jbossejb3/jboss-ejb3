/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.pool.strictmax;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jboss.ejb3.pool.Pool;
import org.jboss.ejb3.pool.StatelessObjectFactory;

/**
 * A pool with a maximum size.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @version $Revision$
 */
public class StrictMaxPool<T> implements Pool<T>
{
   private StatelessObjectFactory<T> factory;
   private Semaphore semaphore;
   private int maxSize;
   private long timeout;
   private TimeUnit timeUnit;
   // Guarded by the implicit lock for "pool"
   private LinkedList<T> pool = new LinkedList<T>();
   
   public StrictMaxPool(StatelessObjectFactory<T> factory, int maxSize, long timeout, TimeUnit timeUnit)
   {
      assert factory != null : "factory is null";
      
      this.factory = factory;
      this.maxSize = maxSize;
      this.semaphore = new Semaphore(maxSize, true);
      this.timeout = timeout;
      this.timeUnit = timeUnit;
   }
   
   public void discard(T obj)
   {
      throw new RuntimeException("NYI");
   }

   public T get()
   {
      try
      {
         boolean acquired = semaphore.tryAcquire(timeout, timeUnit);
         if(!acquired)
            throw new RuntimeException("Failed to acquire a permit within " + timeout + " " + timeUnit);
      }
      catch (InterruptedException e)
      {
         throw new RuntimeException("Acquire semaphore was interupted");
      }
      
      synchronized(pool)
      {
         if(!pool.isEmpty())
         {
            return pool.removeFirst();
         }
      }
      
      return factory.create();
   }

   public void release(T obj)
   {
      boolean destroyIt = false;
      synchronized (pool)
      {
         if(pool.size() < maxSize)
            pool.add(obj);
         else
            destroyIt = true;
      }
      if(destroyIt)
         factory.destroy(obj);
      semaphore.release();
   }

   public void start()
   {
      // TODO Auto-generated method stub
      
   }

   public void stop()
   {
      synchronized (pool)
      {
         for (T obj : pool)
         {
            factory.destroy(obj);
         }
         pool.clear();
      }
   }

}
