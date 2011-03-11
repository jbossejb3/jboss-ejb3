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
package org.jboss.ejb3.servitor.common;

import org.jboss.ejb3.context.spi.EJBComponent;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import java.security.Principal;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EnterpriseBeanServitor implements EJBComponent
{
   @Override
   public EJBHome getEJBHome() throws IllegalStateException
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.servitor.common.EntpriseBeanServitor.getEJBHome");
   }

   @Override
   public EJBLocalHome getEJBLocalHome() throws IllegalStateException
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.servitor.common.EntpriseBeanServitor.getEJBLocalHome");
   }

   @Override
   public boolean getRollbackOnly() throws IllegalStateException
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.servitor.common.EntpriseBeanServitor.getRollbackOnly");
   }

   @Override
   public TimerService getTimerService() throws IllegalStateException
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.servitor.common.EntpriseBeanServitor.getTimerService");
   }

   @Override
   public UserTransaction getUserTransaction() throws IllegalStateException
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.servitor.common.EntpriseBeanServitor.getUserTransaction");
   }

   @Override
   public boolean isCallerInRole(Principal callerPrincipal, String roleName) throws IllegalStateException
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.servitor.common.EntpriseBeanServitor.isCallerInRole");
   }

   @Override
   public Object lookup(String name) throws IllegalArgumentException
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.servitor.common.EntpriseBeanServitor.lookup");
   }

   @Override
   public void setRollbackOnly() throws IllegalStateException
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.servitor.common.EntpriseBeanServitor.setRollbackOnly");
   }
}
