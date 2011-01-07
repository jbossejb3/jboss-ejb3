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

import org.jboss.ejb3.effigy.AccessTimeoutEffigy;
import org.jboss.ejb3.effigy.SessionBeanEffigy;
import org.jboss.ejb3.effigy.StatefulTimeoutEffigy;

import java.lang.reflect.Method;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class SessionBeanEffigyImpl extends EnterpriseBeanEffigyImpl implements SessionBeanEffigy
{
   @Override
   public AccessTimeoutEffigy getAccessTimeout(Method method)
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.dsl.SessionBeanEffigyImpl.getAccessTimeout");
   }

   @Override
   public Method getAfterBeginMethod()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.dsl.SessionBeanEffigyImpl.getAfterBeginMethod");
   }

   @Override
   public Method getAfterCompletionMethod()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.dsl.SessionBeanEffigyImpl.getAfterCompletionMethod");
   }

   @Override
   public Method getBeforeCompletionMethod()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.dsl.SessionBeanEffigyImpl.getBeforeCompletionMethod");
   }

   @Override
   public StatefulTimeoutEffigy getStatefulTimeout()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.dsl.SessionBeanEffigyImpl.getStatefulTimeout");
   }
}
