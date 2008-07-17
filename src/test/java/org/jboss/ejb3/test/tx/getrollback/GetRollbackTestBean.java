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
package org.jboss.ejb3.test.tx.getrollback;

import static javax.ejb.TransactionAttributeType.MANDATORY;
import static javax.ejb.TransactionAttributeType.NEVER;
import static javax.ejb.TransactionAttributeType.REQUIRED;

import javax.ejb.EJBContext;
import javax.ejb.PrePassivate;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.ejb3.test.tx.common.MockEJBContext;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
@Stateless
public class GetRollbackTestBean
{
   private EJBContext ctx = new MockEJBContext();
   
   public boolean prePassivateRan = false;
   
   @TransactionAttribute(MANDATORY)
   public boolean mandatory()
   {
      return ctx.getRollbackOnly();
   }
   
   @TransactionAttribute(NEVER)
   public boolean never()
   {
      return ctx.getRollbackOnly();
   }
   
   @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
   public boolean notSupported()
   {
      return ctx.getRollbackOnly();
   }
   
   @PrePassivate
   public void prePassivate()
   {
      ctx.getRollbackOnly();
      
      prePassivateRan = true;
   }
   
   @TransactionAttribute(REQUIRED)
   public boolean required()
   {
      return ctx.getRollbackOnly();
   }
   
   @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
   public boolean requiresNew()
   {
      return ctx.getRollbackOnly();
   }
   
   @TransactionAttribute(TransactionAttributeType.SUPPORTS)
   public boolean supports()
   {
      return ctx.getRollbackOnly();
   }
}
