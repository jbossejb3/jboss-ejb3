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
package org.jboss.ejb3.interceptors.effigy;

import org.jboss.ejb3.effigy.EnterpriseBeanEffigy;
import org.jboss.ejb3.effigy.InterceptorEffigy;
import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.model.InterceptionModel;
import org.jboss.interceptor.spi.model.InterceptionType;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class EnterpriseBeanInterceptionModel implements InterceptionModel<ClassMetadata<?>, ClassMetadata<?>>
{
   private static final long serialVersionUID = 1L;
   
   private Set<InterceptorMetadata<ClassMetadata<?>>> allInterceptors;

   EnterpriseBeanInterceptionModel(EnterpriseBeanEffigy enterpriseBeanEffigy)
   {
      this.allInterceptors = buildAllInterceptors(enterpriseBeanEffigy.getAllInterceptors());
   }

   private static Set<InterceptorMetadata<ClassMetadata<?>>> buildAllInterceptors(Iterable<InterceptorEffigy> interceptors)
   {
      Set<InterceptorMetadata<ClassMetadata<?>>> result = new HashSet<InterceptorMetadata<ClassMetadata<?>>>();
      for(InterceptorEffigy interceptor : interceptors)
      {
         result.add(new InterceptorInterceptorMetadata(interceptor));
      }
      return result;
   }

   @Override
   public List<InterceptorMetadata<ClassMetadata<?>>> getInterceptors(InterceptionType interceptionType, Method method)
   {
      // must be fast!
      // TODO: do not blindly return allInterceptors
      return new LinkedList(allInterceptors);
   }

   @Override
   public Set<InterceptorMetadata<ClassMetadata<?>>> getAllInterceptors()
   {
      return allInterceptors;
   }

   @Override
   public ClassMetadata<?> getInterceptedEntity()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.interceptors.effigy.EnterpriseBeanInterceptionModel.getInterceptedEntity");
   }
}
