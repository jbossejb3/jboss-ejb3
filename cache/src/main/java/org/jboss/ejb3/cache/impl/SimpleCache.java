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
import org.jboss.ejb3.cache.StatefulObjectFactory;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class SimpleCache<T extends Identifiable> implements Cache<T>
{
   private StatefulObjectFactory<T> factory;
   private Map<Object, T> cache;
   
   public SimpleCache(StatefulObjectFactory<T> factory)
   {
      assert factory != null;
      
      this.factory = factory;
      this.cache = new HashMap<Object, T>();
   }
   
   public T create(Class<?>[] initTypes, Object[] initValues)
   {
      T obj = factory.create(initTypes, initValues);
      synchronized(cache)
      {
         cache.put(obj.getId(), obj);
      }
      return obj;
   }

   public T get(Object key) throws NoSuchEJBException
   {
      T obj;
      synchronized (cache)
      {
         obj = cache.get(key);
      }
      if(obj == null)
         throw new NoSuchEJBException(String.valueOf(key));
      return obj;
   }

   public T peek(Object key) throws NoSuchEJBException
   {
      return get(key);
   }
   
   public void release(T obj)
   {
      // release does nothing
   }
   
   public void remove(Object key)
   {
      T obj;
      synchronized (cache)
      {
         obj = cache.remove(key);
      }
      // EJBTHREE-1218: throw NoSuchEJBException if the bean can not be found
      if(obj == null)
         throw new NoSuchEJBException(String.valueOf(key));
      
      factory.destroy(obj);
   }
   
   public void start()
   {
      // do nothing
   }
   
   public void stop()
   {
      // do nothing
   }
}
