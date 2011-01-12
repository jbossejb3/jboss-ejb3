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
package org.jboss.ejb3.servitor.stateless.simple;

import org.jboss.interceptor.proxy.InterceptorInvocation;
import org.jboss.interceptor.proxy.SimpleInterceptionChain;
import org.jboss.interceptor.spi.context.InvocationContextFactory;
import org.jboss.interceptor.spi.instance.InterceptorInstantiator;
import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.model.InterceptionModel;
import org.jboss.interceptor.spi.model.InterceptionType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class InterceptorInvocationHandler<T> implements InvocationHandler
{
   private T targetInstance;
   private InterceptorMetadata<ClassMetadata<T>> targetClassInterceptorMetadata;
   private InterceptionModel<ClassMetadata<T>, ?> interceptionModel;
   private Map<InterceptorMetadata<?>, Object> interceptorHandlerInstances = new HashMap<InterceptorMetadata<?>, Object>();
   private InvocationContextFactory invocationContextFactory;

   protected InterceptorInvocationHandler(T targetInstance, InterceptorMetadata<ClassMetadata<T>> targetClassInterceptorMetadata, InterceptionModel<ClassMetadata<T>, ?> interceptionModel, InterceptorInstantiator<?,?> interceptorInstantiator, InvocationContextFactory invocationContextFactory)
   {
      this.targetInstance = targetInstance;
      this.targetClassInterceptorMetadata = targetClassInterceptorMetadata;
      this.interceptionModel = interceptionModel;
      this.invocationContextFactory = invocationContextFactory;

      for (InterceptorMetadata interceptorMetadata : this.interceptionModel.getAllInterceptors())
      {
         interceptorHandlerInstances.put(interceptorMetadata, interceptorInstantiator.createFor(interceptorMetadata.getInterceptorReference()));
      }      
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      InterceptionType interceptionType = InterceptionType.AROUND_INVOKE;
      List<? extends InterceptorMetadata<?>> interceptorList = interceptionModel.getInterceptors(interceptionType, method);
      Collection<InterceptorInvocation<?>> interceptorInvocations = new ArrayList<InterceptorInvocation<?>>();
      for (InterceptorMetadata interceptorReference : interceptorList)
      {
         interceptorInvocations.add(new InterceptorInvocation(interceptorHandlerInstances.get(interceptorReference), interceptorReference, interceptionType));
      }
      if (targetClassInterceptorMetadata != null && targetClassInterceptorMetadata.getInterceptorMethods(interceptionType) != null && !targetClassInterceptorMetadata.getInterceptorMethods(interceptionType).isEmpty())
      {
         interceptorInvocations.add(new InterceptorInvocation(targetInstance, targetClassInterceptorMetadata, interceptionType));
      }
      SimpleInterceptionChain chain = new SimpleInterceptionChain(interceptorInvocations, interceptionType, targetInstance, method);
      return chain.invokeNextInterceptor(invocationContextFactory.newInvocationContext(chain, targetInstance, method, args));
   }
}
