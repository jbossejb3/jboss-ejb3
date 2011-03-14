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

import org.jboss.ejb3.cache.Identifiable;
import org.jboss.ejb3.cache.ObjectStore;
import org.jboss.ejb3.cache.PassivatingCache;
import org.jboss.ejb3.cache.PassivationManager;
import org.jboss.ejb3.cache.StatefulObjectFactory;
import org.jboss.ejb3.cache.grouped.GroupedPassivatingCache;
import org.jboss.ejb3.cache.grouped.PassivationGroup;
import org.jboss.logging.Logger;

import javax.ejb.NoSuchEJBException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision$
 */
public class GroupedPassivatingCacheImpl<T extends Identifiable & Serializable> implements GroupedPassivatingCache<T>
{
   private static final Logger log = Logger.getLogger(GroupedPassivatingCacheImpl.class);
   
   private StatefulObjectFactory<T> factory;
   
   private final PassivatingCache<PassivationGroup> groupCache;
   private final SimplePassivatingCache<Entry> delegate;
   private final Map<Serializable, Entry> storage = new HashMap<Serializable, Entry>();
   
   protected class Entry implements Identifiable, Serializable
   {
      private static final long serialVersionUID = 1L;
      
      Serializable id;
      T obj;
      PassivationGroupImpl group;
      Serializable groupId;
      
      Entry(T obj)
      {
         assert obj != null : "obj is null";
         
         this.obj = obj;
         this.id = obj.getId();
      }
      
      public Serializable getId()
      {
         return id;
      }
      
      void passivate()
      {
         // make sure we don't passivate the group twice
         group = null;
         
         delegate.passivate(this.id);
         
         obj = null;
      }
      
      @Override
      public String toString()
      {
         return super.toString() + "{id=" + id + ",obj=" + obj + ",groupId=" + groupId + ",group=" + group + "}";
      }
   }
   
   private class EntryContainer implements StatefulObjectFactory<Entry>, PassivationManager<Entry>, ObjectStore<Entry>
   {
      private final PassivationManager<T> passivationManager;
      private final ObjectStore<T> store;
      
      EntryContainer(PassivationManager<T> passivationManager, ObjectStore<T> store)
      {
         this.passivationManager = passivationManager;
         this.store = store;
      }
      
      @Override
      public Entry createInstance()
      {
         return new Entry(factory.createInstance());
      }
      
      @Override
      public void destroyInstance(Entry entry)
      {
         factory.destroyInstance(entry.obj);
      }
      
      @Override
      public Entry load(Object key)
      {
         Entry entry = storage.get(key);
         if(entry != null)
         {
            log.trace("entry = " + entry);
            return entry;
         }
         // This only happens when there is no group
         T obj = store.load(key);
         if(obj == null)
            return null;
         return new Entry(obj);
      }
      
      @Override
      @SuppressWarnings("unchecked")
      public void postActivate(Entry entry)
      {
         log.trace("post activate " + entry);
         if(entry.obj == null)
         {
            if(entry.group == null)
            {
               // TODO: peek or get?
               entry.group = (PassivationGroupImpl) groupCache.get(entry.groupId);
            }
            entry.obj = (T) entry.group.getMember(entry.id);
         }
         passivationManager.postActivate(entry.obj);
      }
      
      @Override
      public void prePassivate(Entry entry)
      {
         log.trace("pre passivate " + entry);
         passivationManager.prePassivate(entry.obj);
         // Am I being called recursively
         if(entry.group != null)
         {
            entry.group.removeActive(entry.id);
            entry.group.prePassivate();
            groupCache.passivate(entry.groupId);
            // Why clear? Because entry is removed from active, and thus passivate is never called.
            entry.group = null;
            entry.obj = null;
         }
      }
      
      @Override
      public void store(Entry entry)
      {
         log.trace("store " + entry);
         if(entry.groupId == null)
            store.store(entry.obj);
         else
            storage.put(entry.id, entry);
      }
   }
   
   public GroupedPassivatingCacheImpl(PassivationManager<T> passivationManager, ObjectStore<T> store, PassivatingCache<PassivationGroup> groupCache)
   {
      assert groupCache != null : "groupCache is null";
      assert passivationManager != null : "passivationManager is null";
      
      this.groupCache = groupCache;
      EntryContainer container = new EntryContainer(passivationManager, store);
      this.delegate = new SimplePassivatingCache<Entry>(container, container);
      this.delegate.setStatefulObjectFactory(container);
   }
   
   @Override
   public void passivate(Object key)
   {
      delegate.passivate(key);
   }

   @Override
   public T create()
   {
      return delegate.create().obj;
   }

   @Override
   public void discard(Serializable key)
   {
      remove(key);
   }
   
   @Override
   public T get(Serializable key) throws NoSuchEJBException
   {
      return delegate.get(key).obj;
   }

   public T peek(Serializable key) throws NoSuchEJBException
   {
      return delegate.peek(key).obj;
   }

   @Override
   public void release(T obj)
   {
      delegate.releaseByKey(obj.getId());
   }

   @Override
   public void remove(Serializable key)
   {
      delegate.remove(key);
   }

   @Override
   public void setGroup(T obj, PassivationGroup group)
   {
      Entry entry;
      Object key = obj.getId();
      entry = delegate.peek(key);
      if(entry.group != null)
         throw new IllegalStateException("object " + key + " already associated with a passivation group");
      entry.group = (PassivationGroupImpl) group;
      entry.groupId = group.getId();
      // TODO: remove member at the appropriate time
      entry.group.addMember(key, entry);
   }

   public void setName(String name)
   {
      delegate.setName(name + "-delegate");
   }
   
   public void setSessionTimeout(int sessionTimeout)
   {
      delegate.setSessionTimeout(sessionTimeout);
   }
   
   @Override
   public void setStatefulObjectFactory(StatefulObjectFactory<T> factory)
   {
      this.factory = factory;
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
