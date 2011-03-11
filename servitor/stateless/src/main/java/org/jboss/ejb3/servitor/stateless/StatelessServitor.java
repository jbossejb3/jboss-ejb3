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
package org.jboss.ejb3.servitor.stateless;

import org.jboss.ejb3.context.spi.EJBContext;
import org.jboss.ejb3.context.spi.SessionBeanComponent;
import org.jboss.ejb3.context.spi.SessionContext;
import org.jboss.ejb3.effigy.SessionBeanEffigy;
import org.jboss.ejb3.endpoint.Endpoint;
import org.jboss.ejb3.endpoint.SessionFactory;
import org.jboss.ejb3.interceptors.container.AbstractContainer;
import org.jboss.ejb3.interceptors.container.BeanContext;
import org.jboss.ejb3.servitor.common.EnterpriseBeanServitor;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * StatelessServitor services stateless session beans via an endpoint.
 *
 * Stateless session beans are session beans whose instances have no conversational state. This means that
 * all bean instances are equivalent when they are not involved in servicing a client-invoked method.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatelessServitor extends EnterpriseBeanServitor
        implements Endpoint, SessionBeanComponent
{
   private SessionBeanEffigy sessionBeanEffigy;

   private AbstractContainer interceptorContainer;
   private Pool<? extends EJBContext> pool;

   public StatelessServitor(SessionBeanEffigy sessionBeanEffigy)
   {
      this.sessionBeanEffigy = sessionBeanEffigy;

      this.interceptorContainer = new AbstractContainer(sessionBeanEffigy);
   }

   public <T> T getBusinessObject(SessionContext ctx, Class<T> businessInterface) throws IllegalStateException
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.servitor.stateless.StatelessServitor.getBusinessObject");
   }

   public Class<?> getComponentClass()
   {
      return sessionBeanEffigy.getEjbClass();
   }

   public EJBLocalObject getEJBLocalObject(SessionContext ctx) throws IllegalStateException
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.servitor.stateless.StatelessServitor.getEJBLocalObject");
   }

   public EJBObject getEJBObject(SessionContext ctx) throws IllegalStateException
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.servitor.stateless.StatelessServitor.getEJBObject");
   }

   protected Pool<? extends EJBContext> getPool()
   {
      return pool;
   }

   public SessionFactory getSessionFactory() throws IllegalStateException
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.servitor.stateless.StatelessServitor.getSessionFactory");
   }

   public Object invoke(Serializable session, Map<String, Object> contextData, Class<?> invokedBusinessInterface, Method method, Object... args)
           throws Throwable
   {
      return interceptorContainer.invoke((BeanContext) session, contextData, method, args);
   }

   protected boolean isApplicationException(Exception ex, Method method)
   {
      // TODO: implement
      return false;
   }

   public boolean isSessionAware()
   {
      return false;
   }

   public void setPool(Pool<? extends EJBContext> pool)
   {
      this.pool = pool;
   }
}
