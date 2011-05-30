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
package org.jboss.ejb3.context.base.ejbthree2252;

import org.jboss.ejb3.context.base.BaseSessionContext;
import org.jboss.ejb3.context.base.BaseSessionInvocationContext;
import org.jboss.ejb3.context.spi.SessionBeanComponent;
import org.jboss.ejb3.context.spi.SessionContext;
import org.junit.Test;

import javax.ejb.SessionSynchronization;
import java.lang.reflect.Method;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class TransactionOperationAllowancesTestCase
{
   @Test
   public void testConstruction()
   {
      final BaseSessionInvocationContext ctx = new BaseSessionInvocationContext(Object.class, null, null)
      {
         @Override
         public Object proceed() throws Exception
         {
            throw new RuntimeException("NYI: .proceed");
         }
      };
      // there is no instance yet
      try
      {
         ctx.getRollbackOnly();
         fail("Expected IllegalStateException (EJB 3.1 FR 4.6.1, 4.7.2, 4.8.6, 5.5.1)");
      }
      catch (IllegalStateException e)
      {
         // good
      }
      try
      {
         ctx.setRollbackOnly();
         fail("Expected IllegalStateException (EJB 3.1 FR 4.6.1, 4.7.2, 4.8.6, 5.5.1)");
      }
      catch (IllegalStateException e)
      {
         // good
      }
   }

   @Test
   public void testInjection()
   {
      final SessionBeanComponent component = mock(SessionBeanComponent.class);
      final Object instance = new Object();
      SessionContext instanceCtx = new BaseSessionContext(component, instance);
      final BaseSessionInvocationContext ctx = new BaseSessionInvocationContext(Object.class, null, null)
      {
         @Override
         public Object proceed() throws Exception
         {
            throw new RuntimeException("NYI: .proceed");
         }
      };
      ctx.setEJBContext(instanceCtx);
      try
      {
         ctx.getRollbackOnly();
         fail("Expected IllegalStateException (EJB 3.1 FR 4.6.1, 4.7.2, 4.8.6, 5.5.1)");
      }
      catch (IllegalStateException e)
      {
         // good
      }
      try
      {
         ctx.setRollbackOnly();
         fail("Expected IllegalStateException (EJB 3.1 FR 4.6.1, 4.7.2, 4.8.6, 5.5.1)");
      }
      catch (IllegalStateException e)
      {
         // good
      }
      verifyNoMoreInteractions(component);
   }

   @Test
   public void testInvocation() throws Exception {
      final SessionBeanComponent component = mock(SessionBeanComponent.class);
      final Object instance = new Object();
      final SessionContext instanceCtx = new BaseSessionContext(component, instance);
      final Method method = Object.class.getMethod("toString");
      final BaseSessionInvocationContext ctx = new BaseSessionInvocationContext(Object.class, method, null)
      {
         @Override
         public Object proceed() throws Exception
         {
            throw new RuntimeException("NYI: .proceed");
         }
      };
      ctx.setEJBContext(instanceCtx);
      ctx.getRollbackOnly();
      ctx.setRollbackOnly();
      verify(component).getRollbackOnly();
      verify(component).setRollbackOnly();
      verifyNoMoreInteractions(component);
   }

   @Test
   public void testSessionSynchronization() throws Exception {
      final SessionBeanComponent component = mock(SessionBeanComponent.class);
      final Object instance = new Object();
      final SessionContext instanceCtx = new BaseSessionContext(component, instance);
      // might be any other method annotated with @AfterBegin
      final Method method = SessionSynchronization.class.getMethod("afterBegin");
      BaseSessionInvocationContext ctx = new BaseSessionInvocationContext(false, Object.class, null, null)
      {
         @Override
         public Object proceed() throws Exception
         {
            throw new RuntimeException("NYI: .proceed");
         }
      };
      ctx.setEJBContext(instanceCtx);
      ctx.getRollbackOnly();
      ctx.setRollbackOnly();
      verify(component).getRollbackOnly();
      verify(component).setRollbackOnly();
      verifyNoMoreInteractions(component);
   }
}
