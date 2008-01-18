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

import java.util.Map;

/**
 * Registry for all configured Stateful Cache Factory implementations
 * 
 * @author <a href="mailto:andrew.rubinger@redhat.com">ALR</a>
 * @version $Revision: $
 */
public class CacheFactoryRegistry
{
   // Instance Members
   private Map<String, Class<? extends Ejb3CacheFactory>> factories;

   // Accessors / Mutators

   public Map<String, Class<? extends Ejb3CacheFactory>> getFactories()
   {
      return factories;
   }

   public void setFactories(Map<String, Class<? extends Ejb3CacheFactory>> factories)
   {
      this.factories = factories;
   }

   // Functional Methods

   /**
    * Obtains the Cache Factory with the specified registered name
    * 
    * @param name The registered name of the cache factory to retrieve
    * @return The Cache Factory
    */
   public Ejb3CacheFactory getCacheFactory(String name) throws CacheFactoryNotRegisteredException
   {
      // Obtain cache factory
      Class<? extends Ejb3CacheFactory> cacheFactory = this.factories.get(name);

      // Ensure registered
      if (cacheFactory == null)
      {
         throw new CacheFactoryNotRegisteredException("Cache Factory with name " + name + " is not registered.");
      }

      try
      {
         // Return 
         return cacheFactory.newInstance();
      }
      catch (InstantiationException e)
      {
         throw new RuntimeException("Error in instanciating cache factory " + cacheFactory.getName(), e);
      }
      catch (IllegalAccessException e)
      {
         throw new RuntimeException(e);
      }
   }
}
