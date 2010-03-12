/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.concurrency.aop.interceptor;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;

import org.jboss.aop.Advisor;
import org.jboss.aop.InstanceAdvisor;
import org.jboss.aop.advice.InterceptorFactory;
import org.jboss.ejb3.interceptors.aop.AbstractInterceptorFactory;

/**
 * Responsible for creating an AOP based interceptor ({@link ContainerManagedConcurrencyInterceptor})
 * if the bean for which this {@link InterceptorFactory} is configured requires container managed 
 * concurrency.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class ContainerManagedConcurrencyInterceptorFactory extends AbstractInterceptorFactory
{
   /*
    * By default a singleton container must have container managed concurrency, this can be setup
    * within aop.xml instead of forcing it through an interceptor factory.
    */
   
   /**
    * Returns a {@link ContainerManagedConcurrencyInterceptor} if the advisor is for a bean
    * which requires container managed concurrency. Else returns null
    */
   @Override
   public Object createPerInstance(Advisor advisor, InstanceAdvisor instanceAdvisor)
   {
      if(!isContainerManagedConcurrency(advisor))
         return null;
      return new ContainerManagedConcurrencyInterceptor();
   }
   
   /**
    * Returns true if the bean corresponding to the passed <code>advisor</code> requires
    * container managed concurrency. Else returns false.
    * 
    * @param advisor
    * @return
    */
   private boolean isContainerManagedConcurrency(Advisor advisor)
   {
      ConcurrencyManagement cm = (ConcurrencyManagement) advisor.resolveAnnotation(ConcurrencyManagement.class);
      // 4.8.5.3 By default, a singleton bean has container managed concurrency demarcation if the concurrency management type is not specified.
      if(cm == null)
         return true;
      return cm.value() == ConcurrencyManagementType.CONTAINER;
   }
}
