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
package org.jboss.ejb3.pool;

import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.Container;
import org.jboss.ejb3.InfinitePool;
import org.jboss.injection.Injector;
import org.jboss.lang.ref.WeakThreadLocal;
import org.jboss.logging.Logger;


/**
 * Pools EJBs within a ThreadLocal.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision$
 */
public class ThreadlocalPool implements Pool
{
private static final Logger log = Logger.getLogger(ThreadlocalPool.class);
   
   protected Pool pool = new InfinitePool();
   protected WeakThreadLocal<BeanContext> currentBeanContext = new WeakThreadLocal<BeanContext>();
   private int inUse = 0;
   
   public ThreadlocalPool()
   {
   }

   protected BeanContext create()
   {
      return pool.get();
   }
   
   protected BeanContext create(Class[] initTypes, Object[] initValues)
   {
      return pool.get(initTypes, initValues);
   }

   public void discard(BeanContext obj)
   {
      pool.discard(obj);
      --inUse;
   }
   
   public void destroy()
   {
      log.trace("destroying pool");
      
      pool.destroy();
      
      // This really serves little purpose, because we want the whole thread local map to die
      currentBeanContext.remove();
      
      inUse = 0;
   }
   
   public BeanContext get()
   {
      BeanContext ctx = null;
      
      synchronized(pool)
      {
         ctx = currentBeanContext.get();
         if (ctx != null)
         {
            currentBeanContext.set(null);
            ++inUse;
            return ctx;
         }
   
         ctx = create();
         ++inUse;
      }
      
      return ctx;
   }

   public BeanContext get(Class[] initTypes, Object[] initValues)
   {
      BeanContext ctx = null;
      synchronized(pool)
      {
         ctx = currentBeanContext.get();
         if (ctx != null)
         {
            currentBeanContext.set(null);
            ++inUse;
            return ctx;
         }
   
         ctx = create(initTypes, initValues);
         ++inUse;
      }
      
      return ctx;
   }

   public void initialize(Container container, int maxSize, long timeout)
   {
      pool.initialize(container, maxSize, timeout);
   }
   
   public void release(BeanContext ctx)
   {
      synchronized(pool)
      {
         if (currentBeanContext.get() != null)
         {
            remove(ctx);
         }
         else
         {
            currentBeanContext.set(ctx);
         }
         
         --inUse;
      }
   }
   
   public void remove(BeanContext ctx)
   {
      pool.remove(ctx);
   }
   
   public int getCurrentSize()
   {
      int size;
      synchronized (pool)
      {
         size = pool.getCreateCount() - pool.getRemoveCount();
      }
      return size;
   }
   
   public int getAvailableCount()
   {
      return getMaxSize() - inUse;
   }
   
   public int getCreateCount()
   {
      return pool.getCreateCount();
   }
   
   public int getMaxSize()
   {
      // the thread local pool dynamically grows for new threads
      // if a bean is reentrant it'll grow and shrink over the reentrant call
      return getCurrentSize();
   }

   public int getRemoveCount()
   {
      return pool.getRemoveCount();
   }
   
   public void setInjectors(Injector[] injectors)
   {
      pool.setInjectors(injectors);
   }
   
   public void setMaxSize(int maxSize)
   {
      //this.maxSize = maxSize;
      log.warn("EJBTHREE-1703: setting a max size on ThreadlocalPool is bogus");
   }
}
