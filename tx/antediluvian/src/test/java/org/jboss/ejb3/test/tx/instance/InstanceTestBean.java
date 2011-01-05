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
package org.jboss.ejb3.test.tx.instance;

import static javax.ejb.TransactionAttributeType.MANDATORY;
import static javax.ejb.TransactionAttributeType.NEVER;
import static javax.ejb.TransactionAttributeType.REQUIRED;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
@Stateful
public class InstanceTestBean implements InstanceTest
{
   @TransactionAttribute(MANDATORY)
   public void mandatory()
   {
      
   }
   
   @TransactionAttribute(NEVER)
   public void never()
   {
      
   }
   
   @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
   public void notSupported()
   {
      
   }
   
   @TransactionAttribute(REQUIRED)
   public void required()
   {
      
   }
   
   @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
   public void requiresNew()
   {
      
   }
   
   @TransactionAttribute(TransactionAttributeType.SUPPORTS)
   public void supports()
   {
      
   }
}
