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
package org.jboss.ejb3.cache.impl;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.NoSuchEJBException;

import org.jboss.ejb3.cache.Cache;
import org.jboss.ejb3.cache.Identifiable;
import org.jboss.ejb3.cache.LongevityCache;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class SimpleLongevityCache<T extends Identifiable> implements LongevityCache<T>
{
   private Cache<T> delegate;
   
   private Map<Object, Entry> cache;
   
   private static enum State { FINISHED, IN_OPERATION };
   
   private class Entry
   {
      long lastUsed;
      T obj;
      State state;
      
      Entry(T obj)
      {
         assert obj != null : "obj is null";
         
         this.lastUsed = System.currentTimeMillis();
         this.obj = obj;
         this.state = State.IN_OPERATION;
      }
   }
   
   public SimpleLongevityCache(Cache<T> delegate)
   {
      assert delegate != null : "delegate is null";
      
      this.delegate = delegate;
      this.cache = new HashMap<Object, Entry>();
   }
   
   public void finished(T obj)
   {
      synchronized (cache)
      {
         Entry entry = cache.get(obj.getId());
         if(entry.state != State.IN_OPERATION)
            throw new IllegalStateException("entry " + entry + " is not in operation");
         entry.state = State.FINISHED;
         entry.lastUsed = System.currentTimeMillis();
      }
   }

   public T create(Class<?>[] initTypes, Object[] initValues)
   {
      T obj = delegate.create(initTypes, initValues);
      Entry entry = new Entry(obj);
      synchronized (cache)
      {
         cache.put(obj.getId(), entry);
      }
      return obj;
   }

   public T get(Object key) throws NoSuchEJBException
   {
      synchronized (cache)
      {
         Entry entry = cache.get(key);
         if(entry == null)
         {
            T obj = delegate.get(key);
            entry = new Entry(obj);
            cache.put(obj.getId(), entry);
            return obj;
         }
         if(entry.state != State.FINISHED)
            throw new IllegalStateException("entry " + entry + " is not finished");
         entry.state = State.IN_OPERATION;
         entry.lastUsed = System.currentTimeMillis();
         return entry.obj;
      }
   }

   public T peek(Object key) throws NoSuchEJBException
   {
      // This is the fastest
      return delegate.peek(key);
   }

   public void release(T obj)
   {
      synchronized (cache)
      {
         Object key = obj.getId();
         Entry entry = cache.get(key);
         if(entry.state != State.FINISHED)
            throw new IllegalStateException("entry " + entry + " is not finished");
         delegate.release(obj);
         cache.remove(key);
      }
   }

   public void remove(Object key)
   {
      // Note that the object is not in my cache at this point.
      delegate.remove(key);
   }

   public void start()
   {
      delegate.start();
   }

   public void stop()
   {
      delegate.stop();
   }
}
