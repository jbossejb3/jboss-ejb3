/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

import java.lang.reflect.Method;

import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
import javax.transaction.TransactionManager;

import org.jboss.aop.Advisor;
import org.jboss.aop.joinpoint.Joinpoint;
import org.jboss.aop.joinpoint.MethodJoinpoint;
import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.logging.Logger;

/**
 * This interceptor handles transactions for AOP
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision:72368 $
 */
public class CMTTxInterceptorFactory extends org.jboss.aspects.tx.TxInterceptorFactory
{
   @SuppressWarnings("unused")
   private static final Logger log = Logger.getLogger(CMTTxInterceptorFactory.class);

   protected TransactionAttributeType getTxType(Advisor advisor, Joinpoint jp)
   {
      Method method = ((MethodJoinpoint) jp).getMethod();
      return TxUtil.getTxType(advisor, method);
   }

   protected int resolveTransactionTimeout(Advisor advisor, Method method)
   {
      TransactionTimeout annotation = (TransactionTimeout)advisor.resolveAnnotation(method, TransactionTimeout.class);
      
      if (annotation == null)
         annotation = (TransactionTimeout)advisor.resolveAnnotation(TransactionTimeout.class);
      
      if (annotation != null)
      {
         return annotation.value();
      }

      return -1;
   }

   @Override
   protected void initializePolicy()
   {
      policy = new Ejb3TxPolicy();
   }

   @Override
   public Object createPerJoinpoint(Advisor advisor, Joinpoint jp)
   {
      // We have to do this until AOP supports matching based on annotation attributes
      TransactionManagementType type = TxUtil.getTransactionManagementType(advisor);
      if (type == TransactionManagementType.BEAN)
         return new NullInterceptor();

      TransactionManager tm = TxUtil.getTransactionManager();
      
      Method method = ((MethodJoinpoint) jp).getMethod();
      int timeout = resolveTransactionTimeout(advisor, method);

      if (policy == null)
      {
         super.initialize();
      }

      TransactionAttributeType txType = getTxType(advisor, jp);
      
      if (txType.equals(TransactionAttributeType.NEVER))
      {
         // make sure we use the EJB3 interceptor, not the AOP one. 
         return new TxInterceptor.Never(tm, policy);
      }
      else if (txType.equals(TransactionAttributeType.REQUIRED))
      {
         return new TxInterceptor.Required(tm, policy, timeout);
      }
      else if (txType.equals(TransactionAttributeType.REQUIRES_NEW))
      {
         return new TxInterceptor.RequiresNew(tm, policy, timeout);
      }
      else if(txType.equals(TransactionAttributeType.NOT_SUPPORTED))
      {
         return new TxInterceptor.NotSupported(tm, policy, timeout);
      }
      else if(txType.equals(TransactionAttributeType.MANDATORY))
      {
         return new TxInterceptor.Mandatory(tm, policy, timeout);
      }
      else if(txType.equals(TransactionAttributeType.SUPPORTS))
      {
         return new TxInterceptor.Supports(tm, policy, timeout);
      }
      else
      {
         Object interceptor = super.createPerJoinpoint(advisor, jp);
         return interceptor;
      }
   }
}
