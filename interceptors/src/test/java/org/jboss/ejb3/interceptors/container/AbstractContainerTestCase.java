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

import org.jboss.interceptor.builder.InterceptionModelBuilder;
import org.jboss.interceptor.proxy.DefaultInvocationContextFactory;
import org.jboss.interceptor.proxy.DirectClassInterceptorInstantiator;
import org.jboss.interceptor.reader.InterceptorMetadataUtils;
import org.jboss.interceptor.reader.cache.DefaultMetadataCachingReader;
import org.jboss.interceptor.reader.cache.MetadataCachingReader;
import org.jboss.interceptor.spi.context.InvocationContextFactory;
import org.jboss.interceptor.spi.instance.InterceptorInstantiator;
import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.model.InterceptionModel;
import org.junit.Before;
import org.junit.Test;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;

import static org.jboss.ejb3.interceptors.dsl.InterceptorMetadataFactory.aroundInvokes;
import static org.jboss.ejb3.interceptors.dsl.InterceptorMetadataFactory.interceptor;
import static org.jboss.ejb3.interceptors.dsl.InterceptorMetadataFactory.map;
import static org.jboss.ejb3.interceptors.dsl.InterceptorMetadataFactory.postConstructs;
import static org.jboss.ejb3.interceptors.dsl.InterceptorReferenceFactory.interceptorReference;
import static org.jboss.ejb3.interceptors.dsl.MethodMetadataFactory.methods;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class AbstractContainerTestCase
{
   private MetadataCachingReader metadataCachingReader = new DefaultMetadataCachingReader();

   @Before
   public void before()
   {
      SimpleInterceptor.postConstructs = 0;
   }
   
   @Test
   public void testInterception() throws Throwable
   {
      Class<?> targetClass = SimpleBean.class;

      InterceptorInstantiator<?,?> interceptorInstantiator = new DirectClassInterceptorInstantiator();
      
      InvocationContextFactory invocationContextFactory = new DefaultInvocationContextFactory();

      ClassMetadata<?> targetClassMetadata =  metadataCachingReader.getClassMetadata(targetClass);
      // TODO: wrong, should really create the metadata myself, not generate reflective metadata
      InterceptorMetadata<ClassMetadata<?>> targetClassInterceptorMetadata = InterceptorMetadataUtils.readMetadataForTargetClass(targetClassMetadata);
      InterceptionModelBuilder<ClassMetadata<?>,?> builder = InterceptionModelBuilder.<ClassMetadata<?>>newBuilderFor(targetClassMetadata);
      InterceptorMetadata<?> interceptor = interceptor(
         interceptorReference(metadataCachingReader.getClassMetadata(SimpleInterceptor.class)),
         map(
            aroundInvokes(methods(SimpleInterceptor.class.getMethod("aroundInvoke", InvocationContext.class))),
            postConstructs(methods(SimpleInterceptor.class.getMethod("postConstruct", InvocationContext.class)))
         )
      );
      builder.interceptAll().with(interceptor);
      InterceptionModel<ClassMetadata<?>,?> interceptionModel = builder.build();

      AbstractContainer container = new AbstractContainer(targetClassInterceptorMetadata, interceptionModel, interceptorInstantiator, invocationContextFactory);

      BeanContext instance = container.construct();

      assertEquals(1, SimpleInterceptor.postConstructs);

      Method method = SimpleBean.class.getMethod("sayHi", String.class);
      Object result = container.invoke(instance, method, "test");

      assertEquals("Intercepted Hi test", result);
   }

   public static void testContainer(AbstractContainer container) throws Exception
   {
      BeanContext instance = container.construct();

      assertEquals(1, SimpleInterceptor.postConstructs);

      Method method = SimpleBean.class.getMethod("sayHi", String.class);
      Object result = container.invoke(instance, method, "test");

      assertEquals("Intercepted Hi test", result);      
   }
}
