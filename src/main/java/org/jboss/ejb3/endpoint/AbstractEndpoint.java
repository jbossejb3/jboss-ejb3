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

/**
 * An implementation of Endpoint that holds the association with an optional
 * SessionFactory.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public abstract class AbstractEndpoint implements Endpoint
{
   private SessionFactory factory;

   public AbstractEndpoint()
   {
      this(null);
   }
   
   public AbstractEndpoint(SessionFactory factory)
   {
      this.factory = factory;
   }
   
   public SessionFactory getSessionFactory() throws IllegalStateException
   {
      if(factory == null)
         throw new IllegalStateException("Endpoint " + this + " is not session aware");
      return factory;
   }

   public boolean isSessionAware()
   {
      return factory != null;
   }
}
