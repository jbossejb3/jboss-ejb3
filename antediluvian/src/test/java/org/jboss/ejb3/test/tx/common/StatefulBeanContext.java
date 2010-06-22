/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.test.tx.common;

import java.security.Identity;
import java.security.Principal;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.SessionContext;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import javax.xml.rpc.handler.MessageContext;

import org.jboss.aop.metadata.SimpleMetaData;
import org.jboss.ejb3.cache.Identifiable;
import org.jboss.ejb3.interceptors.container.DummyBeanContext;
import org.jboss.ejb3.tx.TxUtil;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class StatefulBeanContext<T> extends DummyBeanContext<T>
   implements Identifiable, org.jboss.ejb3.tx.container.StatefulBeanContext<T>
{
   private StatefulContainer<T> container;
   private Object id = UUID.randomUUID();
   private SimpleMetaData metaData = new SimpleMetaData();
   
   /**
    * Is this bean taking part of transaction synchronization?
    */
   private boolean txSynchronized = false;
   
   private SessionContext sessionContext = new SessionContext()
   {
      public <T> T getBusinessObject(Class<T> businessInterface) throws IllegalStateException
      {
         throw new RuntimeException("N/A");
      }

      public EJBLocalObject getEJBLocalObject() throws IllegalStateException
      {
         throw new RuntimeException("N/A");
      }

      public EJBObject getEJBObject() throws IllegalStateException
      {
         throw new RuntimeException("N/A");
      }

      public Class getInvokedBusinessInterface() throws IllegalStateException
      {
         throw new RuntimeException("N/A");
      }

      public MessageContext getMessageContext() throws IllegalStateException
      {
         throw new RuntimeException("N/A");
      }

      public Identity getCallerIdentity()
      {
         throw new RuntimeException("N/A");
      }

      public Principal getCallerPrincipal()
      {
         throw new RuntimeException("N/A");
      }

      public EJBHome getEJBHome()
      {
         throw new RuntimeException("N/A");
      }

      public EJBLocalHome getEJBLocalHome()
      {
         throw new RuntimeException("N/A");
      }

      public Properties getEnvironment()
      {
         throw new RuntimeException("N/A");
      }

      public boolean getRollbackOnly() throws IllegalStateException
      {
         throw new RuntimeException("N/A");
      }

      public TimerService getTimerService() throws IllegalStateException
      {
         throw new RuntimeException("N/A");
      }

      public UserTransaction getUserTransaction() throws IllegalStateException
      {
         return TxUtil.getUserTransaction(StatefulBeanContext.this);
      }

      public boolean isCallerInRole(Identity role)
      {
         throw new RuntimeException("N/A");
      }

      public boolean isCallerInRole(String roleName)
      {
         throw new RuntimeException("N/A");
      }

      public Object lookup(String name)
      {
         throw new RuntimeException("N/A");
      }

      public void setRollbackOnly() throws IllegalStateException
      {
         throw new RuntimeException("N/A");
      }
      
   };
   
   /**
    * @param instance
    * @param interceptors
    */
   public StatefulBeanContext(StatefulContainer<T> container, T instance, List<Object> interceptors)
   {
      super(instance, interceptors);
      
      this.container = container;
   }

   protected StatefulContainer<T> getContainer()
   {
      return container;
   }
   
   public Object getId()
   {
      return id;
   }

   public SimpleMetaData getMetaData()
   {
      return metaData;
   }
   
   protected SessionContext getSessionContext()
   {
      return sessionContext;
   }
   
   protected boolean isTxSynchronized()
   {
      return txSynchronized;
   }
   
   protected void setTxSynchronized(boolean b)
   {
      this.txSynchronized = b;
   }
}
