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
package org.jboss.ejb3.cache.simple;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import javax.ejb.EJBException;
import javax.ejb.NoSuchEJBException;

import org.jboss.aop.Advisor;
import org.jboss.ejb3.EJBContainer;
import org.jboss.ejb3.annotation.CacheConfig;
import org.jboss.ejb3.annotation.PersistenceManager;
import org.jboss.ejb3.cache.StatefulCache;
import org.jboss.ejb3.cache.persistence.PersistenceManagerFactory;
import org.jboss.ejb3.cache.persistence.PersistenceManagerFactoryRegistry;
import org.jboss.ejb3.stateful.StatefulBeanContext;
import org.jboss.ejb3.stateful.StatefulContainer;
import org.jboss.logging.Logger;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision$
 */
public class SimpleStatefulCache implements StatefulCache
{
   private Logger log = Logger.getLogger(SimpleStatefulCache.class);

   private StatefulContainer container;
   protected CacheMap cacheMap;
   private int maxSize = 1000;
   private StatefulSessionPersistenceManager pm;
   private long sessionTimeout = 300; // 5 minutes
   private long removalTimeout = 0; // 0 == Never
   private Thread timeoutTask;
   private RemovalTimeoutTask removalTask = null;
   private boolean running = true;
   private int createCount = 0;
   private int passivatedCount = 0;
   private int removeCount = 0;
   
   private Queue<StatefulBeanContext> passivationQueue = new LinkedBlockingQueue<StatefulBeanContext>();

   protected class CacheMap extends LinkedHashMap<Object, StatefulBeanContext>
   {
      private static final long serialVersionUID = 4514182777643616159L;

      public CacheMap()
      {
         super(maxSize, 0.75F, true);
      }
      
      public CacheMap(Map<? extends Object, ? extends StatefulBeanContext> original)
      {
         super(original);
      }

      @Override
      public boolean removeEldestEntry(Entry<Object, StatefulBeanContext> entry)
      {
         boolean removeIt = size() > maxSize;
         if (removeIt)
         {
            StatefulBeanContext centry = (StatefulBeanContext) entry.getValue();
            synchronized (centry)
            {
               if (centry.getCanPassivate())
               {
                  passivate(centry);
                  // its ok to evict because bean will be passivated.
               }
               else
               {
                  centry.markedForPassivation = true;
                  
                  if (!centry.isInUse())
                  {
                     // Can't passivate but not in use means a child bean is 
                     // in use.
                     // It's not ok to evict because bean will not be passivated
                     removeIt = false;
                  }
               }               
            }
         }
         return removeIt;
      }
   }
   
   protected class RemovalTimeoutTask extends Thread
   {
      public RemovalTimeoutTask(String name)
      {
         super(name);
      }
      
      protected void block() throws InterruptedException
      {
         Thread.sleep(removalTimeout * 1000);
      }
      
      protected void preRemoval()
      {
         
      }
      
      protected void postRemoval()
      {
         
      }

      public void run()
      {
         while (running)
         { 
            try
            {
               this.block();
            }
            catch (InterruptedException e)
            {
               running = false;
               return;
            }
            try
            {
               // Invoke pre-removal callback
               this.preRemoval();
               
               long now = System.currentTimeMillis();
               
               synchronized (cacheMap)
               {
                  if (!running) return;
                   
                  Iterator<Entry<Object, StatefulBeanContext>> it = cacheMap.entrySet().iterator();
                  while (it.hasNext())
                  {
                     Entry<Object, StatefulBeanContext> entry = it.next();
                     StatefulBeanContext centry = entry.getValue();
                     if (now - centry.lastUsed >= removalTimeout * 1000)
                     {
                        synchronized (centry)
                        {                                                                    
                           it.remove();
                        }
                     }
                  }                  
               }
               
               List<StatefulBeanContext> beans = pm.getPassivatedBeans();  
               Iterator<StatefulBeanContext> it = beans.iterator();
               while (it.hasNext())
               {       
                  StatefulBeanContext centry = it.next();
                  if (now - centry.lastUsed >= removalTimeout * 1000)
                  {
                     get(centry.getId(), false);
                     remove(centry.getId());
                  }
               }
               
               // Invoke post-removal callback
               this.postRemoval();
            }
            catch (Exception ex)
            {
               log.error("problem removing SFSB thread", ex);
            }
         }
      }
   }

   protected class SessionTimeoutTask extends Thread
   {
      public SessionTimeoutTask(String name)
      {
         super(name);
      }

      public void block() throws InterruptedException
      {
         Thread.sleep(sessionTimeout * 1000);
      }
      
      /**
       * I'm done passivating.
       */
      protected void passivationCompleted()
      {
         
      }
      
      /**
       * I'm done selecting candidates for passivation.
       */
      protected void prePassivationCompleted()
      {
         
      }

      public void run()
      {
         while (running)
         {
            try
            {
               block();
            }
            catch (InterruptedException e)
            {
               running = false;
               return;
            }
            try
            {
               /*
                * EJBTHREE-1549
                * 
                * Passivation is potentially a long-running
                * operation, so copy the contents quickly and 
                * perform passivation off a queue.
                */
               synchronized (cacheMap)
               {
                  if (!running) return;
                  
                  boolean trace = log.isTraceEnabled();
                  Iterator<Entry<Object, StatefulBeanContext>> it = cacheMap.entrySet().iterator();
                  long now = System.currentTimeMillis();
                  while (it.hasNext())
                  {
                     Entry<Object, StatefulBeanContext> entry = it.next();
                     StatefulBeanContext centry = entry.getValue();
                     if (now - centry.lastUsed >= sessionTimeout * 1000)
                     {
                        synchronized (centry)
                        {                     
                           if (centry.getCanPassivate())
                           {
                              if (!centry.getCanRemoveFromCache())
                              {
                                 passivationQueue.add(centry);
                              }
                              else if (trace)
                              {
                                 log.trace("Removing " + entry.getKey() + " from cache");
                              }
                           }
                           else
                           {
                              centry.markedForPassivation = true;                              
                              assert centry.isInUse() : centry + " is not in use, and thus will never be passivated";
                           }
                           // its ok to evict because it will be passivated
                           // or we determined above that we can remove it
                           it.remove();
                        }
                     }
                     else if (trace)
                     {
                        log.trace("Not passivating; id=" + centry.getId() +
                              " only inactive " + Math.max(0, now - centry.lastUsed) + " ms");
                     }
                  }                  
               }
               
               prePassivationCompleted();
               
               StatefulBeanContext ctx;
               while ((ctx = passivationQueue.poll()) != null)
               {  
                  passivate(ctx);
               }
               
               // Make internal callback that we're done
               this.passivationCompleted();
            }

            catch (Exception ex)
            {
               log.error("problem passivation thread", ex);
            }
         }
      }
   }

   public void initialize(EJBContainer container) throws Exception
   {
      this.container = (StatefulContainer) container;
      Advisor advisor = container.getAdvisor();
      cacheMap = new CacheMap();
      PersistenceManager pmConfig = (PersistenceManager) advisor.resolveAnnotation(PersistenceManager.class);
      EJBContainer ejbContainer = (EJBContainer)container;
      String pmConfigValue = pmConfig.value();
      PersistenceManagerFactoryRegistry pmFactoryRegistry = ejbContainer.getDeployment()
            .getPersistenceManagerFactoryRegistry();
      PersistenceManagerFactory pmFactory = pmFactoryRegistry.getPersistenceManagerFactory(pmConfigValue);
      this.pm = pmFactory.createPersistenceManager();
      pm.initialize(container);
      CacheConfig config = (CacheConfig) advisor.resolveAnnotation(CacheConfig.class);
      maxSize = config.maxSize();
      sessionTimeout = config.idleTimeoutSeconds();
      removalTimeout = config.removalTimeoutSeconds();
      log = Logger.getLogger(getClass().getName() + "." + container.getEjbName());
      log.debug("Initializing SimpleStatefulCache with maxSize: " +maxSize + " timeout: " +sessionTimeout +
              " for " +container.getObjectName().getCanonicalName() );
      timeoutTask = new SessionTimeoutTask("SFSB Passivation Thread - " + container.getObjectName().getCanonicalName());
   
      if (removalTimeout > 0)
         removalTask = new RemovalTimeoutTask("SFSB Removal Thread - " + container.getObjectName().getCanonicalName());
   }

   public SimpleStatefulCache()
   {
   }

   public void start()
   {
      running = true;
      timeoutTask.start();
      
      if (removalTask != null)
         removalTask.start();
   }

   public void stop()
   {
      synchronized (cacheMap)
      {
         running = false;
         timeoutTask.interrupt();
         if (removalTask != null)
            removalTask.interrupt();
         cacheMap.clear();
         try
         {
            pm.destroy();
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }
      }
   }

   protected void passivate(StatefulBeanContext ctx)
   {
      ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
      try
      {
         Thread.currentThread().setContextClassLoader(((EJBContainer) ctx.getContainer()).getClassloader());
         synchronized(pm)
         {
            pm.passivateSession(ctx);
         }
         ++passivatedCount;
      }
      finally
      {
         Thread.currentThread().setContextClassLoader(oldCl);
      }
   }

   public StatefulBeanContext create()
   {
      return create(null, null);
   }

   public StatefulBeanContext create(Class<?>[] initTypes, Object[] initValues)
   {
      StatefulBeanContext ctx = null;
      try
      {
         ctx = container.create(initTypes, initValues);
         if (log.isTraceEnabled())
         {
            log.trace("Caching context " + ctx.getId() + " of type " + ctx.getClass());
         }
         synchronized (cacheMap)
         {
            cacheMap.put(ctx.getId(), ctx);
            ctx.setInUse(true);
            ctx.lastUsed = System.currentTimeMillis();
         }
         ++createCount;
      }
      catch (EJBException e)
      {
         e.printStackTrace();
         throw e;
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw new EJBException(e);
      }
      return ctx;
   }
   
   public StatefulBeanContext get(Object key) throws EJBException
   {
      return get(key, true);
   }
   
   public StatefulBeanContext get(Object key, boolean markInUse) throws EJBException
   {
      StatefulBeanContext entry = null;
      synchronized (cacheMap)
      {
         entry = cacheMap.get(key);
      }
      if(entry == null)
      {
         // TODO: optimize
         synchronized (cacheMap)
         {
            entry = cacheMap.get(key);
            if(entry == null)
            {
               Iterator<StatefulBeanContext> i = passivationQueue.iterator();
               while(i.hasNext())
               {
                  StatefulBeanContext ctx = i.next();
                  if(ctx.getId().equals(key))
                  {
                     boolean passivationCanceled = passivationQueue.remove(ctx);
                     if(passivationCanceled)
                     {
                        entry = ctx;
                        cacheMap.put(key, entry);
                     }
                     break;
                  }
               }
            }
         }
      }
      if (entry == null)
      {
         synchronized(pm)
         {
            synchronized (cacheMap)
            {
               entry = cacheMap.get(key);
            }
            if(entry == null)
            {
               entry = pm.activateSession(key);
               if (entry == null)
               {
                  throw new NoSuchEJBException("Could not find stateful bean: " + key);
               }
               --passivatedCount;
               
               // We cache the entry even if we will throw an exception below
               // as we may still need it for its children and XPC references
               if (log.isTraceEnabled())
               {
                  log.trace("Caching activated context " + entry.getId() + " of type " + entry.getClass());
               }
               
               synchronized (cacheMap)
               {
                  cacheMap.put(key, entry);
               }
            }
         }
      }
      
      // Now we know entry isn't null
      if (markInUse)
      { 
         if (entry.isRemoved())
         {
            throw new NoSuchEJBException("Could not find stateful bean: " + key +
                                         " (bean was marked as removed");
         }      
      
         entry.setInUse(true);
         entry.lastUsed = System.currentTimeMillis();
      }
      
      return entry;
   }

   public StatefulBeanContext peek(Object key) throws NoSuchEJBException
   {
      return get(key, false);
   }
   
   public void release(StatefulBeanContext ctx)
   {
      synchronized (ctx)
      {
         ctx.setInUse(false);
         ctx.lastUsed = System.currentTimeMillis();
         if (ctx.markedForPassivation)
         {
            passivate(ctx);
         }
      }
   }

   public void remove(Object key)
   {
      if(log.isTraceEnabled())
      {
         log.trace("Removing context " + key);
      }
      StatefulBeanContext ctx = null;
      synchronized (cacheMap)
      {
         ctx = cacheMap.get(key);
      }
      if(ctx == null)
         throw new NoSuchEJBException("Could not find Stateful bean: " + key);
      if (!ctx.isRemoved())
         container.destroy(ctx);
      
      ++removeCount;
      
      if (ctx.getCanRemoveFromCache())
      {
         synchronized (cacheMap)
         {
            cacheMap.remove(key);
         }
      }
   }

   public int getCacheSize()
   {
      return cacheMap.size();
   }

   public int getTotalSize()
   {
      return getCacheSize() + getPassivatedCount();
   }
   
   public int getCreateCount()
   {
      return createCount;
   }
   
   public int getPassivatedCount()
   {
      return passivatedCount;
   }
   
   public int getRemoveCount()
   {
      return removeCount;
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
      return maxSize;
   }
   
   public int getCurrentSize()
   {
      return cacheMap.size();
   }
   
   public boolean isStarted()
   {
      return this.running;
   }

   protected Thread getTimeoutTask()
   {
      return timeoutTask;
   }
   
   protected void setTimeoutTask(Thread timeoutTask)
   {
      this.timeoutTask = timeoutTask;
   }

   protected long getSessionTimeout()
   {
      return sessionTimeout;
   }

   protected long getRemovalTimeout()
   {
      return removalTimeout;
   }

   protected RemovalTimeoutTask getRemovalTask()
   {
      return removalTask;
   }

   protected void setRemovalTask(RemovalTimeoutTask removalTask)
   {
      this.removalTask = removalTask;
   }
}
