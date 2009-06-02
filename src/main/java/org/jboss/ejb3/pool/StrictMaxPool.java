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

import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJBException;

import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.Container;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @version $Revision$
 */
public class StrictMaxPool
        extends AbstractPool
{
// Constants -----------------------------------------------------
   public static final int DEFAULT_MAX_SIZE = 30;
   public static final long DEFAULT_TIMEOUT = Long.MAX_VALUE;

   // Attributes ----------------------------------------------------
   /**
    * A FIFO semaphore that is set when the strict max size behavior is in effect.
    * When set, only maxSize instances may be active and any attempt to get an
    * instance will block until an instance is freed.
    */
   private Semaphore strictMaxSize;
   private int inUse = 0;
   /**
    * The time in milliseconds to wait for the strictMaxSize semaphore.
    */
   private long strictTimeout;

   /**
    * The pool data structure
    */
   protected LinkedList pool = new LinkedList();
   /**
    * The maximum number of instances allowed in the pool
    */
   protected int maxSize = 30;

   Logger log = Logger.getLogger(StrictMaxPool.class);


   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   /**
    * super.initialize() must have been called in advance
    */
   public void initialize(Container container, int maxSize, long timeout)
   {
      super.initialize(container, maxSize, timeout);
      this.maxSize = maxSize;
      this.strictMaxSize = new Semaphore(maxSize, true);
      this.strictTimeout = timeout;
   }

   public int getCurrentSize()
   {
      return getCreateCount() - getRemoveCount();
   }

   public int getAvailableCount()
   {
      return maxSize - inUse;
   }

   public int getMaxSize()
   {
      return maxSize;
   }

   public void setMaxSize(int maxSize)
   {
      this.maxSize = maxSize;
      this.strictMaxSize = new Semaphore(maxSize, true);
   }

   /**
    * Get an instance without identity.
    * Can be used by finders,create-methods, and activation
    *
    * @return Context /w instance
    */
   public BeanContext get()
   {
      boolean trace = log.isTraceEnabled();
      if (trace)
         log.trace("Get instance " + this + "#" + pool.size() + "#" + container.getBeanClass());

      // Block until an instance is available
      try
      {
         boolean acquired = strictMaxSize.tryAcquire(strictTimeout, TimeUnit.MILLISECONDS);
         if (trace)
            log.trace("Acquired(" + acquired + ") strictMaxSize semaphore, remaining=" + strictMaxSize.availablePermits());
         if (acquired == false)
            throw new EJBException("Failed to acquire the pool semaphore, strictTimeout=" + strictTimeout);
      }
      catch (InterruptedException e)
      {
         throw new EJBException("Pool strictMaxSize semaphore was interrupted");
      }

      synchronized (pool)
      {
         if (!pool.isEmpty())
         {
            BeanContext bean = (BeanContext) pool.removeFirst();
            ++inUse;
            return bean;
         }
      }

      // Pool is empty, create an instance
      ++inUse;
      return create();

   }

   public BeanContext get(Class[] initTypes, Object[] initValues)
   {
      boolean trace = log.isTraceEnabled();
      if (trace)
         log.trace("Get instance " + this + "#" + pool.size() + "#" + container.getBeanClass());

      // Block until an instance is available
      try
      {
         boolean acquired = strictMaxSize.tryAcquire(strictTimeout, TimeUnit.MILLISECONDS);
         if (trace)
            log.trace("Acquired(" + acquired + ") strictMaxSize semaphore, remaining=" + strictMaxSize.availablePermits());
         if (acquired == false)
            throw new EJBException("Failed to acquire the pool semaphore, strictTimeout=" + strictTimeout);
      }
      catch (InterruptedException e)
      {
         throw new EJBException("Pool strictMaxSize semaphore was interrupted");
      }

      synchronized (pool)
      {
         if (!pool.isEmpty())
         {
            BeanContext bean = (BeanContext) pool.removeFirst();
            ++inUse;
            return bean;
         }
      }

      // Pool is empty, create an instance
      ++inUse;
      return create(initTypes, initValues);
   }

   /**
    * Return an instance after invocation.
    * <p/>
    * Called in 2 cases:
    * a) Done with finder method
    * b) Just removed
    *
    * @param ctx
    */
   public void release(BeanContext ctx)
   {
      if (log.isTraceEnabled())
      {
         String msg = pool.size() + "/" + maxSize + " Free instance:" + this
                      + "#" + container.getBeanClass();
         log.trace(msg);
      }

      try
      {
         // Add the unused context back into the pool
         boolean removeIt = false;
         synchronized (pool)
         {
            if (pool.size() < maxSize)
            {
               pool.addFirst(ctx);
            }
            else
            {
               removeIt = true;
            }
         }
         if (removeIt) remove(ctx);
         // If we block when maxSize instances are in use, invoke release on strictMaxSize
         strictMaxSize.release();
         --inUse;
      }
      catch (Exception ignored)
      {
      }

   }

   public void destroy()
   {
      freeAll();
   }

   public void discard(BeanContext ctx)
   {
      if (log.isTraceEnabled())
      {
         String msg = "Discard instance:" + this + "#" + ctx
                      + "#" + container.getBeanClass();
         log.trace(msg);
      }

      // If we block when maxSize instances are in use, invoke release on strictMaxSize
      strictMaxSize.release();
      --inUse;

      // Let the super do any other remove stuff
      super.doRemove(ctx);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------
   /*
   protected void destroy() throws Exception
   {
      freeAll();
      this.container = null;
   }
   */

   // Private -------------------------------------------------------

   /**
    * At undeployment we want to free completely the pool.
    */
   private void freeAll()
   {
      LinkedList clone = (LinkedList) pool.clone();
      for (int i = 0; i < clone.size(); i++)
      {
         BeanContext bc = (BeanContext) clone.get(i);
         // Clear TX so that still TX entity pools get killed as well
         discard(bc);
      }
      pool.clear();
      inUse = 0;

   }

   // Inner classes -------------------------------------------------

   @Override
   public void remove(BeanContext ctx)
   {
      if (log.isTraceEnabled())
      {
         String msg = "Removing instance:" + this + "#" + ctx + "#" + container.getBeanClass();
         log.trace(msg);
      }

      strictMaxSize.release();
      --inUse;
      // let the super do the other remove stuff
      super.doRemove(ctx);
   }
}
