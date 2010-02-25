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
package org.jboss.ejb3.context.base.passivation.unit;

import org.jboss.ejb3.context.CurrentEJBContext;
import org.jboss.ejb3.context.CurrentInvocationContext;
import org.jboss.ejb3.context.base.BaseSessionContext;
import org.jboss.ejb3.context.base.BaseSessionInvocationContext;
import org.jboss.ejb3.context.base.passivation.StatefulGreeterBean;
import org.jboss.ejb3.context.base.stateless.StatelessBeanManager;
import org.jboss.ejb3.context.spi.SessionBeanManager;
import org.jboss.ejb3.context.spi.SessionContext;
import org.junit.Test;

import javax.ejb.SessionBean;
import java.rmi.MarshalledObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulUnitTestCase
{
   @Test
   public void testPassivation() throws Exception
   {
      final StatefulGreeterBean bean = new StatefulGreeterBean();
      final SessionBeanManager manager = new StatelessBeanManager();
      final BaseSessionContext context = new BaseSessionContext(manager, bean);
      BaseSessionInvocationContext invocation = new BaseSessionInvocationContext(null, null, null) {
         public Object proceed()
         {
            // lookup
            SessionContext ctx = CurrentEJBContext.get(SessionContext.class);
            bean.setSessionContext(ctx);
            return null;
         }
      };
      CurrentInvocationContext.push(invocation);
      try
      {
         invocation.setEJBContext(context);
         
         invocation.proceed();
      }
      finally
      {
         CurrentInvocationContext.pop();
      }
      // should be a method invocation
      bean.setName("testPassivation");

      // should be a passivate invocation
      final MarshalledObject<StatefulGreeterBean> mo = new MarshalledObject<StatefulGreeterBean>(bean);

      StatefulGreeterBean bean2;
      BaseSessionInvocationContext activateInvocation = new BaseSessionInvocationContext(null, null, null) {
         @Override
         public SessionBeanManager getManager()
         {
            return manager;
         }

         @Override
         public Object proceed() throws Exception
         {
            SessionBean bean = mo.get();
            bean.ejbActivate();
            return bean;
         }
      };
      CurrentInvocationContext.push(activateInvocation);
      try
      {
         bean2 = (StatefulGreeterBean) activateInvocation.proceed();
      }
      finally
      {
         CurrentInvocationContext.pop();
      }

      assertNotSame(bean, bean2);
      String result = bean2.sayHi();
      assertEquals("Hi testPassivation", result);
   }
}
