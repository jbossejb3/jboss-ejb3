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

import java.util.Map;

import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.ejb3.stateful.StatefulBeanContext;
import org.jboss.logging.Logger;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @deprecated Use direct JBossCache passivation now.
 * @version $Revision$
 */
public class StatefulEvictionPolicy extends PassivationEvictionPolicy
{
   private static final Logger log = Logger.getLogger(StatefulEvictionPolicy.class);

   public StatefulEvictionPolicy()
   {
      super();
   }

   public void evict(Fqn fqn) throws Exception
   {
      // never allow root node to be evicted
      if (fqn.size() == 2)
      {
         StatefulBeanContext bean = (StatefulBeanContext) cache_.get(fqn, "bean");
         
         if (bean == null)
         {
            super.evict(fqn);
            return;
         }
         synchronized (bean)
         {
            if (bean.isInUse())
            {
               bean.markedForPassivation = true;
               super.evict(fqn);
               return;
            }
            super.evict(fqn);
         }
      }
   }
}
