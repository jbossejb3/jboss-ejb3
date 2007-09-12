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
package org.jboss.ejb3.cache.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ejb.NoSuchEJBException;

import org.jboss.ejb3.cache.Identifiable;
import org.jboss.ejb3.cache.ObjectStore;
import org.jboss.ejb3.cache.PassivatingCache;
import org.jboss.ejb3.cache.PassivationManager;
import org.jboss.ejb3.cache.StatefulObjectFactory;
import org.jboss.logging.Logger;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class SimplePassivatingCache<T extends Identifiable & Serializable> implements PassivatingCache<T>
{
   private static final Logger log = Logger.getLogger(SimplePassivatingCache.class);
   
   private StatefulObjectFactory<T> factory;
   private PassivationManager<T> passivationManager;
   private ObjectStore<T> store;
   
   private Map<Object, Entry> cache;
   
   private int sessionTimeout = -1;
   private String name;
   
   private Thread sessionTimeoutTask;
   
   private static enum EntryState { READY, IN_USE };
   
   private class Entry
   {
      long lastUsed;
      T obj;
      EntryState state;
      
      Entry(T obj)
      {
         assert obj != null : "obj is null";
         
         this.lastUsed = System.currentTimeMillis();
         this.obj = obj;
         this.state = EntryState.IN_USE;
      }
   }
   
   private class SessionTimeoutThread extends Thread
   {
      public SessionTimeoutThread(String name)
      {
         super(name);
         setDaemon(true);
      }
      
      @Override
      public void run()
      {
         try
         {
            while(!Thread.currentThread().isInterrupted())
            {
               Thread.sleep(1000);
               
               synchronized (cache)
               {
                  if(Thread.currentThread().isInterrupted())
                     return;
                  
                  long then = System.currentTimeMillis() - sessionTimeout * 1000;
                  Iterator<Entry> it = cache.values().iterator();
                  while(it.hasNext())
                  {
                     Entry entry = it.next();
                     if(then >= entry.lastUsed && entry.state != EntryState.IN_USE)
                     {
                        // TODO: can passivate?
                        try
                        {
                           passivationManager.prePassivate(entry.obj);
                        }
                        catch(Throwable t)
                        {
                           log.warn("pre passivate failed for " + entry.obj, t);
                        }
                        
                        store.store(entry.obj);
                        
                        it.remove();
                     }
                  }
               }
            }
         }
         catch(InterruptedException e)
         {
            // do nothing
         }
      }
   }
   
   public SimplePassivatingCache(StatefulObjectFactory<T> factory, PassivationManager<T> passivationManager, ObjectStore<T> store)
   {
      assert factory != null : "factory is null";
      assert passivationManager != null : "passivationManager is null";
      assert store != null : "store is null";
      
      this.factory = factory;
      this.passivationManager = passivationManager;
      this.store = store;
      this.cache = new HashMap<Object, Entry>();
   }
   
   public T create(Class<?>[] initTypes, Object[] initValues)
   {
      T obj = factory.create(initTypes, initValues);
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
            T obj = store.load(key);
            if(obj != null)
            {
               passivationManager.postActivate(obj);
               
               entry = new Entry(obj);
               cache.put(key, entry);
               return entry.obj;
            }
         }
         if(entry == null)
            throw new NoSuchEJBException(String.valueOf(key));
         if(entry.state != EntryState.READY)
            throw new IllegalStateException("entry " + key + " is not ready");
         entry.state = EntryState.IN_USE;
         entry.lastUsed = System.currentTimeMillis();
         return entry.obj;
      }
   }

   public void passivate(Object key)
   {
      log.trace("passivate " + key);
      synchronized (cache)
      {
         Entry entry = cache.get(key);
         
         if(entry == null)
            throw new IllegalArgumentException("entry " + key + " not found in cache " + this);
         
         if(entry.state == EntryState.IN_USE)
            throw new IllegalStateException("entry " + entry + " is in use");
         
         passivationManager.prePassivate(entry.obj);
         
         store.store(entry.obj);
         
         cache.remove(key);
      }
   }
   
   public T peek(Object key) throws NoSuchEJBException
   {
      synchronized (cache)
      {
         Entry entry = cache.get(key);
         if(entry == null)
         {
            T obj = store.load(key);
            if(obj != null)
            {
               passivationManager.postActivate(obj);
               
               entry = new Entry(obj);
               cache.put(key, entry);
               entry.state = EntryState.READY;
            }
         }
         if(entry == null)
            throw new NoSuchEJBException(String.valueOf(key));
         return entry.obj;
      }
   }

   public void release(T obj)
   {
      releaseByKey(obj.getId());
   }

   protected void releaseByKey(Object key)
   {
      synchronized (cache)
      {
         Entry entry = cache.get(key);
         if(entry == null)
            throw new IllegalStateException("object " + key + " not from this cache");
         if(entry.state != EntryState.IN_USE)
            throw new IllegalStateException("entry " + entry + " is not in use");
         entry.state = EntryState.READY;
         entry.lastUsed = System.currentTimeMillis();
      }
   }
   
   public void remove(Object key)
   {
      Entry entry;
      synchronized (cache)
      {
         entry = cache.remove(key);
         if(entry.state != EntryState.READY)
            throw new IllegalStateException("entry " + entry + " is not ready");
      }
      if(entry != null)
         factory.destroy(entry.obj);
   }

   public void setName(String name)
   {
      this.name = name;
   }
   
   public void setSessionTimeout(int sessionTimeout)
   {
      assert sessionTimeout >= 0 : "sessionTimeout must be >= 0";
      this.sessionTimeout = sessionTimeout;
   }
   
   public void start()
   {
      assert name != null : "name has not been set";
      assert sessionTimeout != -1 : "sessionTimeout has not been set";
      
      if(sessionTimeout > 0)
      {
         sessionTimeoutTask = new SessionTimeoutThread("Passivation Thread - " + name);
         sessionTimeoutTask.start();
      }
   }

   public void stop()
   {
      if(sessionTimeoutTask != null)
      {
         sessionTimeoutTask.interrupt();
         try
         {
            sessionTimeoutTask.join(5000);
         }
         catch (InterruptedException e)
         {
            // ignore
         }
         if(sessionTimeoutTask.isAlive())
            log.warn("Failed to stop " + sessionTimeoutTask);
      }
   }

}
