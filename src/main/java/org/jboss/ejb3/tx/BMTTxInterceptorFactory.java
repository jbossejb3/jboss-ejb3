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

import javax.ejb.Stateful;
import javax.ejb.TransactionManagementType;
import javax.transaction.TransactionManager;

import org.jboss.aop.Advisor;
import org.jboss.aop.joinpoint.Joinpoint;
import org.jboss.ejb3.interceptors.aop.AbstractInterceptorFactory;
import org.jboss.logging.Logger;

/**
 * This interceptor handles transactions for AOP
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision:72368 $
 */
public class BMTTxInterceptorFactory extends AbstractInterceptorFactory
{
   @SuppressWarnings("unused")
   private static final Logger log = Logger.getLogger(CMTTxInterceptorFactory.class);

   public Object createPerJoinpoint(Advisor advisor, Joinpoint jp)
   {
      // We have to do this until AOP supports matching based on annotation attributes
      TransactionManagementType type = TxUtil.getTransactionManagementType(advisor);
      if (type != TransactionManagementType.BEAN)
         return new NullInterceptor();
      
      TransactionManager tm = TxUtil.getTransactionManager();
      boolean stateful = advisor.resolveAnnotation(Stateful.class) != null;
      // Both MessageDriven and Stateless are stateless
      if(stateful)
         return new StatefulBMTInterceptor(tm);
      else
         return new StatelessBMTInterceptor(tm);
   }
}
