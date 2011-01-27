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

import org.jboss.ejb3.effigy.SessionBeanEffigy;
import org.jboss.ejb3.effigy.dsl.SessionBeanFactory;
import org.jboss.ejb3.endpoint.Endpoint;
import org.jboss.ejb3.servitor.stateless.StatelessInstanceInterceptor;
import org.jboss.ejb3.servitor.stateless.StatelessServitor;
import org.jboss.interceptor.builder.InterceptionModelBuilder;
import org.jboss.interceptor.proxy.DefaultInvocationContextFactory;
import org.jboss.interceptor.proxy.DirectClassInterceptorInstantiator;
import org.jboss.interceptor.spi.instance.InterceptorInstantiator;
import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.model.InterceptionModel;
import org.junit.Test;

import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.ejb3.interceptors.dsl.InterceptorMetadataFactory.aroundInvokes;
import static org.jboss.ejb3.interceptors.dsl.InterceptorMetadataFactory.interceptor;
import static org.jboss.ejb3.interceptors.dsl.InterceptorReferenceFactory.interceptorReference;
import static org.jboss.ejb3.interceptors.dsl.MethodMetadataFactory.methods;
import static org.junit.Assert.assertEquals;

/**
 * The goal of this test is to provide code coverage. There is no real link to specs.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SimpleStatelessServitorTestCase
{
   @Test
   public void testSimple() throws Throwable
   {
      SessionBeanEffigy sessionBean = SessionBeanFactory.session(SimpleStatelessBean.class).effigy();

      StatelessServitor servitor = new StatelessServitor(sessionBean);

      servitor.setPool(new DummyPool<DummySessionContext>(new DummyStatelessObjectFactory(servitor)));

      ClassMetadata<StatelessServitor> targetClassMetadata = new SimpleClassMetadata<StatelessServitor>(StatelessServitor.class);
      InterceptorMetadata<ClassMetadata<StatelessServitor>> targetClassInterceptorMetadata = interceptor(
              interceptorReference(targetClassMetadata), null
      );
      InterceptionModelBuilder<ClassMetadata<StatelessServitor>, ?> builder = InterceptionModelBuilder.newBuilderFor(targetClassMetadata);
      // TODO: the interceptor should be put upon the component class, not the view class, but that doesn't work
      builder.interceptAroundInvoke(Endpoint.class.getMethod("invoke", Serializable.class, Map.class, Class.class, Method.class, Object[].class)).
              with(interceptor(interceptorReference(new SimpleClassMetadata<StatelessInstanceInterceptor>(StatelessInstanceInterceptor.class)),
                      aroundInvokes(methods(StatelessInstanceInterceptor.class.getMethod("aroundInvoke", InvocationContext.class)))));
      InterceptionModel<ClassMetadata<StatelessServitor>, ?> interceptionModel = builder.build();

      Endpoint endpoint = proxifyInstance(servitor, Endpoint.class, targetClassInterceptorMetadata, interceptionModel);

      Map<String, Object> contextData = new HashMap<String, Object>();
      String result = (String) endpoint.invoke(null, contextData, SimpleStatelessBean.class, SimpleStatelessBean.class.getMethod("sayHi", String.class), "test");

      assertEquals("Hi test", result);
   }

   private <T, I> I proxifyInstance(T instance, Class<I> intf, InterceptorMetadata<ClassMetadata<T>> targetClassInterceptorMetadata, InterceptionModel<ClassMetadata<T>, ?> interceptionModel)
           throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
   {
      DefaultInvocationContextFactory contextFactory = new DefaultInvocationContextFactory();
      InterceptorInstantiator<Object, ClassMetadata<?>> interceptorInstantiator = new DirectClassInterceptorInstantiator();
      InvocationHandler handler = new InterceptorInvocationHandler<T>(instance, targetClassInterceptorMetadata, interceptionModel, interceptorInstantiator, contextFactory);
      ClassLoader loader = intf.getClassLoader();
      Class<?> interfaces[] = new Class<?>[] { intf };
      return intf.cast(Proxy.newProxyInstance(loader, interfaces, handler));
   }
}
