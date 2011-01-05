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

import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.SessionSynchronization;
import javax.ejb.Stateful;

import org.jboss.ejb3.test.tx.common.AbstractGreeterBean;
import org.jboss.ejb3.test.tx.common.Greeter;
import org.jboss.logging.Logger;

/**
 * getRollbackOnly is not allowed in afterCompletion (EJB 3 4.4.1)
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
@Stateful
@Local(Greeter.class)
public class AfterCompletionTestBean extends AbstractGreeterBean implements SessionSynchronization
{
   private static final Logger log = Logger.getLogger(AfterCompletionTestBean.class);
   
   public static String result = null;
   
   public void afterBegin() throws EJBException, RemoteException
   {
   }

   public void afterCompletion(boolean committed) throws EJBException, RemoteException
   {
      try
      {
         ctx.getRollbackOnly();
         result = "allowed";
      }
      catch(IllegalStateException e)
      {
         result = "disallowed";
      }
      catch(Exception e)
      {
         result = e.getMessage();
         log.error("getRollbacjOnly failed", e);
      }
   }

   public void beforeCompletion() throws EJBException, RemoteException
   {
   }
}
