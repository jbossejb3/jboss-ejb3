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
package org.jboss.ejb3.effigy.dsl;

import org.jboss.ejb3.effigy.EnterpriseBeanEffigy;
import org.jboss.ejb3.effigy.InterceptorEffigy;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EnterpriseBeanFactory<T extends EnterpriseBeanEffigy>
{
   private T enterpriseBeanEffigy;

   protected EnterpriseBeanFactory(T enterpriseBeanEffigy)
   {
      this.enterpriseBeanEffigy = enterpriseBeanEffigy;
   }

   public <E extends EnterpriseBeanEffigy> E as(Class<E> type)
   {
      return type.cast(enterpriseBeanEffigy);
   }

   public EnterpriseBeanFactory<T> beanClass(Class<?> beanClass)
   {
      as(EnterpriseBeanEffigyImpl.class).setEjbClass(beanClass);
      return this;
   }

   public T effigy()
   {
      return enterpriseBeanEffigy;
   }

   public EnterpriseBeanFactory<T> interceptAllWith(InterceptorEffigy interceptor)
   {
      as(EnterpriseBeanEffigyImpl.class).addInterceptor(interceptor);
      return this;
   }

   public EnterpriseBeanFactory<T> name(String ejbName)
   {
      as(EnterpriseBeanEffigyImpl.class).setName(ejbName);
      return this;
   }
}
