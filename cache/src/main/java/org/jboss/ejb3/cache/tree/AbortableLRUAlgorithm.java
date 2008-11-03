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

import org.jboss.cache.Fqn;
import org.jboss.cache.eviction.EvictionPolicy;
import org.jboss.cache.eviction.LRUAlgorithm;
import org.jboss.cache.lock.TimeoutException;
import org.jboss.logging.Logger;

/**
 * LRUAlgorithm subclass that doesn't log an error if it catches
 * ContextInUseException.
 * 
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1.1 $
 */
public class AbortableLRUAlgorithm extends LRUAlgorithm
{
   private static final Logger log = Logger.getLogger(AbortableLRUAlgorithm.class);
   
   public AbortableLRUAlgorithm()
   {
      super();
   }
   
   /**
    * Evict a node from cache.
    *
    * @param fqn node corresponds to this fqn
    * @return True if successful
    */
   protected boolean evictCacheNode(Fqn fqn)
   {
      if (log.isTraceEnabled())
      {
         log.trace("Attempting to evict cache node with fqn of " + fqn);
      }
      
      try
      {
         evictionActionPolicy.evict(fqn);
      }
      catch (ContextInUseException e)
      {
         // Don't log it at any alarming level
         if (log.isTraceEnabled())
            log.trace("Eviction of " + fqn + " aborted as bean is in use");
         return false;
      }
      catch (TimeoutException e)
      {
         log.warn("Eviction of " + fqn + " timed out, retrying later");
         log.debug(e, e);
         return false;
      }
      catch (RuntimeException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof ContextInUseException)
         {
            // Don't log it at any alarming level
            if (log.isTraceEnabled())
               log.trace("Eviction of " + fqn + " aborted as bean is in use");
            return false;            
         }
         log.error("Eviction of " + fqn + " failed", e);
         return false;
      }
      catch (Exception e)
      {
         log.error("Eviction of " + fqn + " failed", e);
         return false;
      }

      if (log.isTraceEnabled())
      {
         log.trace("Eviction of cache node with fqn of " + fqn + " successful");
      }

      return true;
   }

}
