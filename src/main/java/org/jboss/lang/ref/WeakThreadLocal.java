/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.lang.ref;

import java.lang.ref.WeakReference;

/**
 * @author carlo
 *
 */
public class WeakThreadLocal<T>
{
   private ThreadLocal<WeakReference<T>> delegate = new ThreadLocal<WeakReference<T>>();

   public T get()
   {
      WeakReference<T> ref = delegate.get();
      if(ref == null)
         return null;
      return ref.get();
   }
   
   public void remove()
   {
      delegate.remove();
   }

   public void set(T value)
   {
      delegate.set(new WeakReference<T>(value));
   }
}
