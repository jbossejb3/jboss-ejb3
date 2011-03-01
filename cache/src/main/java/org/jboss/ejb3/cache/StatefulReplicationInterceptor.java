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
package org.jboss.ejb3.cache;

import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.ejb3.cache.legacy.StatefulBeanContext;
import org.jboss.ejb3.cache.legacy.StatefulContainer;
import org.jboss.ejb3.cache.legacy.StatefulContainerInvocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Replicate SFSB if it is modified.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Brian Stansberry
 * 
 * @version $Revision$
 */
public class StatefulReplicationInterceptor implements Interceptor
{
   private static final ThreadLocal<Map<StatefulBeanContext, Stack<Boolean>>> replicationContext = 
            new ThreadLocal<Map<StatefulBeanContext, Stack<Boolean>>>();
   
   public String getName()
   {
      return this.getClass().getName();
   }

   public Object invoke(Invocation invocation) throws Throwable
   {
      // Find the ultimate parent context for the tree of SFSBs the target
      // bean is part of.  This "tree" could just be the bean itself, or
      // a multi-layer tree of nested SFSBs.
      StatefulContainerInvocation ejbInv = (StatefulContainerInvocation) invocation;
      StatefulBeanContext ctx = (StatefulBeanContext) ejbInv.getBeanContext();
      StatefulBeanContext root = ctx.getUltimateContainedIn();
      
      // Find out if the ultimate parent is clustered
      boolean clustered = false;
      StatefulContainer container = (StatefulContainer) root.getContainer();
      ClusteredStatefulCache clusteredCache = null;
      if (container.getCache() instanceof ClusteredStatefulCache)
      {
         clustered = true;
         clusteredCache = (ClusteredStatefulCache) container.getCache();
      }
      
      // Track nested calls to this tree so we know when the outer call
      // returns -- that's when we replicate
      if (clustered)         
         pushCallStack(root);
      
      boolean stackUnwound = false;
      Object rtn = null;
      try
      {
         rtn = invocation.invokeNext();
      }
      finally
      {
         stackUnwound = (clustered && isCallStackUnwound(root));
      }


      // We only replicate if the ultimate parent is clustered
      // TODO should we fail somehow during bean creation otherwise??
      boolean mustReplicate = clustered;
      
      // If the bean implements Optimized, we call isModified() even
      // if we know we won't replicate, as the bean might be expecting 
      // us to call the method
      Object obj = invocation.getTargetObject();
      if (obj instanceof Optimized)
      {
         if (((Optimized) obj).isModified() == false)
         {
            mustReplicate = false;
         }
      }
      
      if (mustReplicate)
      {
         // Mark the bean for replication. If the call stack is not
         // unwound yet this will tell the outer caller the tree is 
         // dirty even if the outer bean's isModified() returns false
         root.setMarkedForReplication(true);
      }
      
      if (stackUnwound && root.isMarkedForReplication())
      {
         clusteredCache.replicate(root);
      }
      
      if (ctx != root && ctx.isMarkedForReplication())
      {
         // ctx is a ProxiedStatefulBeanContext that may have failed over
         // and needs to invalidate any remote nodes that hold stale refs
         // to their delegate. So we replicate it.
         container = (StatefulContainer) ctx.getContainer();
         StatefulCache cache = container.getCache();
         if (cache instanceof ClusteredStatefulCache)
         {
            clusteredCache = (ClusteredStatefulCache) cache;
            clusteredCache.replicate(ctx);
         }
         else
         {
            // not replicable
            ctx.setMarkedForReplication(false);
         }
      }
      
      return rtn;
   }
   
   private static void pushCallStack(StatefulBeanContext ctx)
   {
      Stack<Boolean> callStack = null;
      Map<StatefulBeanContext, Stack<Boolean>> map = replicationContext.get();
      if (map == null)
      {
         map = new HashMap<StatefulBeanContext, Stack<Boolean>>();
         replicationContext.set(map);
      }
      else
      {
         callStack = map.get(ctx);
      }

      if (callStack == null)
      {
         callStack = new Stack<Boolean>();
         map.put(ctx, callStack);
      }
      
      callStack.push(Boolean.TRUE);
   }
   
   private static boolean isCallStackUnwound(StatefulBeanContext ctx)
   {
      Map<StatefulBeanContext, Stack<Boolean>> map = replicationContext.get();
      if (map == null)
      {
         throw new IllegalStateException("replicationContext contains no Map");
      }
      Stack<Boolean> callStack = map.get(ctx);
      if (callStack == null)
      {
         throw new IllegalStateException("replicationContext contains no call stack");
      }
      
      callStack.pop();
      boolean unwound = (callStack.size() == 0);
      
      if (unwound)
      {
         map.remove(ctx);
         if (map.size() == 0)
         {
            replicationContext.set(null);
         }
      }
      
      return unwound;
   }
}
