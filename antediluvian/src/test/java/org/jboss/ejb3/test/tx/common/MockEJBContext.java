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
import java.util.Properties;

import javax.ejb.EJBContext;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;

import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aspects.currentinvocation.CurrentInvocation;
import org.jboss.ejb3.tx.TxUtil;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
@SuppressWarnings("deprecation")
public class MockEJBContext implements EJBContext
{
   public Identity getCallerIdentity()
   {
      throw new IllegalStateException("N/A");
   }

   public Principal getCallerPrincipal()
   {
      throw new IllegalStateException("N/A");
   }

   private Invocation getCurrentInvocation()
   {
      return CurrentInvocation.getCurrentInvocation();
   }
   
   public EJBHome getEJBHome()
   {
      throw new IllegalStateException("N/A");
   }

   public EJBLocalHome getEJBLocalHome()
   {
      throw new IllegalStateException("N/A");
   }

   public Properties getEnvironment()
   {
      throw new IllegalStateException("N/A");
   }

   public boolean getRollbackOnly() throws IllegalStateException
   {
      return TxUtil.getRollbackOnly();
   }

   public TimerService getTimerService() throws IllegalStateException
   {
      throw new IllegalStateException("N/A");
   }

   public UserTransaction getUserTransaction() throws IllegalStateException
   {
      throw new IllegalStateException("N/A");
   }

   public boolean isCallerInRole(Identity role)
   {
      throw new IllegalStateException("N/A");
   }

   public boolean isCallerInRole(String roleName)
   {
      throw new IllegalStateException("N/A");
   }

   public Object lookup(String name)
   {
      throw new IllegalStateException("N/A");
   }

   public void setRollbackOnly() throws IllegalStateException
   {
      TxUtil.setRollbackOnly();
   }
}
