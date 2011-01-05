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
package org.jboss.ejb3.tx;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJBTransactionRolledbackException;

import org.jboss.aop.Advisor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.ejb3.tx.api.TransactionRetry;

/**
 * Retry an operation if the transaction is rolled back.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class RetryingInterceptor extends AbstractInterceptor
{
   protected <A extends Annotation> A getAnnotation(Invocation invocation, Method method, Class<A> annotationType)
   {
      Advisor advisor = invocation.getAdvisor();
      A annotation = annotationType.cast(advisor.resolveAnnotation(method, annotationType));
      if(annotation == null)
         annotation = annotationType.cast(advisor.resolveAnnotation(annotationType));
      return annotation;
   }
   
   public Object invoke(Invocation invocation) throws Throwable
   {
      Method method = ((MethodInvocation) invocation).getMethod();
      TransactionRetry txRetry = getAnnotation(invocation, method, TransactionRetry.class);
      
      int numRetries = txRetry.numRetries();
      long waitTime = TimeUnit.MILLISECONDS.convert(txRetry.waitTime(), txRetry.waitTimeUnit());
      
      while(true)
      {
         try
         {
            return invocation.invokeNext();
         }
         catch(EJBTransactionRolledbackException e)
         {
            Thread.sleep(waitTime);
            if(--numRetries <= 0)
               throw e;
         }
      }
   }
}
