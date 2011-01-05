/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.ejb3.context.base.ejbthree2204;

import org.jboss.ejb3.context.base.BaseInvocationContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class IllegalArgumentTestCase
{
   private static Class<?> addParameterTypes[] = { Number.class, Number.class };
   private static Method addMethod;

   private Object target;
   private InvocationContext invocationContext;

   private static Object[] array(Object... a)
   {
      return a;
   }

   @Before
   public void before()
   {
      this.target = new CalculatorBean();
      Object parameters[] = { 1, 2 };
      this.invocationContext = new BaseInvocationContext(addMethod, parameters)
      {
         @Override
         public Object proceed() throws Exception
         {
            return getMethod().invoke(target, getParameters());
         }
      };
   }

   @BeforeClass
   public static void beforeClass() throws Exception
   {
      addMethod = CalculatorBean.class.getMethod("add", addParameterTypes);
   }

   @Test
   public void testBadArguments() throws Exception
   {
      try
      {
         invocationContext.setParameters(array("a", "b"));
         fail("Expected IllegalArgumentException");
      }
      catch(IllegalArgumentException e)
      {
         // good
      }
   }

   @Test
   public void testGoodArguments() throws Exception
   {
      invocationContext.setParameters(array(2, 3));
      int result = (Integer) invocationContext.proceed();
      assertEquals(5, result);
   }

   @Test
   public void testNoneAllowed() throws Exception
   {
      final Object target = new CalculatorBean();
      Method method = CalculatorBean.class.getMethod("something");
      Object parameters[] = { };
      InvocationContext invocationContext = new BaseInvocationContext(method, parameters)
      {
         @Override
         public Object proceed() throws Exception
         {
            return getMethod().invoke(target, getParameters());
         }
      };
      try
      {
         invocationContext.setParameters(array("bad"));
         fail("Expected IllegalArgumentException");
      }
      catch(IllegalArgumentException e)
      {
         // good
      }
   }

   @Test
   public void testNull() throws Exception
   {
      try
      {
         invocationContext.setParameters(null);
         fail("Expected IllegalArgumentException");
      }
      catch(IllegalArgumentException e)
      {
         // good
      }
   }

   @Test
   public void testNullArguments() throws Exception
   {
      invocationContext.setParameters(array(null, null));
      // Do not actually call proceed, it'll fail.
      // If we come here without encountering NPE it's good.
   }

   @Test
   public void testNullGood() throws Exception
   {
      // I'm not 100% possitive this allowed by spec
      final Object target = new CalculatorBean();
      Method method = CalculatorBean.class.getMethod("something");
      Object parameters[] = { };
      InvocationContext invocationContext = new BaseInvocationContext(method, parameters)
      {
         @Override
         public Object proceed() throws Exception
         {
            return getMethod().invoke(target, getParameters());
         }
      };
      invocationContext.setParameters(null);
   }

   @Test
   public void testPrimitive() throws Exception
   {
      final Object target = new CalculatorBean();
      Method method = CalculatorBean.class.getMethod("mult", Integer.TYPE, Integer.TYPE);
      Object parameters[] = { };
      InvocationContext invocationContext = new BaseInvocationContext(method, parameters)
      {
         @Override
         public Object proceed() throws Exception
         {
            return getMethod().invoke(target, getParameters());
         }
      };
      invocationContext.setParameters(array(2, 3));
      int result = (Integer) invocationContext.proceed();
      assertEquals(6, result);
   }

   @Test
   public void testTooLittleArguments() throws Exception
   {
      try
      {
         invocationContext.setParameters(array(1));
         fail("Expected IllegalArgumentException");
      }
      catch(IllegalArgumentException e)
      {
         // good
      }
   }

   @Test
   public void testTooManyArguments() throws Exception
   {
      try
      {
         invocationContext.setParameters(array(1, 2, 3));
         fail("Expected IllegalArgumentException");
      }
      catch(IllegalArgumentException e)
      {
         // good
      }
   }
}
