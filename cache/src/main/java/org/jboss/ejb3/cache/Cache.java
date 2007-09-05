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

import javax.ejb.NoSuchEJBException;

/**
 * Cache a stateful object and make sure any life cycle callbacks are
 * called at the appropiate time.
 * 
 * A cache is linked to an object factory. How the link is establish is left beyond
 * scope.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public interface Cache<T extends Identifiable>
{
   /**
    * Create a new object.
    * 
    * @param initTypes
    * @param initValues
    * @return
    */
   T create(Class<?> initTypes[], Object initValues[]);
   
   /**
    * Get the specified object from cache.
    * 
    * @param key    the identifier of the object
    * @return       the object
    * @throws NoSuchEJBException if the object does not exist
    */
   T get(Object key) throws NoSuchEJBException;
   
   /**
    * Remove the specified object from cache.
    * 
    * @param key    the identifier of the object
    */
   void remove(Object key);
}
