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
package org.jboss.ejb3.context.base.stateless.unit;

import org.jboss.ejb3.context.CurrentEJBContext;
import org.jboss.ejb3.context.CurrentInvocationContext;
import org.jboss.ejb3.context.base.BaseSessionContext;
import org.jboss.ejb3.context.base.BaseSessionInvocationContext;
import org.jboss.ejb3.context.base.stateless.GreeterBean;
import org.jboss.ejb3.context.base.stateless.StatelessBeanManager;
import org.jboss.ejb3.context.spi.SessionBeanManager;
import org.jboss.ejb3.context.spi.SessionContext;
import org.junit.Test;

import javax.ejb.TimerService;
import javax.transaction.UserTransaction;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatelessUnitTestCase
{
   @Test
   public void test1() throws Exception
   {
      final GreeterBean bean = new GreeterBean();
      SessionBeanManager manager = new StatelessBeanManager();
      final BaseSessionContext context = new BaseSessionContext(manager, bean);
      BaseSessionInvocationContext invocation = new BaseSessionInvocationContext(null, null, null) {
         @Override
         public <T> T getBusinessObject(Class<T> businessInterface) throws IllegalStateException
         {
            throw new IllegalStateException("getBusinessObject not allowed during injection (EJB 3.0 FR 4.5.2)");
         }

         @Override
         public TimerService getTimerService()
         {
            throw new IllegalStateException("getTimerService not allowed during injection (EJB 3.0 FR 4.5.2)");
         }

         @Override
         public UserTransaction getUserTransaction()
         {
            throw new IllegalStateException("getUserTransaction not allowed during injection (EJB 3.0 FR 4.5.2)");
         }

         public Object proceed()
         {
            //GreeterBean target = (GreeterBean) getTarget();
            //target.setSessionContext(CurrentEJBContext.get(SessionContext.class));
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
   }
}
