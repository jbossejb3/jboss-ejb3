/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.ejb3.context.naming;

import org.jboss.ejb3.context.CurrentInvocationContext;
import org.jboss.ejb3.context.base.BaseEJBContext;
import org.jboss.ejb3.context.base.BaseInvocationContext;
import org.jboss.ejb3.context.spi.EJBContext;
import org.jboss.ejb3.context.spi.InvocationContext;
import org.jnp.server.SingletonNamingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SimpleUnitTestCase
{
   private static SingletonNamingServer server;
   private static EJBContextBinder binder;
   private static InitialContext ctx;

   @AfterClass
   public static void afterClass() throws NamingException
   {
      if(ctx != null)
         ctx.close();
      ctx = null;
      
      if(binder != null)
         binder.stop();
      binder = null;

      if(server != null)
         server.destroy();
      server = null;
   }

   @BeforeClass
   public static void beforeClass() throws NamingException
   {
      new SingletonNamingServer();

      EJBContextBinder binder = new EJBContextBinder();
      binder.start();

      ctx = new InitialContext();
   }

   @Test
   public void test1() throws Throwable
   {
      try
      {
         try
         {
            ctx.lookup("java:internal/EJBContext");
         }
         catch(NamingException e)
         {
            throw e.getCause();
         }
         fail("Should throw IllegalStateException");
      }
      catch(IllegalStateException e)
      {
         // good
      }
   }

   @Test
   public void test2() throws NamingException
   {
      InvocationContext invocation = new BaseInvocationContext(null, null) {
         @Override
         public Object proceed() throws Exception
         {
            throw new UnsupportedOperationException();
         }
      };
      EJBContext expected = new BaseEJBContext(null, null);
      invocation.setEJBContext(expected);
      CurrentInvocationContext.push(invocation);
      try
      {
         EJBContext result = (EJBContext) ctx.lookup("java:internal/EJBContext");
         assertSame(expected, result);
      }
      finally
      {
         CurrentInvocationContext.pop();
      }
   }
}
