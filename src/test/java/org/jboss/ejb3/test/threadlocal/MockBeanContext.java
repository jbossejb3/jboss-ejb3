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

import javax.ejb.EJBContext;

import org.jboss.aop.metadata.SimpleMetaData;
import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.Container;
import org.jboss.ejb3.interceptor.InterceptorInfo;
import org.jboss.logging.Logger;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision$
 */
public class MockBeanContext implements BeanContext
{
   private static Logger log = Logger.getLogger(MockBeanContext.class);
   
   private Object instance;
   
   public MockBeanContext(Object instance)
   {
      assert instance != null;
      
      this.instance = instance;
   }
   
   @Override
   protected void finalize() throws Throwable
   {
      log.info("finalize");
      super.finalize();
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.BeanContext#getContainer()
    */
   public Container getContainer()
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.BeanContext#getEJBContext()
    */
   public EJBContext getEJBContext()
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.BeanContext#getInstance()
    */
   public Object getInstance()
   {
      assert instance != null;
      return instance;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.BeanContext#getInterceptorInstances(org.jboss.ejb3.interceptor.InterceptorInfo[])
    */
   public Object[] getInterceptorInstances(InterceptorInfo[] interceptorInfos)
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.interceptors.container.BeanContext#getInterceptors()
    */
   public Object[] getInterceptors()
   {
      // TODO Auto-generated method stub
      return null;
   }
   
   /* (non-Javadoc)
    * @see org.jboss.ejb3.BeanContext#getInvokedMethodKey()
    */
   public Object getInvokedMethodKey()
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.BeanContext#getMetaData()
    */
   public SimpleMetaData getMetaData()
   {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.BeanContext#initialiseInterceptorInstances()
    */
   public void initialiseInterceptorInstances()
   {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.BeanContext#remove()
    */
   public void remove()
   {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.BeanContext#setContainer(org.jboss.ejb3.Container)
    */
   public void setContainer(Container container)
   {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.BeanContext#setInstance(java.lang.Object)
    */
   public void setInstance(Object instance)
   {
      assert instance != null;
      this.instance = instance;
   }

}
