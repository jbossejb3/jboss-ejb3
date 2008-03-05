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
import org.jboss.ejb3.EJBContainer;
import org.jboss.ejb3.annotation.CacheConfig;
import org.jboss.ejb3.stateful.StatefulBeanContext;
import org.jboss.injection.Injector;
import org.jboss.logging.Logger;

/**
 * The base of all pool implementations.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision$
 */
public abstract class AbstractPool implements Pool
{
   @SuppressWarnings("unused")
   private static final Logger log = Logger.getLogger(AbstractPool.class);

   protected Injector[] injectors;
   protected Container container;
   protected int createCount = 0;
   protected int removeCount = 0;

   public AbstractPool()
   {

   }

   public int getCreateCount()
   {
      return createCount;
   }
   
   public int getRemoveCount()
   {
      return removeCount;
   }
   
   public void initialize(Container container, int maxSize, long timeout)
   {
      assert container != null : "container is null";
      
      this.container = container;
   }

   public abstract void setMaxSize(int maxSize);

   @Deprecated
   protected BeanContext<?> create()
   {
      return create(null, null);
   }
   
   protected BeanContext<?> create(Class[] initTypes, Object[] initValues)
   {
      BeanContext ctx;
      ctx = createBeanContext();
      if (ctx instanceof StatefulBeanContext)
      {         
         StatefulBeanContext sfctx = (StatefulBeanContext) ctx;
         // FIXME: remove this class cast
         EJBContainer c = (EJBContainer) container;
         // Tell context how to handle replication
         CacheConfig config = c.getAnnotation(CacheConfig.class);
         if (config != null)
         {
            sfctx.setReplicationIsPassivation(config.replicationIsPassivation());
         }
         // this is for propagated extended PC's
         ctx = sfctx = sfctx.pushContainedIn();
      }
      container.pushContext(ctx);
      try
      {
         if (injectors != null)
         {
            for (int i = 0; i < injectors.length; i++)
            {
               injectors[i].inject(ctx);
            }
         }

         ctx.initialiseInterceptorInstances();

      }
      finally
      {
         container.popContext();
         if (ctx instanceof StatefulBeanContext)
         {
            // this is for propagated extended PC's
            StatefulBeanContext sfctx = (StatefulBeanContext) ctx;
            sfctx.popContainedIn();
         }
      }
      
      container.invokePostConstruct(ctx, initValues);
      
      //TODO This needs to be reimplemented as replacement for create() on home interface
      container.invokeInit(ctx.getInstance(), initTypes, initValues);
      
      ++createCount;
      
      return ctx;
   }
   
   private BeanContext createBeanContext()
   {
      return container.createBeanContext();
   }
   
   public void remove(BeanContext ctx)
   {
      try
      {
         container.invokePreDestroy(ctx);
      }
      finally
      {
         ctx.remove();
         ++removeCount;
      }
   }

   public void discard(BeanContext<?> ctx)
   {
      remove(ctx);
   }

   public void setInjectors(Injector[] injectors)
   {
      this.injectors = injectors;
   }
}
