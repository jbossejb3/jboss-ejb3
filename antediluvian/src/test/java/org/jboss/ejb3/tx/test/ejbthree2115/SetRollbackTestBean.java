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
package org.jboss.ejb3.tx.test.ejbthree2115;

import org.jboss.ejb3.test.tx.common.MockEJBContext;

import javax.annotation.Resource;
import javax.ejb.*;

import static javax.ejb.TransactionAttributeType.*;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
@Stateful
public class SetRollbackTestBean
{
   private EJBContext ctx = new MockEJBContext();

   public boolean prePassivateRan = false;

   @TransactionAttribute(MANDATORY)
   public void mandatory()
   {
      ctx.setRollbackOnly();
   }

   @TransactionAttribute(NEVER)
   public void never()
   {
      ctx.setRollbackOnly();
   }

   @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
   public void notSupported()
   {
      ctx.setRollbackOnly();
   }

   @PrePassivate
   public void prePassivate()
   {
      ctx.setRollbackOnly();

      prePassivateRan = true;
   }

   @TransactionAttribute(REQUIRED)
   public void required()
   {
      ctx.setRollbackOnly();
   }

   @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
   public void requiresNew()
   {
      ctx.setRollbackOnly();
   }

   @Resource
   public void setSessionContext(SessionContext ctx)
   {
      ctx.setRollbackOnly();
   }

   @TransactionAttribute(TransactionAttributeType.SUPPORTS)
   public void supports()
   {
      ctx.setRollbackOnly();
   }
}