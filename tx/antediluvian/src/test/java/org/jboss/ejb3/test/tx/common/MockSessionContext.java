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

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.SessionContext;
import javax.xml.rpc.handler.MessageContext;

/**
 * A mock session context which doesn't do anything.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class MockSessionContext extends MockEJBContext implements SessionContext
{
   public <T> T getBusinessObject(Class<T> businessInterface) throws IllegalStateException
   {
      throw new IllegalStateException("N/A");
   }

   public EJBLocalObject getEJBLocalObject() throws IllegalStateException
   {
      throw new IllegalStateException("N/A");
   }

   public EJBObject getEJBObject() throws IllegalStateException
   {
      throw new IllegalStateException("N/A");
   }

   public Class<?> getInvokedBusinessInterface() throws IllegalStateException
   {
      throw new IllegalStateException("N/A");
   }

   public MessageContext getMessageContext() throws IllegalStateException
   {
      throw new IllegalStateException("N/A");
   }
}
