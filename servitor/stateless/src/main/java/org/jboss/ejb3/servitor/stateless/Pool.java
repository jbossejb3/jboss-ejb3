/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.ejb3.servitor.stateless;

/**
 * A pool of stateless objects.
 *
 * A pool is linked to an object factory. How this link is established
 * is left beyond scope.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface Pool<T>
{
   /**
    * Discard an object. This will be called
    * in case of a system exception.
    *
    * @param obj    the object
    */
   void discard(T obj);

   /**
    * Get the an object from the pool. This will mark
    * the object as being in use.
    *
    * @return       the object
    */
   T get();

   /**
    * Release the object from use.
    *
    * @param obj    the object
    */
   void release(T obj);
}
