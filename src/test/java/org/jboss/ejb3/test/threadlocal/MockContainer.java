/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
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
package org.jboss.ejb3.test.threadlocal;

import java.security.Principal;
import java.util.Hashtable;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.TimerService;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.Container;
import org.jboss.ejb3.DependencyPolicy;
import org.jboss.ejb3.pool.Pool;
import org.jboss.ejb3.statistics.InvocationStatistics;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision$
 */
public class MockContainer implements Container
{

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#construct()
    */
   public Object construct()
   {
      return new MockBean();
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#create()
    */
   public void create() throws Exception
   {
      // TODO Auto-generated method stub

   }

   public BeanContext<?> createBeanContext()
   {
      return new MockBeanContext(construct());
   }
   
   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#destroy()
    */
   public void destroy() throws Exception
   {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#getBeanClass()
    */
   public Class getBeanClass()
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#getDependencyPolicy()
    */
   public DependencyPolicy getDependencyPolicy()
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#getEjbName()
    */
   public String getEjbName()
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#getEnc()
    */
   public Context getEnc()
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#getInitialContext()
    */
   public InitialContext getInitialContext()
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#getInitialContextProperties()
    */
   public Hashtable getInitialContextProperties()
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#getInvokeStats()
    */
   public InvocationStatistics getInvokeStats()
   {
      // TODO Auto-generated method stub
      return null;
   }

   public Object getMBean()
   {
      throw new RuntimeException("mock");
   }
   
   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#getObjectName()
    */
   public ObjectName getObjectName()
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#getPool()
    */
   public Pool getPool()
   {
      // TODO Auto-generated method stub
      return null;
   }

   public <T> T getSecurityManager(Class<T> type)
   {
      throw new RuntimeException("mock");
   }
   
   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#getTimerService()
    */
   public TimerService getTimerService()
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#getTimerService(java.lang.Object)
    */
   public TimerService getTimerService(Object pKey)
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#injectContext(org.jboss.ejb3.BeanContext)
    */
   public void injectBeanContext(BeanContext<?> beanContext)
   {
      // TODO Auto-generated method stub
      
   }
   
   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#invokeInit(java.lang.Object)
    */
   public void invokeInit(Object bean)
   {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#invokeInit(java.lang.Object, java.lang.Class[], java.lang.Object[])
    */
   public void invokeInit(Object bean, Class[] initTypes, Object[] initValues)
   {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#invokePostActivate(org.jboss.ejb3.BeanContext)
    */
   public void invokePostActivate(BeanContext beanContext)
   {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#invokePostConstruct(org.jboss.ejb3.BeanContext)
    */
   public void invokePostConstruct(BeanContext beanContext, Object[] params)
   {
      ((MockBean) beanContext.getInstance()).postConstruct();
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#invokePreDestroy(org.jboss.ejb3.BeanContext)
    */
   public void invokePreDestroy(BeanContext beanContext)
   {
      ((MockBean) beanContext.getInstance()).preDestroy();
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#invokePrePassivate(org.jboss.ejb3.BeanContext)
    */
   public void invokePrePassivate(BeanContext beanContext)
   {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#isClustered()
    */
   public boolean isClustered()
   {
      // TODO Auto-generated method stub
      return false;
   }

   public BeanContext<?> peekContext()
   {
      throw new RuntimeException("mock");
   }
   
   public BeanContext<?> popContext()
   {
      // do nothing
      return null;
   }
   
   public void processMetadata()
   {
      throw new RuntimeException("mock");
   }

   public void pushContext(BeanContext<?> ctx)
   {
      // do nothing
   }
   
   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#start()
    */
   public void start() throws Exception
   {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.Container#stop()
    */
   public void stop() throws Exception
   {
      // TODO Auto-generated method stub

   }

   /*
    * (non-Javadoc)
    * @see org.jboss.ejb3.Container#getName()
    */
   public String getName()
   {
      // TODO Auto-generated method stub
      return null;
   }
   
   @Override
   public EJBHome getEJBHome() throws IllegalStateException
   {
      // TODO Auto-generated method stub
      throw new RuntimeException("NYI");
   }

   @Override
   public EJBLocalHome getEJBLocalHome() throws IllegalStateException
   {
      // TODO Auto-generated method stub
      throw new RuntimeException("NYI");
   }

   @Override
   public boolean getRollbackOnly() throws IllegalStateException
   {
      // TODO Auto-generated method stub
      throw new RuntimeException("NYI");
   }

   @Override
   public UserTransaction getUserTransaction() throws IllegalStateException
   {
      // TODO Auto-generated method stub
      throw new RuntimeException("NYI");
   }

   @Override
   public boolean isCallerInRole(Principal callerPrincipal, String roleName) throws IllegalStateException
   {
      // TODO Auto-generated method stub
      throw new RuntimeException("NYI");
   }

   @Override
   public Object lookup(String name) throws IllegalArgumentException
   {
      // TODO Auto-generated method stub
      throw new RuntimeException("NYI");
   }

   @Override
   public void setRollbackOnly() throws IllegalStateException
   {
      // TODO Auto-generated method stub
      throw new RuntimeException("NYI");
   }


}
