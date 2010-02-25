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
package org.jboss.ejb3.context.base;

import org.jboss.ejb3.context.CurrentInvocationContext;
import org.jboss.ejb3.context.spi.SessionBeanManager;
import org.jboss.ejb3.context.spi.SessionContext;
import org.jboss.ejb3.context.spi.SessionInvocationContext;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.xml.rpc.handler.MessageContext;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class BaseSessionContext extends BaseEJBContext
   implements SessionContext
{
   public BaseSessionContext(SessionBeanManager manager, Object instance)
   {
      super(manager, instance);
   }

   public <T> T getBusinessObject(Class<T> businessInterface) throws IllegalStateException
   {
      // to allow override per invocation
      return getCurrentInvocationContext().getBusinessObject(businessInterface);
   }

   protected SessionInvocationContext getCurrentInvocationContext()
   {
      SessionInvocationContext current = CurrentInvocationContext.get(SessionInvocationContext.class);
      assert current.getEJBContext() == this;
      return current;
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

   public Class getInvokedBusinessInterface() throws IllegalStateException
   {
      return getCurrentInvocationContext().getInvokedBusinessInterface();
   }

   public SessionBeanManager getManager()
   {
      return (SessionBeanManager) super.getManager();
   }
   
   public MessageContext getMessageContext() throws IllegalStateException
   {
      return getCurrentInvocationContext().getMessageContext();
   }
}
