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
package org.jboss.ejb3.pool;

import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.Container;
import org.jboss.injection.Injector;

/**
 * Minimally a pool acts as a factory for a bean.  It will handle callbacks
 * to ejbCreate and ejbRemove as well.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision$
 */
public interface Pool
{
   /**
    * Creates if no object is available in pool.  ejbCreate will be called if created
    */
   BeanContext<?> get();

   BeanContext<?> get(Class<?>[] initTypes, Object[] initValues);

   /**
    * Put bean back in pool
    */
   void release(BeanContext<?> obj);

   /**
    * Destroy bean.  ejbRemove callback is executed
    */
   void remove(BeanContext<?> obj);

   /**
    * Discard the bean.  Called in different context as remove.  If there is a system exception this is
    * called.
    *
    * @param obj
    */
   void discard(BeanContext<?> obj);

   public void setInjectors(Injector[] injectors);

   void initialize(Container container, int maxSize, long timeout);

   int getCurrentSize();
   
   int getAvailableCount();
   
   int getMaxSize();
   
   void setMaxSize(int maxSize);
   
   int getCreateCount();
   
   int getRemoveCount();
  
   /**
    * Destroy the pool.
    */
   void destroy();
}
