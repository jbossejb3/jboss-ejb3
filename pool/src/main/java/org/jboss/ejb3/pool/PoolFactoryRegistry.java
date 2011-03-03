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
package org.jboss.ejb3.pool;

import java.util.Map;

/**
 * Registry for all configured Pool Factory implementations
 * 
 * @author <a href="mailto:andrew.rubinger@redhat.com">ALR</a>
 * @version $Revision: $
 */
public class PoolFactoryRegistry
{
   // Instance Members
   private Map<String, Class<? extends PoolFactory>> factories;

   // Accessors / Mutators

   public Map<String, Class<? extends PoolFactory>> getFactories()
   {
      return factories;
   }

   public void setFactories(Map<String, Class<? extends PoolFactory>> factories)
   {
      this.factories = factories;
   }

   // Functional Methods

   /**
    * Obtains the Pool Factory with the specified registered name
    * 
    * @param name The registered name of the pool factory to retrieve
    * @return The Pool Factory
    */
   public PoolFactory getPoolFactory(String name) throws PoolFactoryNotRegisteredException
   {
      // Obtain cache factory
      Class<? extends PoolFactory> poolFactory = this.factories.get(name);

      // Ensure registered
      if (poolFactory == null)
      {
         throw new PoolFactoryNotRegisteredException("Pool Factory with name " + name + " is not registered.");
      }

      try
      {
         // Return 
         return poolFactory.newInstance();
      }
      catch (InstantiationException e)
      {
         throw new RuntimeException("Error in instanciating pool factory " + poolFactory.getName(), e);
      }
      catch (IllegalAccessException e)
      {
         throw new RuntimeException(e);
      }
   }
}
