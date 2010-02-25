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
import java.lang.reflect.Method;
import java.security.Identity;
import java.security.Principal;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SessionInvocation implements SessionInvocationContext
{
   private SessionContext instanceContext;
   private Principal callerPrincipal;
   private Class<?> invokedBusinessInterface;
   private MessageContext messageContext;

   public SessionContext getEJBContext()
   {
      if(instanceContext == null)
         throw new IllegalStateException("No instance associated with invocation " + this);
      return instanceContext;
   }

   public <T> T getBusinessObject(Class<T> businessInterface) throws IllegalStateException
   {
      // we need an instance attached
      SessionContext ctx = getEJBContext();
      return ctx.getManager().getBusinessObject(ctx, businessInterface);      
   }

   // redundant
   @SuppressWarnings({"deprecation"})
   public Identity getCallerIdentity()
   {
      throw new UnsupportedOperationException("getCallerIdentity is deprecated");
   }

   public Principal getCallerPrincipal()
   {
      if(callerPrincipal == null)
         throw new IllegalStateException("No callerPrincipal set on " + this);
      return callerPrincipal;
   }

   public Map<String, Object> getContextData()
   {
      throw new RuntimeException("NYI");
   }

   // redundant
   public EJBHome getEJBHome()
   {
      return getManager().getEJBHome();
   }

   // redundant
   public EJBLocalHome getEJBLocalHome()
   {
      return getManager().getEJBLocalHome();
   }

   public EJBLocalObject getEJBLocalObject() throws IllegalStateException
   {
      SessionContext ctx = getEJBContext();
      return ctx.getManager().getEJBLocalObject(ctx);
   }

   public EJBObject getEJBObject() throws IllegalStateException
   {
      SessionContext ctx = getEJBContext();
      return ctx.getManager().getEJBObject(ctx);
   }

   // redundant
   public Properties getEnvironment()
   {
      throw new UnsupportedOperationException("getCallerIdentity is deprecated");
   }

   public Class<?> getInvokedBusinessInterface() throws IllegalStateException
   {
      if(invokedBusinessInterface == null)
         throw new IllegalStateException("No invoked business interface on " + this);
      return invokedBusinessInterface;
   }

   public SessionBeanManager getManager()
   {
      // for now
      return getEJBContext().getManager();
   }

   public MessageContext getMessageContext() throws IllegalStateException
   {
      if(messageContext == null)
         throw new IllegalStateException("No message context on " + this);
      return messageContext;
   }

   public Method getMethod()
   {
      throw new RuntimeException("NYI");
   }

   public Object[] getParameters()
   {
      throw new RuntimeException("NYI");
   }

   public boolean getRollbackOnly()
   {
      return getManager().getRollbackOnly();
   }

   public Object getTarget()
   {
      return getEJBContext().getTarget();
   }

   // redundant
   public TimerService getTimerService()
   {
      return getManager().getTimerService();
   }

   public UserTransaction getUserTransaction()
   {
      return getManager().getUserTransaction();
   }

   // redundant
   @SuppressWarnings({"deprecation"})
   public boolean isCallerInRole(Identity role)
   {
      throw new IllegalStateException("deprecated");
   }

   public boolean isCallerInRole(String roleName)
   {
      // TODO: really?
      return getManager().isCallerInRole(getCallerPrincipal(), roleName);
   }

   // redundant
   public Object lookup(String name) throws IllegalArgumentException
   {
      throw new RuntimeException("NYI");
   }

   public Object proceed() throws Exception
   {
      throw new RuntimeException("NYI");
   }

   public void setCallerPrincipal(Principal callerPrincipal)
   {
      // FIXME: security check
      this.callerPrincipal = callerPrincipal;
   }

   public void setEJBContext(EJBContext instanceContext)
   {
      this.instanceContext = SessionContext.class.cast(instanceContext);
   }

   public void setMessageContext(MessageContext messageContext)
   {
      this.messageContext = messageContext;
   }
   
   public void setParameters(Object[] params)
   {
      throw new RuntimeException("NYI");
   }

   public void setRollbackOnly()
   {
      getManager().setRollbackOnly();
   }
}
