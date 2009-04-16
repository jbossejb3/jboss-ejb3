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
package org.jboss.ejb3.cache.tree;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJBException;
import javax.ejb.NoSuchEJBException;

import org.jboss.cache.Cache;
import org.jboss.cache.CacheException;
import org.jboss.cache.CacheManager;
import org.jboss.cache.CacheStatus;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.Region;
import org.jboss.cache.RegionNotEmptyException;
import org.jboss.cache.config.EvictionRegionConfig;
import org.jboss.cache.notifications.annotation.CacheListener;
import org.jboss.cache.notifications.annotation.NodeActivated;
import org.jboss.cache.notifications.annotation.NodePassivated;
import org.jboss.cache.notifications.event.NodeActivatedEvent;
import org.jboss.cache.notifications.event.NodePassivatedEvent;
import org.jboss.ejb3.EJBContainer;
import org.jboss.ejb3.annotation.CacheConfig;
import org.jboss.ejb3.cache.ClusteredStatefulCache;
import org.jboss.ejb3.pool.Pool;
import org.jboss.ejb3.stateful.NestedStatefulBeanContext;
import org.jboss.ejb3.stateful.ProxiedStatefulBeanContext;
import org.jboss.ejb3.stateful.StatefulBeanContext;
import org.jboss.ejb3.stateful.StatefulContainer;
import org.jboss.ha.framework.server.CacheManagerLocator;
import org.jboss.logging.Logger;
import org.jboss.util.id.GUID;

/**
 * Clustered SFSB cache that uses JBoss Cache to cache and replicate
 * bean contexts.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Brian Stansberry
 *
 * @version $Revision$
 */
public class StatefulTreeCache implements ClusteredStatefulCache
{
   private static final int FQN_SIZE = 4; // depth of fqn that we store the session in.
   private static final String SFSB = "sfsb";
   private static final int DEFAULT_BUCKET_COUNT = 100;

   private static final String[] DEFAULT_HASH_BUCKETS = new String[DEFAULT_BUCKET_COUNT];

   static
   {
      for (int i = 0; i < DEFAULT_HASH_BUCKETS.length; i++)
      {
         DEFAULT_HASH_BUCKETS[i] = String.valueOf(i);
      }
   }

   private ThreadLocal<Boolean> localActivity = new ThreadLocal<Boolean>();
   private Logger log = Logger.getLogger(StatefulTreeCache.class);
   private WeakReference<ClassLoader> classloader;
   private Cache cache;
   private Fqn cacheNode;
   private Region region;
   private ClusteredStatefulCacheListener listener;

   public static long MarkInUseWaitTime = 15000;

   protected String[] hashBuckets = DEFAULT_HASH_BUCKETS;
   protected volatile int createCount = 0;
   protected volatile int passivatedCount = 0;
   protected volatile int removeCount = 0;
   protected volatile int totalSize = -1;
   protected long removalTimeout = 0;
   protected RemovalTimeoutTask removalTask = null;
   protected boolean running = true;
   protected Map<Object, Long> beans = null;
   protected CacheConfig cacheConfig;
   protected CacheManager cacheManager;
   protected StatefulContainer ejbContainer;
   protected Object shutdownLock = new Object();
   protected final Object metricsLock = new Object();

   public StatefulBeanContext create()
   {      
      return create(null, null);
   }

   public StatefulBeanContext create(Class[] initTypes, Object[] initValues)
   {
      StatefulBeanContext ctx = null;
      try
      {
         ctx = ejbContainer.create(initTypes, initValues);
         if (log.isTraceEnabled())
         {
            log.trace("Caching context " + ctx.getId() + " of type " + ctx.getClass());
         }
         putInCache(ctx);
         ctx.setInUse(true);
         ctx.lastUsed = System.currentTimeMillis();
         ++createCount;
         totalSize = -1;
         if (beans != null)
         {
            beans.put(ctx.getId(), new Long(ctx.lastUsed));
         }
      }
      catch (EJBException e)
      {
         throw e;
      }
      catch (Exception e)
      {
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
      Fqn id = getFqn(key, false);
      Boolean active = localActivity.get();
      try
      {
         localActivity.set(Boolean.TRUE);
         // If need be, gravitate
         cache.getInvocationContext().getOptionOverrides().setForceDataGravitation(true);
         entry = (StatefulBeanContext) cache.get(id, "bean");
      }
      catch (CacheException e)
      {
         RuntimeException re = convertToRuntimeException(e);
         throw re;
      }
      finally
      {
         localActivity.set(active);
      }

      if (entry == null)
      {
         throw new NoSuchEJBException("Could not find stateful bean: " + key);
      }
      else if (markInUse && entry.isRemoved())
      {
         throw new NoSuchEJBException("Could not find stateful bean: " + key +
                                      " (bean was marked as removed)");
      }

      entry.postReplicate();

      if (markInUse)
      {
         entry.setInUse(true);

         // Mark the Fqn telling the eviction thread not to passivate it yet.
         // Note the Fqn we use is relative to the region!
         region.markNodeCurrentlyInUse(new Fqn(key.toString()), MarkInUseWaitTime);
         entry.lastUsed = System.currentTimeMillis();
         if (beans != null)
         {
            beans.put(key, new Long(entry.lastUsed));
         }
      }

      if(log.isTraceEnabled())
      {
         log.trace("get: retrieved bean with cache id " +id.toString());
      }

      return entry;
   }

   public StatefulBeanContext peek(Object key) throws NoSuchEJBException
   {
      return get(key, false);
   }

   public void remove(Object key)
   {
      Fqn id = getFqn(key, false);
      try
      {
         if(log.isTraceEnabled())
         {
            log.trace("remove: cache id " +id.toString());
         }
         cache.getInvocationContext().getOptionOverrides().setForceDataGravitation(true);
         StatefulBeanContext ctx = (StatefulBeanContext) cache.get(id, "bean");
         
         if(ctx == null)
            throw new NoSuchEJBException("Could not find Stateful bean: " + key);
         
         if (!ctx.isRemoved())
         {
            ejbContainer.destroy(ctx);
         }
         else if (log.isTraceEnabled())
         {
            log.trace("remove: " +id.toString() + " already removed from pool");
         }

         if (ctx.getCanRemoveFromCache())
         {
            // Do a cluster-wide removal of the ctx
            cache.removeNode(id);
         }
         else
         {
            // We can't remove the ctx as it contains live nested beans
            // But, we must replicate it so other nodes know the parent is removed!
            putInCache(ctx);
            if(log.isTraceEnabled())
            {
               log.trace("remove: removed bean " +id.toString() + " cannot be removed from cache");
            }
         }
         
         if (beans != null)
         {
            beans.remove(key);
         }

         ++removeCount;
         totalSize = -1;
      }
      catch (CacheException e)
      {
         RuntimeException re = convertToRuntimeException(e);
         throw re;
      }
   }

   public void release(StatefulBeanContext ctx)
   {
      synchronized (ctx)
      {
         ctx.setInUse(false);
         ctx.lastUsed = System.currentTimeMillis();
         if (beans != null)
         {
            beans.put(ctx.getId(), new Long(ctx.lastUsed));
         }
         // OK, it is free to passivate now.
         // Note the Fqn we use is relative to the region!
         region.unmarkNodeCurrentlyInUse(getFqn(ctx.getId(), true));
      }
   }

   public void replicate(StatefulBeanContext ctx)
   {
      // StatefulReplicationInterceptor should only pass us the ultimate
      // parent context for a tree of nested beans, which should always be
      // a standard StatefulBeanContext
      if (ctx instanceof NestedStatefulBeanContext)
      {
         throw new IllegalArgumentException("Received unexpected replicate call for nested context " + ctx.getId());
      }

      try
      {
         putInCache(ctx);
      }
      catch (CacheException e)
      {
         RuntimeException re = convertToRuntimeException(e);
         throw re;
      }
   }

   public void initialize(EJBContainer container) throws Exception
   {
      this.ejbContainer = (StatefulContainer) container;
      
      log = Logger.getLogger(getClass().getName() + "." + this.ejbContainer.getEjbName());

      this.classloader = new WeakReference<ClassLoader>(this.ejbContainer.getClassloader());

      this.cacheManager = CacheManagerLocator.getCacheManagerLocator().getCacheManager(null);
      
      this.cacheConfig = (CacheConfig) this.ejbContainer.getAnnotation(CacheConfig.class);

      removalTimeout = cacheConfig.removalTimeoutSeconds() * 1000L;
      if (removalTimeout > 0)
      {
         this.beans = new ConcurrentHashMap<Object, Long>();
         this.removalTask = new RemovalTimeoutTask("SFSB Removal Thread - " + this.ejbContainer.getObjectName().getCanonicalName());
      }
   }

   protected EvictionRegionConfig getEvictionRegionConfig(Fqn fqn)
   {
      AbortableLRUAlgorithmConfiguration algoCfg = new AbortableLRUAlgorithmConfiguration();
      EvictionRegionConfig erc = new EvictionRegionConfig(fqn, algoCfg);

      log.debug("Setting time to live to " + cacheConfig.idleTimeoutSeconds() + " seconds and maxNodes to " + cacheConfig.maxSize());

      algoCfg.setTimeToLive((int) cacheConfig.idleTimeoutSeconds(), TimeUnit.SECONDS);
      algoCfg.setMaxNodes(cacheConfig.maxSize());

      // JBC 2.x used '0' to denote no limit; 3.x uses '-1' since '0' now denotes '0'.  
      if (algoCfg.getTimeToLive() == 0) algoCfg.setTimeToLive(-1);
      if (algoCfg.getMaxNodes() == 0) algoCfg.setMaxNodes(-1);

      return erc;
   }

   public void start()
   {      
      // Get our cache
      String name = cacheConfig.name();
      if (name == null || name.trim().length() == 0)
         name = CacheConfig.DEFAULT_CLUSTERED_OBJECT_NAME;
      try
      {
         cache = cacheManager.getCache(name, true);
      }
      catch (CacheException ce)
      {
         throw convertToRuntimeException(ce);
      }
      catch (RuntimeException re)
      {
         throw re;
      }
      catch (Exception e1)
      {
         throw new RuntimeException("Cannot get cache with name " + name, e1);
      }

      cacheNode = new Fqn(new Object[] { SFSB, this.ejbContainer.getDeploymentPropertyListString() });
      
      // Try to create an eviction region per ejb
      region = cache.getRegion(cacheNode, true);
      EvictionRegionConfig erc = getEvictionRegionConfig(cacheNode);
      region.setEvictionRegionConfig(erc);

      if (cache.getCacheStatus() != CacheStatus.STARTED)
      {
         if (cache.getCacheStatus() != CacheStatus.CREATED)
            cache.create();
         cache.start();
      }
      
      // JBCACHE-1136.  There's no reason to have state in an inactive region
      cleanBeanRegion();

      // Transfer over the state for the region
      region.registerContextClassLoader(classloader.get());
      try
      {
        region.activate();
      }
      catch (RegionNotEmptyException e)
      {
         // this can happen with nested bean contexts if gravitation
         // pulls a parent bean over after the parent region is stopped
         // Clean up and try again
         cleanBeanRegion();
         region.activate();
      }
      
      // JBCACHE-1349 -- ensure root node exists
      Node regionRoot = cache.getNode(cacheNode);
      if (regionRoot == null)
      {
         regionRoot = cache.getRoot().addChild(cacheNode);
      }
      regionRoot.setResident(true);
      
      log.debug("started(): created region: " +region + " for ejb: " + ejbContainer.getEjbName());
      
      // register to listen for cache events

      // TODO this approach may not be scalable when there are many beans
      // since then we will need to go thru N listeners to figure out which
      // one this event belongs to. Consider having a singleton listener
      listener = new ClusteredStatefulCacheListener();
      cache.addCacheListener(listener);

      if (removalTask != null)
         removalTask.start();

      totalSize = -1;
      running = true;
   }

   public void stop()
   {
      running = false;

      // Block until the removalTask is done removing a bean (if it is)
      synchronized (shutdownLock)
      {
         if (removalTask != null && removalTask.isAlive())
            removalTask.interrupt();
      }
      
      if (cache != null)
      {
         // Remove the listener
         if (listener != null)
            cache.removeCacheListener(listener);

         // Remove locally. We do this to clean up the persistent store,
         // which is not affected by the inactivateRegion call below.
         cleanBeanRegion();

         if (region != null)
         {
            region.deactivate();
            region.unregisterContextClassLoader();

            cache.removeRegion(region.getFqn());
            // Clear any queues
            region.resetEvictionQueues();
            region = null;
         }
      }

      classloader = null;

      // Return the cache
      String name = cacheConfig.name();
      if (name == null || name.trim().length() == 0)
         name = CacheConfig.DEFAULT_CLUSTERED_OBJECT_NAME;
      cacheManager.releaseCache(name);

      log.debug("stop(): StatefulTreeCache stopped successfully for " +cacheNode);
   }

   public int getCacheSize()
   {
      return getTotalSize() - getPassivatedCount();
   }

   public int getTotalSize()
   {
      int result = (beans != null) ? beans.size() : totalSize;
      
      if (result < 0)
      {     
         synchronized (metricsLock)
         {
            if (totalSize > 0)
            {
               result = totalSize;
            }
            else
            {
               int count = 0;
               try
               {
                  Set children = null;
                  for (int i = 0; i < hashBuckets.length; i++)
                  {
                     Node node = cache.getRoot().getChild(new Fqn(cacheNode, hashBuckets[i]));
                     if (node != null)
                     {
                        children = node.getChildrenNames();
                        count += (children == null ? 0 : children.size());
                     }
                  }
                  result = totalSize = count;
               }
               catch (CacheException e)
               {
                  log.error("Caught exception calculating total size", e);
                  result = -1;
               }
            }
         }
      }
      return result;
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
      return -1;
   }

   public int getMaxSize()
   {
      return (cacheConfig == null) ? -1 : cacheConfig.maxSize();
   }

   public int getCurrentSize()
   {
      return getCacheSize();
   }

   private void putInCache(StatefulBeanContext ctx)
   {
      Boolean active = localActivity.get();
      try
      {
         localActivity.set(Boolean.TRUE);
         ctx.preReplicate();
         cache.put(getFqn(ctx.getId(), false), "bean", ctx);
         ctx.markedForReplication = false;
      }
      finally
      {
         localActivity.set(active);
      }
   }

   private Fqn getFqn(Object id, boolean regionRelative)
   {
      String beanId = id.toString();
      int index;
      if (id instanceof GUID)
      {
         index = (id.hashCode()& 0x7FFFFFFF) % hashBuckets.length;
      }
      else
      {
         index = (beanId.hashCode()& 0x7FFFFFFF) % hashBuckets.length;
      }

      if (regionRelative)
         return new Fqn( new Object[] {hashBuckets[index], beanId} );
      else
         return new Fqn(cacheNode, hashBuckets[index], beanId);
   }

   private void cleanBeanRegion()
   {
      try {
         // Remove locally.
         cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(true);
         cache.removeNode(cacheNode);
      }
      catch (CacheException e)
      {
         log.error("Stop(): can't remove bean from the underlying distributed cache");
      }
   }

   /**
    * Creates a RuntimeException, but doesn't pass CacheException as the cause
    * as it is a type that likely doesn't exist on a client.
    * Instead creates a RuntimeException with the original exception's
    * stack trace.
    */
   private RuntimeException convertToRuntimeException(CacheException e)
   {
      RuntimeException re = new RuntimeException(e.getClass().getName() + " " + e.getMessage());
      re.setStackTrace(e.getStackTrace());
      return re;
   }

   /**
    * A CacheListener that allows us to get notifications of passivations and
    * activations and thus notify the cached StatefulBeanContext.
    */
   @CacheListener
   public class ClusteredStatefulCacheListener
   {
      @NodeActivated
      public void nodeActivated(NodeActivatedEvent event)
      {
         // Ignore everything but "post" events for nodes in our region
         if(event.isPre()) return;
         Map nodeData = event.getData();
         if (nodeData == null) return;
         Fqn fqn = event.getFqn();
         if(fqn.size() != FQN_SIZE) return;
         if(!fqn.isChildOrEquals(cacheNode)) return;

         // Don't activate a bean just so we can replace the object
         // with a replicated one
         if (Boolean.TRUE != localActivity.get())
         {
            // But we do want to record that the bean's now in memory
            --passivatedCount;
            return;
         }

         StatefulBeanContext bean = (StatefulBeanContext) nodeData.get("bean");

         if(bean == null)
         {
            throw new IllegalStateException("nodeLoaded(): null bean instance.");
         }

         --passivatedCount;
         totalSize = -1;
       
         if(log.isTraceEnabled())
         {
            log.trace("nodeLoaded(): send postActivate event to bean at fqn: " +fqn);
         }

         ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
         try
         {
            ClassLoader cl = classloader.get();
            if (cl != null)
            {
               Thread.currentThread().setContextClassLoader(cl);
            }

            bean.activateAfterReplication();
         }
         finally
         {
            Thread.currentThread().setContextClassLoader(oldCl);
         }

      }

      @NodePassivated
      public void nodePassivated(NodePassivatedEvent event)
      {
         // Ignore everything but "pre" events for nodes in our region
         if(!event.isPre()) return;
         Fqn fqn = event.getFqn();
         if(fqn.size() != FQN_SIZE) return;
         if(!fqn.isChildOrEquals(cacheNode)) return;

         StatefulBeanContext bean = null;
         ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
         Boolean active = localActivity.get();
         try
         {
            localActivity.set(Boolean.TRUE);
            bean = (StatefulBeanContext) event.getData().get("bean");
            if (bean != null)
            {
               ClassLoader cl = classloader.get();
               if (cl != null)
               {
                  Thread.currentThread().setContextClassLoader(cl);
               }

               if (!bean.getCanPassivate())
               {
                  // Abort the eviction
                  throw new ContextInUseException("Cannot passivate bean " + fqn +
                        " -- it or one if its children is currently in use");
               }

               if(log.isTraceEnabled())
               {
                  log.trace("nodePassivated(): send prePassivate event to bean at fqn: " +fqn);
               }

               bean.passivateAfterReplication();
               ++passivatedCount;
               totalSize = -1;
            }
         }
         catch (NoSuchEJBException e)
         {
            // TODO is this still necessary? Don't think we
            // should have orphaned proxies any more
            if (bean instanceof ProxiedStatefulBeanContext)
            {
               // This is probably an orphaned proxy; double check and remove it
               try
               {
                  bean.getContainedIn();
                  // If that didn't fail, it's not an orphan
                  throw e;
               }
               catch (NoSuchEJBException n)
               {
                  log.debug("nodePassivated(): removing orphaned proxy at " + fqn);
                  try
                  {
                     cache.removeNode(fqn);
                  }
                  catch (CacheException c)
                  {
                     log.error("nodePassivated(): could not remove orphaned proxy at " + fqn, c);
                     // Just fall through and let the eviction try
                  }
               }
            }
            else
            {
               throw e;
            }
         }
         finally
         {
            Thread.currentThread().setContextClassLoader(oldCl);
            localActivity.set(active);
         }
      }
   }
   
   private class RemovalTimeoutTask extends Thread
   {
      public RemovalTimeoutTask(String name)
      {
         super(name);
      }

      public void run()
      {
         while (running)
         {
            try
            {
               Thread.sleep(removalTimeout);
            }
            catch (InterruptedException e)
            {
               running = false;
               return;
            }
            try
            {
               long now = System.currentTimeMillis();

               Iterator<Map.Entry<Object, Long>> it = beans.entrySet().iterator();
               
               // Block stop() processing while we process
               synchronized (shutdownLock)
               {
                  while (running && it.hasNext())
                  {
                     Map.Entry<Object, Long> entry = it.next();
                     long lastUsed = entry.getValue().longValue();
                     if (now - lastUsed >= removalTimeout)
                     {
                        try
                        {
                           remove(entry.getKey());
                        }
                        catch (NoSuchEJBException nosuch)
                        {
                           it.remove();
                        }
                        catch (Exception e)
                        {
                           log.error("problem removing SFSB " + entry.getKey(), e);
                        }
                     }
                  }
               }
            }
            catch (Exception ex)
            {
               log.error("problem removing SFSB thread", ex);
            }
         }
      }
   }
   
   public boolean isStarted()
   {
      return this.running;
   }
}
