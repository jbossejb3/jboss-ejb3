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

import java.util.HashMap;
import java.util.Map;

import org.jboss.ejb3.cache.grouped.PassivationGroup;
import org.jboss.logging.Logger;
import org.jboss.util.id.GUID;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision$
 */
public class PassivationGroupImpl implements PassivationGroup
{
   private static final Logger log = Logger.getLogger(PassivationGroupImpl.class);
   private static final long serialVersionUID = 1L;

   private Object id = new GUID();
   private Map<Object, Object> members = new HashMap<Object, Object>();
   private transient Map<Object, GroupedPassivatingCacheImpl.Entry> active = new HashMap<Object, GroupedPassivatingCacheImpl.Entry>();
   
//   protected PassivationGroup()
//   {
//      
//   }
   
   void addMember(Object key, GroupedPassivatingCacheImpl.Entry entry)
   {
      log.trace("add member " + key + ", " + entry);
      members.put(key, entry.obj);
      active.put(key, entry);
   }
   
   public Object getId()
   {
      return id;
   }
   
   Object getMember(Object key)
   {
      return members.get(key);
   }
   
   void postActivate()
   {
      // do nothing
   }
   
   void prePassivate()
   {
      for(GroupedPassivatingCacheImpl.Entry entry : active.values())
      {
         entry.passivate();
      }
      active.clear();
   }
   
   void removeActive(Object key)
   {
      active.remove(key);
   }
}
