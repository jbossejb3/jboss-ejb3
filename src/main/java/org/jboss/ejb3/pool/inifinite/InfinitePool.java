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
package org.jboss.ejb3.pool.inifinite;

import java.util.HashSet;
import java.util.Set;

import org.jboss.ejb3.pool.Pool;
import org.jboss.ejb3.pool.StatelessObjectFactory;

/**
 * A pool that has no constraints.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class InfinitePool<T> implements Pool<T>
{
   private StatelessObjectFactory<T> factory;
   private Set<T> active = new HashSet<T>();
   
   public InfinitePool(StatelessObjectFactory<T> factory)
   {
      assert factory != null : "factory is null";
      
      this.factory = factory;
   }
   
   public void discard(T obj)
   {
      throw new RuntimeException("NYI");
   }

   public T get()
   {
      T obj = factory.create();
      synchronized (active)
      {
         active.add(obj);
      }
      return obj;
   }

   public void release(T obj)
   {
      synchronized (active)
      {
         boolean contains = active.remove(obj);
         if(!contains)
            throw new IllegalArgumentException(obj + " is not of this pool");
      }
      factory.destroy(obj);
   }

   public void start()
   {
      // TODO Auto-generated method stub

   }

   public void stop()
   {
      // TODO: this is a bit wicked
      for(T obj : active)
      {
         factory.destroy(obj);
      }
      active.clear();
   }

}
