/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @authors tag. See the copyright.txt in the
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
package org.jboss.ejb3.effigy.int2;

import org.jboss.ejb3.effigy.common.JBossSessionBeanEffigy;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;

import java.lang.reflect.Method;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class JBossSessionBean31Effigy extends JBossSessionBeanEffigy
{
   private Method afterBeginMethod;
   private Method afterCompletionMethod;
   private Method beforeCompletionMethod;
   
   protected JBossSessionBean31Effigy(ClassLoader classLoader, JBossSessionBean31MetaData beanMetaData)
           throws ClassNotFoundException
   {
      super(classLoader, beanMetaData);

      this.afterBeginMethod = method(beanMetaData.getAfterBeginMethod());
      this.afterCompletionMethod = method(beanMetaData.getAfterCompletionMethod());
      this.beforeCompletionMethod = method(beanMetaData.getBeforeCompletionMethod());
   }

   @Override
   public Method getAfterBeginMethod()
   {
      return afterBeginMethod;
   }

   @Override
   public Method getAfterCompletionMethod()
   {
      return afterCompletionMethod;
   }

   @Override
   public Method getBeforeCompletionMethod()
   {
      return beforeCompletionMethod;
   }

   private Method method(NamedMethodMetaData namedMethod)
   {
      if(namedMethod == null)
         return null;

      return ClassHelper.findMethod(getEjbClass(), namedMethod.getMethodName());
   }
}
