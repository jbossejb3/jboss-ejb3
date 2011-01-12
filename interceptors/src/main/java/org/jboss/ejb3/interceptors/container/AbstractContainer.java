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
package org.jboss.ejb3.interceptors.container;

import org.jboss.interceptor.proxy.InterceptorInvocation;
import org.jboss.interceptor.proxy.SimpleInterceptionChain;
import org.jboss.interceptor.spi.context.InvocationContextFactory;
import org.jboss.interceptor.spi.instance.InterceptorInstantiator;
import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.model.InterceptionModel;
import org.jboss.interceptor.spi.model.InterceptionType;

import javax.ejb.EJBException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The base of all containers. Provides functions to allow for object
 * construction and invocation with interception.
 * <p/>
 * Note that it's up to the actual implementation to expose any methods.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class AbstractContainer
{
   private InterceptorInstantiator<?, ?> interceptorInstantiator;
   private InvocationContextFactory invocationContextFactory;
   private InterceptionModel<ClassMetadata<?>, ?> interceptionModel;
   private InterceptorMetadata<ClassMetadata<?>> targetClassInterceptorMetadata;

   /**
    * @param targetClassInterceptorMetadata
    * @param interceptorInstantiator
    * @param invocationContextFactory
    * @param interceptionModel
    * @deprecated exposing the usage of jboss-interceptors is a bad thing.
    */
   @Deprecated
   public AbstractContainer(InterceptorMetadata<ClassMetadata<?>> targetClassInterceptorMetadata, InterceptionModel<ClassMetadata<?>, ?> interceptionModel, InterceptorInstantiator<?,?> interceptorInstantiator, InvocationContextFactory invocationContextFactory)
   {
      this.targetClassInterceptorMetadata = targetClassInterceptorMetadata;
      this.interceptionModel = interceptionModel;
      this.interceptorInstantiator = interceptorInstantiator;
      this.invocationContextFactory = invocationContextFactory;
   }

   /**
    * Create a new bean instance according to the state diagrams from the specification.
    * 
    * 1. newInstance
    * 2. dependency injection
    * 3. PostConstruct calls, if any
    *
    * Opposed to what stateful session bean requires, this method will *not* call any init or ejbCreate methods.
    * 
    * @return
    * @throws Exception
    */
   protected BeanContext construct() throws Exception
   {
      try
      {
         // 1. newInstance
         Object instance = targetClassInterceptorMetadata.getInterceptorClass().getJavaClass().newInstance();
         Map<Class<?>, Object> interceptorHandlerInstances = new HashMap<Class<?>, Object>();
         for (InterceptorMetadata interceptorMetadata : this.interceptionModel.getAllInterceptors())
         {
            interceptorHandlerInstances.put(interceptorMetadata.getInterceptorClass().getJavaClass(), interceptorInstantiator.createFor(interceptorMetadata.getInterceptorReference()));
         }
         // 2. dependency injection
         // TODO: dependency injection which is handled by BeanInstantiator or BeanContextFactory
         BeanContext bean = new DummyBeanContext(instance, interceptorHandlerInstances);
         // 3. PostConstruct calls, if any
         // TODO: delegated? for now do it myself
         executeInterception(bean, null, null, InterceptionType.POST_CONSTRUCT);

         // Step 4, which is only applicable to stateful session beans is explicitly beyond scope of this method.
         // 4. Init method, or ejbCreate<METHOD>, if any

         return bean;
      }
      catch (InstantiationException e)
      {
         throw new EJBException(e);
      }
      catch (IllegalAccessException e)
      {
         throw new EJBException(e);
      }
   }

   protected void destroy(BeanContext bean) throws Exception
   {
      // TODO: use BeanContextFactory
      // PreDestroy callbacks, if any
      executeInterception(bean, null, null, InterceptionType.PRE_DESTROY);
   }

   /**
    * @see org.jboss.interceptor.proxy.InterceptorMethodHandler#executeInterception(Object, java.lang.reflect.Method, java.lang.reflect.Method, Object[], org.jboss.interceptor.spi.model.InterceptionType)
    */
   private Object executeInterception(BeanContext bean, Method method, Object[] args, InterceptionType interceptionType) throws Exception
   {
      if(bean == null)
         throw new NullPointerException("bean instance is null");
      
      Object targetInstance = bean.getInstance();
      List<? extends InterceptorMetadata<?>> interceptorList = interceptionModel.getInterceptors(interceptionType, method);
      Collection<InterceptorInvocation<?>> interceptorInvocations = new ArrayList<InterceptorInvocation<?>>();
      if(interceptorList != null)
      {
         for (InterceptorMetadata interceptorReference : interceptorList)
         {
            // TODO: maybe store the interceptor instances on a different key
            interceptorInvocations.add(new InterceptorInvocation(bean.getInterceptor(interceptorReference.getInterceptorClass().getJavaClass()), interceptorReference, interceptionType));
         }
      }
      if (targetClassInterceptorMetadata != null && targetClassInterceptorMetadata.getInterceptorMethods(interceptionType) != null && !targetClassInterceptorMetadata.getInterceptorMethods(interceptionType).isEmpty())
      {
         interceptorInvocations.add(new InterceptorInvocation(targetInstance, targetClassInterceptorMetadata, interceptionType));
      }
      SimpleInterceptionChain chain = new SimpleInterceptionChain(interceptorInvocations, interceptionType, targetInstance, method);
      try
      {
         return chain.invokeNextInterceptor(invocationContextFactory.newInvocationContext(chain, targetInstance, method, args));
      }
      catch(Throwable t)
      {
         if(t instanceof Exception)
            throw (Exception) t;
         if(t instanceof Error)
            throw (Error) t;
         EJBException ex = new EJBException();
         ex.initCause(t);
         throw ex;
      }
   }

   /**
    * Call a method upon a target object with all interceptors in place.
    *
    * @param target     the target to invoke upon
    * @param method     the method to invoke
    * @param arguments  arguments to the method
    * @return           return value of the method
    * @throws Exception if anything goes wrong
    */
   public Object invoke(BeanContext target, Method method, Object... arguments) throws Exception
   {
      return executeInterception(target, method, arguments, InterceptionType.AROUND_INVOKE);
   }
}
