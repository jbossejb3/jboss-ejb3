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

import java.lang.reflect.Method;
import java.util.LinkedList;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
abstract class AbstractInterceptedImpl
{
   private LinkedList<Method> aroundInvokes;
   private Class<?> interceptedClass;
   private LinkedList<Method> postConstructs;
   private LinkedList<Method> preDestroys;

   public boolean addAroundInvoke(Method method)
   {
      if(aroundInvokes == null)
         aroundInvokes = new LinkedList<Method>();
      return aroundInvokes.add(method);
   }

   public boolean addPostConstruct(Method method)
   {
      if(postConstructs == null)
         postConstructs = new LinkedList<Method>();
      return postConstructs.add(method);
   }

   public boolean addPreDestroy(Method method)
   {
      if(preDestroys == null)
         preDestroys = new LinkedList<Method>();
      return preDestroys.add(method);
   }

   protected Class<?> getInterceptedClass()
   {
      return interceptedClass;
   }

   public Iterable<Method> getAroundInvokes()
   {
      return aroundInvokes;
   }

   public Iterable<Method> getPostConstructs()
   {
      return postConstructs;
   }

   public Iterable<Method> getPreDestroys()
   {
      return preDestroys;
   }

   protected void setInterceptedClass(Class<?> cls)
   {
      this.interceptedClass = cls;
   }
}
