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

import org.jboss.ejb3.effigy.ApplicationExceptionEffigy;
import org.jboss.ejb3.effigy.EnterpriseBeanEffigy;
import org.jboss.ejb3.effigy.InterceptorEffigy;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class EnterpriseBeanEffigyImpl implements EnterpriseBeanEffigy
{
   private Class<?> ejbClass;
   private String name;
   // TODO: this is just a list of all methods interceptors, it needs to be more specific
   private List<InterceptorEffigy> interceptors;

   protected boolean addInterceptor(InterceptorEffigy interceptor)
   {
      if(interceptors == null)
         interceptors = new LinkedList<InterceptorEffigy>();
      return interceptors.add(interceptor);
   }

   @Override
   public Iterable<InterceptorEffigy> getAllInterceptors()
   {
      return interceptors;
   }

   @Override
   public ApplicationExceptionEffigy getApplicationException(Class<?> exceptionClass)
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.dsl.EnterpriseBeanEffigyImpl.getApplicationException");
   }

   @Override
   public Iterable<Method> getAroundInvokes()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.dsl.EnterpriseBeanEffigyImpl.getAroundInvokes");
   }

   @Override
   public Class<?> getEjbClass()
   {
      return ejbClass;
   }

   @Override
   public Iterable<InterceptorEffigy> getInterceptors(Method method)
   {
      return interceptors;
   }

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public Iterable<Method> getPostConstructs()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.dsl.EnterpriseBeanEffigyImpl.getPostConstructs");
   }

   @Override
   public Iterable<Method> getPreDestroys()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.dsl.EnterpriseBeanEffigyImpl.getPreDestroys");
   }

   public void setEjbClass(Class<?> cls)
   {
      this.ejbClass = cls;
   }

   public void setName(String name)
   {
      this.name = name;
   }
}
