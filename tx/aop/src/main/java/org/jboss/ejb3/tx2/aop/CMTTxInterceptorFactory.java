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
package org.jboss.ejb3.tx2.aop;

import org.jboss.aop.Advisor;
import org.jboss.aop.InstanceAdvisor;
import org.jboss.aop.advice.AspectFactory;
import org.jboss.aop.joinpoint.Joinpoint;

import javax.transaction.TransactionManager;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class CMTTxInterceptorFactory implements AspectFactory
{
   private TransactionManager transactionManager;

   public Object createPerVM()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.tx2.aop.CMTTxInterceptorFactory.createPerVM");
   }

   public Object createPerClass(Advisor advisor)
   {
      // Iny Mini Miny Moe
      return new CMTTxInterceptorWrapper(getTransactionManager());
   }

   public Object createPerInstance(Advisor advisor, InstanceAdvisor instanceAdvisor)
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.tx2.aop.CMTTxInterceptorFactory.createPerInstance");
   }

   public Object createPerJoinpoint(Advisor advisor, Joinpoint jp)
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.tx2.aop.CMTTxInterceptorFactory.createPerJoinpoint");
   }

   public Object createPerJoinpoint(Advisor advisor, InstanceAdvisor instanceAdvisor, Joinpoint jp)
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.tx2.aop.CMTTxInterceptorFactory.createPerJoinpoint");
   }

   public String getName()
   {
      return getClass().getName();
   }
   
   /**
    * Returns the TransactionManager
    * @return
    */
   private TransactionManager getTransactionManager()
   {
      if (this.transactionManager == null)
      {
         this.transactionManager = TxUtil.getTransactionManager();
      }
      return this.transactionManager;
   }
}
