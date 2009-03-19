/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.endpoint.test.equality.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

import javassist.util.proxy.ProxyFactory;

import org.jboss.ejb3.endpoint.Endpoint;
import org.jboss.ejb3.endpoint.reflect.EndpointInvocationHandler;
import org.jboss.ejb3.endpoint.reflect.EndpointProxy;
import org.jboss.ejb3.endpoint.test.javassist.MethodHandlerAdapter;
import org.junit.Test;

/**
 * Invoking equals, hashCode and toString on a proxy must not result in
 * a call to the Endpoint.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class EqualityTestCase
{
   private class SimpleEndpoint implements Endpoint
   {
      public Object invoke(Serializable session, Class<?> invokedBusinessInterface, Method method, Object[] args)
         throws Throwable
      {
         if(!method.getName().equals("sayHi"))
            fail("Invalid call to method " + method);
         return "Hi " + args[0];
      }
   }
   
   private interface SimpleInterface
   {
      String sayHi(String name);
   }
   
   private static int createHashCode(Endpoint endpoint, Serializable session, Class<?> invokedBusinessInterface)
   {
      int hashCode = endpoint.hashCode();
      if(session != null)
         hashCode += (session.hashCode() << 2);
      if(invokedBusinessInterface != null)
         hashCode += (invokedBusinessInterface.hashCode() << 4);
      return hashCode;
   }
   
   @Test
   public void testEquals() throws Exception
   {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session = null;
      Class<SimpleInterface> businessInterface = SimpleInterface.class;
      SimpleInterface proxy1 = EndpointProxy.newProxyInstance(loader, session, businessInterface, endpoint);
      SimpleInterface proxy2 = EndpointProxy.newProxyInstance(loader, session, businessInterface, endpoint);
      assertTrue(proxy1.equals(proxy2));
   }

   @Test
   public void testEqualsDifferentBusinessInterface() throws Exception
   {
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session = UUID.randomUUID();
      Class<SimpleInterface> businessInterface1 = SimpleInterface.class;
      Class<?> businessInterface2 = null;
      InvocationHandler handler1 = new EndpointInvocationHandler(endpoint, session, businessInterface1);
      InvocationHandler handler2 = new EndpointInvocationHandler(endpoint, session, businessInterface2);
      assertFalse(handler1.equals(handler2));
   }

   @Test
   public void testEqualsDifferentProxyProviders() throws Exception
   {
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session = null;
      Class<SimpleInterface> businessInterface = SimpleInterface.class;
      InvocationHandler handler = new EndpointInvocationHandler(endpoint, session, businessInterface);
      
      ProxyFactory factory = new ProxyFactory();
      factory.setHandler(new MethodHandlerAdapter(handler));
      factory.setInterfaces(new Class[] { businessInterface });
      SimpleInterface javassistProxy = (SimpleInterface) factory.create(null, null);
      
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      SimpleInterface proxy = EndpointProxy.newProxyInstance(loader, session, businessInterface, endpoint);
      
      assertTrue(javassistProxy.equals(proxy));
      assertTrue(proxy.equals(javassistProxy));
   }

   @Test
   public void testEqualsDifferentSession() throws Exception
   {
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session1 = UUID.randomUUID();
      Serializable session2 = UUID.randomUUID();
      Class<SimpleInterface> businessInterface = null;
      InvocationHandler handler1 = new EndpointInvocationHandler(endpoint, session1, businessInterface);
      InvocationHandler handler2 = new EndpointInvocationHandler(endpoint, session2, businessInterface);
      assertFalse(handler1.equals(handler2));
   }

   @Test
   public void testEqualsDummyObject() throws Exception
   {
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session = null;
      Class<SimpleInterface> businessInterface = null;
      InvocationHandler handler = new EndpointInvocationHandler(endpoint, session, businessInterface);
      assertFalse(handler.equals(new Object()));
   }

   @Test
   public void testEqualsNull() throws Exception
   {
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session = null;
      Class<SimpleInterface> businessInterface = null;
      EndpointInvocationHandler handler = new EndpointInvocationHandler(endpoint, session, businessInterface);
      assertFalse(handler.equals(null));
   }

   @Test
   public void testEqualsOtherEndpoint() throws Exception
   {
      Endpoint endpoint1 = new SimpleEndpoint();
      Endpoint endpoint2 = new SimpleEndpoint();
      Serializable session = null;
      Class<SimpleInterface> businessInterface = null;
      InvocationHandler handler1 = new EndpointInvocationHandler(endpoint1, session, businessInterface);
      InvocationHandler handler2 = new EndpointInvocationHandler(endpoint2, session, businessInterface);
      assertFalse(handler1.equals(handler2));
   }

   @Test
   public void testEqualsSame() throws Exception
   {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session = null;
      Class<SimpleInterface> businessInterface = SimpleInterface.class;
      SimpleInterface proxy = EndpointProxy.newProxyInstance(loader, session, businessInterface, endpoint);
      assertTrue(proxy.equals(proxy));
   }

   @Test
   public void testEqualsSameSession() throws Exception
   {
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session1 = UUID.randomUUID();
      Serializable session2 = session1;
      Class<SimpleInterface> businessInterface = null;
      InvocationHandler handler1 = new EndpointInvocationHandler(endpoint, session1, businessInterface);
      InvocationHandler handler2 = new EndpointInvocationHandler(endpoint, session2, businessInterface);
      assertTrue(handler1.equals(handler2));
   }

   @Test
   public void testEqualsWithAndWithoutSession() throws Exception
   {
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session1 = UUID.randomUUID();
      Serializable session2 = null;
      Class<SimpleInterface> businessInterface = null;
      InvocationHandler handler1 = new EndpointInvocationHandler(endpoint, session1, businessInterface);
      InvocationHandler handler2 = new EndpointInvocationHandler(endpoint, session2, businessInterface);
      assertFalse(handler1.equals(handler2));
   }

   @Test
   public void testEqualsWithoutAndWithSession() throws Exception
   {
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session1 = null;
      Serializable session2 = UUID.randomUUID();
      Class<SimpleInterface> businessInterface = null;
      InvocationHandler handler1 = new EndpointInvocationHandler(endpoint, session1, businessInterface);
      InvocationHandler handler2 = new EndpointInvocationHandler(endpoint, session2, businessInterface);
      assertFalse(handler1.equals(handler2));
   }

   @Test
   public void testHashCode() throws Exception
   {
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session = null;
      Class<SimpleInterface> businessInterface = null;
      InvocationHandler handler = new EndpointInvocationHandler(endpoint, session, businessInterface);
      int result = handler.hashCode();
      assertEquals(createHashCode(endpoint, session, businessInterface), result);
   }

   @Test
   public void testHashCodeWithBusinessInterface() throws Exception
   {
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session = null;
      Class<SimpleInterface> businessInterface = SimpleInterface.class;
      InvocationHandler handler = new EndpointInvocationHandler(endpoint, session, businessInterface);
      int result = handler.hashCode();
      assertEquals(createHashCode(endpoint, session, businessInterface), result);
      assertFalse(createHashCode(endpoint, session, null) == result);
   }

   @Test
   public void testHashCodeWithSession() throws Exception
   {
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session = UUID.randomUUID();
      Class<SimpleInterface> businessInterface = null;
      InvocationHandler handler = new EndpointInvocationHandler(endpoint, session, businessInterface);
      int result = handler.hashCode();
      assertEquals(createHashCode(endpoint, session, businessInterface), result);
      assertFalse(createHashCode(endpoint, null, businessInterface) == result);
   }

   @Test
   public void testHashCodeWithSessionAndBusinessInterface() throws Exception
   {
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session = UUID.randomUUID();
      Class<SimpleInterface> businessInterface = SimpleInterface.class;
      InvocationHandler handler = new EndpointInvocationHandler(endpoint, session, businessInterface);
      int result = handler.hashCode();
      assertEquals(createHashCode(endpoint, session, businessInterface), result);
      assertFalse(createHashCode(endpoint, null, businessInterface) == result);
      assertFalse(createHashCode(endpoint, session, null) == result);
      assertFalse(createHashCode(endpoint, null, null) == result);
   }

   @Test
   public void testToString() throws Exception
   {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Endpoint endpoint = new SimpleEndpoint();
      Serializable session = null;
      Class<SimpleInterface> businessInterface = SimpleInterface.class;
      SimpleInterface proxy = EndpointProxy.newProxyInstance(loader, session, businessInterface, endpoint);
      String result = proxy.toString();
      assertTrue(result.startsWith("Proxy on " + EndpointInvocationHandler.class.getName()));
   }
}
