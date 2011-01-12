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

import org.jboss.ejb3.effigy.InterceptorEffigy;
import org.jboss.ejb3.interceptors.dsl.InterceptorReferenceFactory;
import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorReference;
import org.jboss.interceptor.spi.metadata.MethodMetadata;
import org.jboss.interceptor.spi.model.InterceptionType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.jboss.ejb3.interceptors.dsl.MethodMetadataFactory.methods;
import static org.jboss.interceptor.spi.model.InterceptionType.AROUND_INVOKE;
import static org.jboss.interceptor.spi.model.InterceptionType.POST_CONSTRUCT;
import static org.jboss.interceptor.spi.model.InterceptionType.PRE_DESTROY;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class InterceptorInterceptorMetadata implements InterceptorMetadata<ClassMetadata<?>>
{
   private static final long serialVersionUID = 1L;
   
   private SimpleClassMetadata classMetadata;
   private InterceptorReference<ClassMetadata<?>> interceptorReference;
   private Map<InterceptionType, List<MethodMetadata>> interceptorMethods = new ConcurrentHashMap<InterceptionType, List<MethodMetadata>>();

   InterceptorInterceptorMetadata(InterceptorEffigy interceptor)
   {
      this.classMetadata = new SimpleClassMetadata(interceptor.getInterceptorClass());
      this.interceptorReference = InterceptorReferenceFactory.interceptorReference(classMetadata);
      putInIfExists(interceptorMethods, AROUND_INVOKE, methods(interceptor.getAroundInvokes()));
      putInIfExists(interceptorMethods, POST_CONSTRUCT, methods(interceptor.getPostConstructs()));
      putInIfExists(interceptorMethods, PRE_DESTROY, methods(interceptor.getPreDestroys()));
   }

   @Override
   public InterceptorReference<ClassMetadata<?>> getInterceptorReference()
   {
      return interceptorReference;
   }

   @Override
   public ClassMetadata<?> getInterceptorClass()
   {
      return classMetadata;
   }

   @Override
   public List<MethodMetadata> getInterceptorMethods(InterceptionType interceptionType)
   {
      // must be fast
      return interceptorMethods.get(interceptionType);
   }

   @Override
   public boolean isEligible(InterceptionType interceptionType)
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.interceptors.effigy.InterceptorInterceptorMetadata.isEligible");
   }

   @Override
   public boolean isTargetClass()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.interceptors.effigy.InterceptorInterceptorMetadata.isTargetClass");
   }

   private static <K, V> void putInIfExists(Map<K, V> map, K key, V value)
   {
      if(value == null)
         return;

      map.put(key, value);
   }
}
