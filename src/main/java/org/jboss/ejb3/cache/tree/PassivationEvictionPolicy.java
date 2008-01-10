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
import org.jboss.cache.eviction.LRUPolicy;
import org.jboss.logging.Logger;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @deprecated Use direct JBossCache passivation now.
 * @version $Revision$
 */
public class PassivationEvictionPolicy extends LRUPolicy
{
   private static final Logger log = Logger.getLogger(PassivationEvictionPolicy.class);
   
   public void evict(Fqn fqn) throws Exception
   {
      log.debug("EVICTING: " + fqn.toString() + " size: " + fqn.size() + " 0: " + fqn.get(0));
      super.evict(fqn);
   }
}
