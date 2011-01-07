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
package org.jboss.ejb3.interceptors.dsl;

import org.jboss.interceptor.reader.SimpleInterceptorMetadata;
import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorReference;
import org.jboss.interceptor.spi.metadata.MethodMetadata;
import org.jboss.interceptor.spi.model.InterceptionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.interceptor.spi.model.InterceptionType.AROUND_INVOKE;
import static org.jboss.interceptor.spi.model.InterceptionType.POST_CONSTRUCT;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class InterceptorMetadataFactory
{
   public static Map<InterceptionType, List<MethodMetadata>> aroundInvokes(List<MethodMetadata> methods)
   {
      return interceptorMethodMap(AROUND_INVOKE, methods);
   }

   public static InterceptorMetadata<?> interceptor(InterceptorReference<ClassMetadata<?>> interceptorReference, Map<InterceptionType, List<MethodMetadata>> interceptorMethodMap)
   {
      return new SimpleInterceptorMetadata<ClassMetadata<?>>(interceptorReference, false, interceptorMethodMap);
   }

   public static Map<InterceptionType, List<MethodMetadata>> interceptorMethodMap(InterceptionType type, List<MethodMetadata> methods)
   {
      Map<InterceptionType, List<MethodMetadata>> map = new HashMap<InterceptionType, List<MethodMetadata>>();
      map.put(type, methods);
      return map;
   }

   public static <K, V> Map<K, V> map(Map<? extends K, ? extends V>... maps)
   {
      Map<K, V> map = new HashMap<K, V>();
      for(Map<? extends K, ? extends V> m : maps)
      {
         map.putAll(m);
      }
      return map;
   }

   public static Map<InterceptionType, List<MethodMetadata>> postConstructs(List<MethodMetadata> methods)
   {
      return interceptorMethodMap(POST_CONSTRUCT, methods);
   }
}
