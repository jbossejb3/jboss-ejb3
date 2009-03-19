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
package org.jboss.ejb3.endpoint.test.invocation.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Date;

import org.jboss.ejb3.endpoint.Endpoint;
import org.jboss.ejb3.endpoint.reflect.EndpointInvocationHandler;
import org.junit.Test;

/**
 * Test an invocation on a dummy endpoint.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class InvocationTestCase
{
   @Test
   public void testInvocation() throws Throwable
   {
      Endpoint endpoint = new Endpoint() {
         public Object invoke(Serializable session, Class<?> invokedBusinessInterface, Method method, Object[] args)
            throws Throwable
         {
            return "Hi " + args[0];
         }
      };
      Serializable session = null;
      Class<?> invokedBusinessInterface = null;
      InvocationHandler handler = new EndpointInvocationHandler(endpoint, session, invokedBusinessInterface);
      Object proxy = null;
      // just make sure method is not null
      Method method = InvocationTestCase.class.getDeclaredMethod("testInvocation");
      Date now = new Date();
      Object args[] = { now };
      Object result = handler.invoke(proxy, method, args);
      assertEquals("Hi " + now, result);
   }
   
   @Test
   public void testIllegalEndpoint()
   {
      Endpoint endpoint = null;
      Serializable session = null;
      Class<?> invokedBusinessInterface = null;
      try
      {
         new EndpointInvocationHandler(endpoint, session, invokedBusinessInterface);
         fail("Should have thrown AssertionError (or run with -ea)");
      }
      catch(AssertionError e)
      {
         assertEquals("endpoint is null", e.getMessage());
      }
   }
}
