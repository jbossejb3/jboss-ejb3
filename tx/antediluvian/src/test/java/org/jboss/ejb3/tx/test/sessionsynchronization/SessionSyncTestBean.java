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
package org.jboss.ejb3.tx.test.sessionsynchronization;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.SessionSynchronization;
import javax.ejb.Stateful;

import org.jboss.ejb3.test.tx.common.MockEJBContext;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
@Stateful
@Local(SessionSyncTest.class)
public class SessionSyncTestBean implements SessionSynchronization
{
   // Normally this is injected
   @Resource
   private EJBContext ctx = new MockEJBContext();
   
   public void afterBegin() throws EJBException, RemoteException
   {
   }

   public void afterCompletion(boolean committed) throws EJBException, RemoteException
   {
   }

   public void beforeCompletion() throws EJBException, RemoteException
   {
      ctx.setRollbackOnly();
   }
   
   public String sayHi(String name)
   {
      return "Hi " + name;
   }
}
