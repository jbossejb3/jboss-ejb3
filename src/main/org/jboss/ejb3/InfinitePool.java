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
package org.jboss.ejb3;

import java.util.LinkedList;
import java.util.List;

import org.jboss.ejb3.pool.AbstractPool;

/**
 * A pool that has no constraints.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: 67042 $
 */
public class InfinitePool extends AbstractPool
{
   private List<BeanContext> active = new LinkedList<BeanContext>();
   
   public void destroy()
   {
      for(BeanContext ctx : active)
      {
         // call super.remove or else get concurrent modification
         super.remove(ctx);
      }
      active = null;
   }

   public BeanContext<?> get()
   {
      return get(null, null);
   }

   public BeanContext<?> get(Class[] initTypes, Object[] initValues)
   {
      BeanContext ctx = create(initTypes, initValues);
      synchronized(active)
      {
         active.add(ctx);
      }
      return ctx;
   }

   public int getAvailableCount()
   {
      return -1;
   }

   public int getCurrentSize()
   {
      return active.size();
   }

   public int getMaxSize()
   {
      return -1;
   }

   public void release(BeanContext ctx)
   {
      remove(ctx);
   }

   public void remove(BeanContext ctx)
   {
      synchronized(active)
      {
         active.remove(ctx);
      }
      
      super.remove(ctx);
   }
   
   public void setMaxSize(int maxSize)
   {
   }

}
