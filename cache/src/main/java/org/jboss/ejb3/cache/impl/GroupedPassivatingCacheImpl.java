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
import java.util.Map;

import javax.ejb.NoSuchEJBException;

import org.jboss.ejb3.cache.Identifiable;
import org.jboss.ejb3.cache.ObjectStore;
import org.jboss.ejb3.cache.PassivatingCache;
import org.jboss.ejb3.cache.PassivationManager;
import org.jboss.ejb3.cache.StatefulObjectFactory;
import org.jboss.ejb3.cache.grouped.GroupedPassivatingCache;
import org.jboss.ejb3.cache.grouped.PassivationGroup;
import org.jboss.logging.Logger;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class GroupedPassivatingCacheImpl<T extends Identifiable & Serializable> implements GroupedPassivatingCache<T>
{
   private static final Logger log = Logger.getLogger(GroupedPassivatingCacheImpl.class);
   
   private PassivatingCache<PassivationGroup> groupCache;
   
   private SimplePassivatingCache<Entry> delegate;
   private Map<Object, Entry> storage = new HashMap<Object, Entry>();
   
   protected class Entry implements Identifiable, Serializable
   {
      private static final long serialVersionUID = 1L;
      
      Object id;
      T obj;
      PassivationGroupImpl group;
      Object groupId;
      
      Entry(T obj)
      {
         assert obj != null : "obj is null";
         
         this.obj = obj;
         this.id = obj.getId();
      }
      
      public Object getId()
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
      private StatefulObjectFactory<T> factory;
      private PassivationManager<T> passivationManager;
      private ObjectStore<T> store;
      
      EntryContainer(StatefulObjectFactory<T> factory, PassivationManager<T> passivationManager, ObjectStore<T> store)
      {
         this.factory = factory;
         this.passivationManager = passivationManager;
         this.store = store;
      }
      
      public Entry create(Class<?>[] initTypes, Object[] initValues)
      {
         return new Entry(factory.create(initTypes, initValues));
      }

      public void destroy(Entry entry)
      {
         factory.destroy(entry.obj);
      }
      
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
      
      @SuppressWarnings("unchecked")
      public void postActivate(Entry entry)
      {
         log.trace("post activate " + entry);
         if(entry.obj == null)
         {
            if(entry.group == null)
            {
               // TODO: peek or get?
               entry.group = (PassivationGroupImpl) groupCache.peek(entry.groupId);
            }
            entry.obj = (T) entry.group.getMember(entry.id);
         }
         passivationManager.postActivate(entry.obj);
      }
      
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
      
      public void store(Entry entry)
      {
         log.trace("store " + entry);
         if(entry.groupId == null)
            store.store(entry.obj);
         else
            storage.put(entry.id, entry);
      }
   }
   
   public GroupedPassivatingCacheImpl(StatefulObjectFactory<T> factory, PassivationManager<T> passivationManager, ObjectStore<T> store, PassivatingCache<PassivationGroup> groupCache)
   {
      assert groupCache != null : "groupCache is null";
      assert passivationManager != null : "passivationManager is null";
      
      this.groupCache = groupCache;
      EntryContainer container = new EntryContainer(factory, passivationManager, store);
      this.delegate = new SimplePassivatingCache<Entry>(container, container, container);
   }
   
   public void passivate(Object key)
   {
      delegate.passivate(key);
   }

   public T create(Class<?>[] initTypes, Object[] initValues)
   {
      return delegate.create(initTypes, initValues).obj;
   }

   public T get(Object key) throws NoSuchEJBException
   {
      return delegate.get(key).obj;
   }

   public T peek(Object key) throws NoSuchEJBException
   {
      return delegate.peek(key).obj;
   }

   public void release(T obj)
   {
      delegate.releaseByKey(obj.getId());
   }

   public void remove(Object key)
   {
      delegate.remove(key);
   }

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
   
   public void start()
   {
      delegate.start();
   }

   public void stop()
   {
      delegate.stop();
   }
}
