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

import org.jboss.ejb3.context.CurrentInvocationContext;
import org.jboss.ejb3.context.spi.EJBContext;
import org.jboss.ejb3.context.spi.SessionBeanManager;
import org.jboss.ejb3.context.spi.SessionContext;
import org.jboss.ejb3.context.spi.SessionInvocationContext;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import javax.xml.rpc.handler.MessageContext;
import java.security.Identity;
import java.security.Principal;
import java.util.Properties;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatelessContext implements SessionContext, EJBContext
{
   private SessionBeanManager manager;
   private Object instance;

   public StatelessContext(SessionBeanManager manager, Object instance)
   {
      this.manager = manager;
      this.instance = instance;
   }

   public <T> T getBusinessObject(Class<T> businessInterface) throws IllegalStateException
   {
      // to allow override per invocation
      return getCurrentInvocationContext().getBusinessObject(businessInterface);
   }

   @SuppressWarnings({"deprecation"})
   public Identity getCallerIdentity()
   {
      throw new UnsupportedOperationException("getCallerIdentity is deprecated");
   }

   public Principal getCallerPrincipal()
   {
      // per invocation
      return getCurrentInvocationContext().getCallerPrincipal();
   }

   protected SessionInvocationContext getCurrentInvocationContext()
   {
      SessionInvocationContext current = CurrentInvocationContext.get(SessionInvocationContext.class);
      assert current.getEJBContext() == this;
      return current;
   }

   public EJBHome getEJBHome()
   {
      return manager.getEJBHome();
   }

   public EJBLocalHome getEJBLocalHome()
   {
      return manager.getEJBLocalHome();
   }

   public EJBLocalObject getEJBLocalObject() throws IllegalStateException
   {
      // to allow override per invocation
      return getCurrentInvocationContext().getEJBLocalObject();
   }

   public EJBObject getEJBObject() throws IllegalStateException
   {
      // to allow override per invocation
      return getCurrentInvocationContext().getEJBObject();
   }

   public Properties getEnvironment()
   {
      throw new UnsupportedOperationException("getCallerIdentity is deprecated");
   }

   public Class getInvokedBusinessInterface() throws IllegalStateException
   {
      return getCurrentInvocationContext().getInvokedBusinessInterface();
   }

   public SessionBeanManager getManager()
   {
      return manager;
   }
   
   public MessageContext getMessageContext() throws IllegalStateException
   {
      throw new RuntimeException("NYI");
   }

   public boolean getRollbackOnly() throws IllegalStateException
   {
      // to allow override per invocation
      return getCurrentInvocationContext().getRollbackOnly();
   }

   public Object getTarget()
   {
      return instance;
   }

   public TimerService getTimerService() throws IllegalStateException
   {
      // to allow override per invocation
      return getCurrentInvocationContext().getTimerService();
   }

   public UserTransaction getUserTransaction() throws IllegalStateException
   {
      // to allow override per invocation
      return getCurrentInvocationContext().getUserTransaction();
   }

   @SuppressWarnings({"deprecation"})
   public boolean isCallerInRole(Identity role)
   {
      throw new IllegalStateException("deprecated");
   }

   public boolean isCallerInRole(String roleName)
   {
      return getCurrentInvocationContext().isCallerInRole(roleName);
   }

   public Object lookup(String name) throws IllegalArgumentException
   {
      return getManager().lookup(name);
   }

   public void setRollbackOnly() throws IllegalStateException
   {
      // to allow override per invocation
      getCurrentInvocationContext().setRollbackOnly();
   }
}
