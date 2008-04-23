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
package org.jboss.ejb3.test.tx.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.SessionContext;

import org.jboss.aop.Advisor;
import org.jboss.aop.ClassAdvisor;
import org.jboss.aop.MethodInfo;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.ConstructionInvocation;
import org.jboss.aop.util.MethodHashing;
import org.jboss.ejb3.cache.Cache;
import org.jboss.ejb3.cache.StatefulObjectFactory;
import org.jboss.ejb3.cache.impl.SimpleCache;
import org.jboss.ejb3.interceptors.InterceptorFactory;
import org.jboss.ejb3.interceptors.InterceptorFactoryRef;
import org.jboss.ejb3.interceptors.aop.LifecycleCallbacks;
import org.jboss.ejb3.interceptors.container.AbstractContainer;
import org.jboss.ejb3.interceptors.container.BeanContext;
import org.jboss.ejb3.interceptors.container.BeanContextFactory;
import org.jboss.ejb3.interceptors.container.ContainerMethodInvocation;
import org.jboss.ejb3.interceptors.container.DestructionInvocation;
import org.jboss.ejb3.interceptors.lang.ClassHelper;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class StatefulContainer<T> extends AbstractContainer<T, StatefulContainer<T>>
{
   private static final Logger log = Logger.getLogger(StatefulContainer.class);
   
   private BeanContextFactory<T, StatefulContainer<T>> beanContextFactory = new BeanContextFactory<T, StatefulContainer<T>>()
   {

      public BeanContext<T> createBean() throws Exception
      {
         try
         {
            ClassAdvisor advisor = getAdvisor();
            InterceptorFactoryRef interceptorFactoryRef = (InterceptorFactoryRef) advisor.resolveAnnotation(InterceptorFactoryRef.class);
            if(interceptorFactoryRef == null)
               throw new IllegalStateException("No InterceptorFactory specified on " + advisor.getName());
            //log.debug("interceptor factory class = " + interceptorFactoryRef.value());
            InterceptorFactory interceptorFactory = interceptorFactoryRef.value().newInstance();
            
            List<Object> ejb3Interceptors = new ArrayList<Object>();
            for(Class<?> interceptorClass : getInterceptorRegistry().getInterceptorClasses())
            {
               Object interceptor = interceptorFactory.create(advisor, interceptorClass);
               ejb3Interceptors.add(interceptor);
            }
            
            Constructor<T> constructor = advisor.getClazz().getConstructor();
            int idx = advisor.getConstructorIndex(constructor);
            Object initargs[] = null;
            T targetObject = getBeanClass().cast(advisor.invokeNew(initargs, idx));
            
            StatefulBeanContext<T> component = new StatefulBeanContext<T>(targetObject, ejb3Interceptors);
            
            // Do injection (sort of)
            try
            {
               Method method = getBeanClass().getMethod("setSessionContext", SessionContext.class);
               SessionContext sessionContext = component.getSessionContext();
               method.invoke(targetObject, sessionContext);
            }
            catch(NoSuchMethodException e)
            {
               log.debug("no method found setSessionContext");
            }
            
            // Do lifecycle callbacks
            Interceptor interceptors[] = createLifecycleInterceptors(component, PostConstruct.class);
            
            ConstructionInvocation invocation = new ConstructionInvocation(interceptors, constructor, initargs);
            invocation.setAdvisor(advisor);
            invocation.setTargetObject(targetObject);
            invocation.invokeNext();
            
            return component;
         }
         catch(Error e)
         {
            throw e;
         }
         catch(Throwable t)
         {
            // TODO: decompose
            throw new RuntimeException(t);
         }
      }

      private Interceptor[] createLifecycleInterceptors(BeanContext<T> component, Class<? extends Annotation> lifecycleAnnotationType) throws Exception
      {
         List<Class<?>> lifecycleInterceptorClasses = getInterceptorRegistry().getLifecycleInterceptorClasses();
         Advisor advisor = getAdvisor();
         return LifecycleCallbacks.createLifecycleCallbackInterceptors(advisor, lifecycleInterceptorClasses, component, lifecycleAnnotationType);
      }
      
      public void destroyBean(BeanContext<T> component)
      {
         try
         {
            Advisor advisor = getAdvisor();
            Interceptor interceptors[] = createLifecycleInterceptors(component, PreDestroy.class);
            
            DestructionInvocation invocation = new DestructionInvocation(interceptors);
            invocation.setAdvisor(advisor);
            invocation.setTargetObject(component.getInstance());
            invocation.invokeNext();
         }
         catch(Throwable t)
         {
            // TODO: disect
            if(t instanceof RuntimeException)
               throw (RuntimeException) t;
            throw new RuntimeException(t);
         }
      }

      public void setContainer(StatefulContainer<T> container)
      {
         // Dummy
      }
   };
   
   private StatefulObjectFactory<StatefulBeanContext<T>> factory = new StatefulObjectFactory<StatefulBeanContext<T>>()
   {
      public StatefulBeanContext<T> create(Class<?>[] initTypes, Object[] initValues)
      {
         try
         {
            Constructor<? extends T> constructor = getBeanClass().getConstructor();
            return (StatefulBeanContext<T>) StatefulContainer.this.construct(constructor);
         }
         catch(NoSuchMethodException e)
         {
            throw new RuntimeException(e);
         }
      }

      public void destroy(StatefulBeanContext<T> obj)
      {
         StatefulContainer.this.destroy(obj);
      }  
   };
   
   private Cache<StatefulBeanContext<T>> cache = new SimpleCache<StatefulBeanContext<T>>(factory);
   
   private class ProxyInvocationHandler implements InvocationHandler
   {
      private Object id;
      
      public ProxyInvocationHandler(Object id)
      {
         assert id != null : "id is null";
         
         this.id = id;
      }
      
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
      {
         return StatefulContainer.this.invoke(id, method, args);
      }
   }
   
   public StatefulContainer(String name, String domainName, Class<T> beanClass)
   {
      super(name, domainName, beanClass);
      setBeanContextFactory(beanContextFactory);
   }
   
   /**
    * For direct access.
    * @return
    * @throws Throwable
    */
   public Object construct() throws Throwable
   {
      StatefulBeanContext<T> ctx = cache.create(null, null);
      Object id = ctx.getId();
      cache.release(ctx);
      return id;
   }
   
   /**
    * For proxied access.
    * @param <I>
    * @param intf
    * @return
    * @throws Throwable
    */
   public <I> I constructProxy(Class<I> intf) throws Throwable
   {
      Object id = construct();
      
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class<?> interfaces[] = { intf };
      Object proxy = Proxy.newProxyInstance(loader, interfaces, new ProxyInvocationHandler(id));
      return intf.cast(proxy);
   }
   
   public void destroy(Object id)
   {
      cache.remove(id);
   }
   
   protected Cache<StatefulBeanContext<T>> getCache()
   {
      return cache;
   }
   
   /**
    * A convenient, but unchecked and slow method to call a method upon a target.
    * 
    * (Slow method)
    * 
    * @param <R>        the return type
    * @param target     the target to invoke upon
    * @param methodName the method name to invoke
    * @param args       the arguments to the method
    * @return           the return value
    * @throws Throwable if anything goes wrong
    */
   @SuppressWarnings("unchecked")
   public <R> R invoke(Object id, String methodName, Object ... args) throws Throwable
   {
      Method method = ClassHelper.getMethod(getBeanClass(), methodName);
      return (R) invoke(id, method, args);
   }
   
   public Object invoke(Object id, Method method, Object[] arguments) throws Throwable
   {
      long methodHash = MethodHashing.calculateHash(method);
      MethodInfo info = getAdvisor().getMethodInfo(methodHash);
      if(info == null)
         throw new IllegalArgumentException("method " + method + " is not under advisement by " + this);
      ContainerMethodInvocation invocation = new StatefulContainerMethodInvocation(info, id, arguments);
      return invocation.invokeNext();
   }
   
   protected void setBeanContextFactory(BeanContextFactory<T, StatefulContainer<T>> factory)
   {
      try
      {
         Field field = AbstractContainer.class.getDeclaredField("beanContextFactory");
         field.setAccessible(true);
         field.set(this, factory);
      }
      catch (SecurityException e)
      {
         throw new RuntimeException(e);
      }
      catch (NoSuchFieldException e)
      {
         throw new RuntimeException(e);
      }
      catch (IllegalArgumentException e)
      {
         throw new RuntimeException(e);
      }
      catch (IllegalAccessException e)
      {
         throw new RuntimeException(e);
      }
   }
}
