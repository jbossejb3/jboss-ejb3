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
package org.jboss.ejb3.test.strictpool;

import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.Container;
import org.jboss.ejb3.pool.AbstractPool;


/**
 * @version <tt>$Revision: 67042 $</tt>
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public class BogusPool
        extends AbstractPool
{
   public void initialize(Container container, Class contextClass, Class beanClass, int maxSize, long timeout)
   {
      throw new RuntimeException("Bogus");
   }

   public BeanContext<?> get()
   {
      throw new RuntimeException("Bogus");
   }

   public BeanContext<?> get(Class[] initTypes, Object[] initValues)
   {
      throw new RuntimeException("Bogus");
   }

   public void release(BeanContext ctx)
   {
      throw new RuntimeException("Bogus");
   }

   @Override
   public void setMaxSize(int maxSize)
   {
      throw new RuntimeException("Bogus");   
   }
   
   public void destroy()
   {
      throw new RuntimeException("Bogus");   
   }
   
   public void discard(BeanContext ctx)
   {
      throw new RuntimeException("Bogus");
   }

   public int getAvailableCount()
   {
      throw new RuntimeException("Bogus");
   }

   public int getCreateCount()
   {
      throw new RuntimeException("Bogus");
   }

   public int getCurrentSize()
   {
      throw new RuntimeException("Bogus");
   }

   public int getMaxSize()
   {
      throw new RuntimeException("Bogus");
   }

   public int getRemoveCount()
   {
      throw new RuntimeException("Bogus");
   }
}
