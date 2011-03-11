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
package org.jboss.ejb3.context.base.stateless;

import org.jboss.ejb3.context.spi.SessionBeanComponent;
import org.jboss.ejb3.context.spi.SessionContext;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import java.security.Principal;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatelessBeanComponent implements SessionBeanComponent
{
   public <T> T getBusinessObject(SessionContext ctx, Class<T> businessInterface)
   {
      return businessInterface.cast(ctx.getTarget());
   }

   public EJBHome getEJBHome()
   {
      throw new IllegalStateException("Bean does not define a remote home");
   }

   public EJBLocalHome getEJBLocalHome()
   {
      throw new IllegalStateException("Bean does not define a local home");
   }

   public EJBLocalObject getEJBLocalObject(SessionContext ctx)
   {
      throw new IllegalStateException("Bean does not define a local interface");
   }

   public EJBObject getEJBObject(SessionContext ctx)
   {
      throw new IllegalStateException("Bean does not define a remote interface");
   }

   public boolean getRollbackOnly()
   {
      // FIXME
      throw new IllegalStateException("FIXME");
   }

   public TimerService getTimerService()
   {
      throw new RuntimeException("NYI");
   }

   public UserTransaction getUserTransaction()
   {
      throw new RuntimeException("NYI");
   }

   public boolean isCallerInRole(Principal callerPrincipal, String roleName)
   {
      throw new RuntimeException("NYI");
   }

   public Object lookup(String name) throws IllegalArgumentException
   {
      return null;
   }

   public void setRollbackOnly()
   {
      // FIXME
      throw new IllegalStateException("FIXME");
   }
}
