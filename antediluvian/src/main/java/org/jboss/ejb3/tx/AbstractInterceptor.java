/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.tx;

import java.lang.annotation.Annotation;

import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.ejb3.interceptors.aop.ExtendedAdvisorHelper;
import org.jboss.ejb3.interceptors.container.AbstractContainer;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public abstract class AbstractInterceptor implements Interceptor
{
   protected <C> C getContainer(Invocation invocation)
   {
      return (C) AbstractContainer.getContainer(invocation.getAdvisor());
   }
   
   public final String getName()
   {
      return getClass().getName();
   }
   
   protected static <T extends Annotation> T resolveAnnotation(Invocation invocation, Class<?> cls, Class<T> annotationType)
   {
      return ExtendedAdvisorHelper.getExtendedAdvisor(invocation.getAdvisor(), invocation.getTargetObject()).resolveAnnotation(cls, annotationType);
   }
}
