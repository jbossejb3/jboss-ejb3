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

import org.jboss.ejb3.cache.legacy.Container;
import org.jboss.ejb3.cache.legacy.StatefulBeanContext;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision$
 */
public interface StatefulSessionPersistenceManager
{
   Logger log = Logger.getLogger(StatefulSessionFilePersistenceManager.class);
   /**
    * The default store directory name ("<tt>sessions</tt>").
    */
   String DEFAULT_STORE_DIRECTORY_NAME = "sessions";

   /**
    * Restores session state from the serialized file & invokes
    * {@link javax.ejb.SessionBean#ejbActivate} on the target bean.
    */
   StatefulBeanContext activateSession(Object id);

   /**
    * Invokes {@link javax.ejb.SessionBean#ejbPassivate} on the target bean and saves the
    * state of the session to a file.
    */
   void passivateSession(StatefulBeanContext ctx);
   
   List<StatefulBeanContext> getPassivatedBeans();

   /**
    * Removes the saved state file (if any) for the given session id.
    */
   void removePassivated(Object id);

   void destroy() throws Exception;

   public void initialize(Container container) throws Exception;
}
