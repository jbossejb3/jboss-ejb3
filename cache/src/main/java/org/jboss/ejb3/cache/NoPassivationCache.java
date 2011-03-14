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
package org.jboss.ejb3.cache;

import javax.ejb.EJBException;
import javax.ejb.NoSuchEJBException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision$
 */
public class NoPassivationCache<T extends Identifiable> implements Cache<T>
{
   private StatefulObjectFactory<T> factory;
   private Map<Serializable, T> cacheMap;
   private AtomicInteger createCount = new AtomicInteger(0);
   private AtomicInteger removeCount = new AtomicInteger(0);
   private boolean running;
   
   public NoPassivationCache()
   {
      cacheMap = new HashMap<Serializable, T>();
   }

   public void start()
   {
      this.running = true;
   }

   public void stop()
   {
      synchronized (cacheMap)
      {
         cacheMap.clear();
      }
      this.running = false;
   }

   @Override
   public T create()
   {
      try
      {
         T instance = factory.createInstance();
         createCount.incrementAndGet();
         synchronized (cacheMap)
         {
            cacheMap.put(instance.getId(), instance);
         }
         return instance;
      }
      catch (EJBException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         throw new EJBException(e);
      }
   }

   @Override
   public void discard(Serializable key)
   {
      // TODO: can we really do this? it might be a failing pre-destroy?
      remove(key);
   }
   
   public T get(Serializable key) throws EJBException
   {
      synchronized (cacheMap)
      {
         T instance = cacheMap.get(key);
         if(instance == null)
         {
            throw new NoSuchEJBException("Could not find Stateful bean: " + key);
         }
         return instance;
      }
   }

   public void release(T instance)
   {
      // do nothing
   }

   public void remove(Serializable key)
   {
      T instance;
      synchronized (cacheMap)
      {
         instance = cacheMap.remove(key);
         if(instance == null)
            throw new NoSuchEJBException("Could not find Stateful bean: " + key);
      }
      removeCount.incrementAndGet();
      factory.destroyInstance(instance);
   }

   public int getCacheSize()
   {
	   return cacheMap.size();
   }
   
   public int getTotalSize()
   {
      return cacheMap.size();
   }
   
   public int getCreateCount()
   {
      return createCount.intValue();
   }
   
   public int getPassivatedCount()
   {
	   return 0;
   }
   
   public int getRemoveCount()
   {
      return removeCount.intValue();
   }
   
   public int getAvailableCount()
   {
      final int maxSize = this.getMaxSize();
      if (maxSize < 0)
      {
         return maxSize;
      }
      final int currentSize = this.getCurrentSize();
      final int available = maxSize - currentSize;
      return available;
   }
   
   public int getMaxSize()
   {
      return -1;
   }
   
   public int getCurrentSize()
   {
      return cacheMap.size();
   }
   
   public boolean isStarted()
   {
      return this.running;
   }

   public void setStatefulObjectFactory(StatefulObjectFactory<T> factory)
   {
      this.factory = factory;
   }
}
