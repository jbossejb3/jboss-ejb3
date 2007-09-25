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
package org.jboss.ejb3.pool.threadlocal;

import org.jboss.ejb3.pool.Pool;
import org.jboss.ejb3.pool.StatelessObjectFactory;
import org.jboss.ejb3.pool.inifinite.InfinitePool;

/**
 * A pool which keeps an object ready per thread.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: $
 */
public class ThreadLocalPool<T> implements Pool<T>
{
   private Pool<T> delegate;
   private WeakThreadLocal<T> pool = new WeakThreadLocal<T>();
   
   public ThreadLocalPool(StatelessObjectFactory<T> factory)
   {
      delegate = new InfinitePool<T>(factory);
   }
   
   public void discard(T obj)
   {
      throw new RuntimeException("NYI");
   }

   public T get()
   {
      if(pool.get() == null)
      {
         return delegate.get();
      }
      else
      {
         T obj = pool.get();
         pool.set(null);
         return obj;
      }
   }

   public void release(T obj)
   {
      if(pool.get() == null)
      {
         pool.set(obj);
      }
      else
      {
         delegate.release(obj);
      }
   }

   public void start()
   {
      delegate.start();
   }

   public void stop()
   {
      delegate.stop();
      
      pool.remove();
   }

}
