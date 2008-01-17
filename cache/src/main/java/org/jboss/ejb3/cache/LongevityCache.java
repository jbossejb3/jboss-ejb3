/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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

/**
 * An longevity cache keeps hold over an object for use with multiple operations.
 * 
 * Thus where a normal cache only allows: get, release, get, release.
 * The longevity cache allows: get, finished, get, finished, release.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision$
 */
public interface LongevityCache<T extends Identifiable> extends Cache<T>
{
   /**
    * Signal the finish of the current operation on the object.
    * The object will remain in use, but is ready to be used
    * for a new operation.
    * 
    * @param obj
    */
   void finished(T obj);
}
