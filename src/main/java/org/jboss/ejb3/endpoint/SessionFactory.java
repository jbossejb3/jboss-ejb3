/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.endpoint;

import java.io.Serializable;

/**
 * Create sessions on an EJB. Usually this would be a stateful session bean.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public interface SessionFactory
{
   /**
    * Create a session on an EJB.
    * 
    * Under the hood, the EJB's construct will be called. Followed by injection, calling
    * the post-construct method and finally calling the appropriate init method. 
    * 
    * @param initTypes the argument types for the init method.
    * @param initValues the arguments for the init method.
    * @return the session created.
    */
   Serializable createSession(Class<?>[] initTypes, Object[] initValues);
   
   /**
    * Destroy a session on an EJB.
    * 
    * Invokes the pre-destroy on the EJB and destroys it.
    * 
    * @param session the session to destroy.
    */
   void destroySession(Serializable session);
}
