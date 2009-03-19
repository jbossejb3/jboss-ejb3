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
package org.jboss.ejb3.endpoint.test.proxy.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.jboss.ejb3.endpoint.Endpoint;
import org.jboss.ejb3.endpoint.reflect.EndpointProxy;
import org.junit.Test;

/**
 * Test the utility class EndpointProxy.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class EndpointProxyTestCase
{
   private interface SimpleInterface
   {
      String sayHi(String name);
   }
   
   @Test
   public void test1()
   {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Endpoint endpoint = new Endpoint() {
         public Object invoke(Serializable session, Class<?> invokedBusinessInterface, Method method, Object[] args)
            throws Throwable
         {
            return "Hi " + args[0];
         }
      };
      Serializable session = null;
      Class<SimpleInterface> businessInterface = SimpleInterface.class;
      SimpleInterface proxy = EndpointProxy.newProxyInstance(loader, session, businessInterface, endpoint);
      String result = proxy.sayHi("me");
      assertEquals("Hi me", result);
   }

   
   @Test
   public void testNoBusinessInterface()
   {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Endpoint endpoint = new Endpoint() {
         public Object invoke(Serializable session, Class<?> invokedBusinessInterface, Method method, Object[] args)
            throws Throwable
         {
            return "Hi " + args[0];
         }
      };
      Serializable session = null;
      Class<?> businessInterface = null;
      try
      {
         EndpointProxy.newProxyInstance(loader, session, businessInterface, endpoint);
         fail("Should have thrown NullPointerException");
      }
      catch(NullPointerException e)
      {
         assertEquals("businessInterface is null", e.getMessage());
      }
   }
}
