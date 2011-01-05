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
package org.jboss.ejb3.tx2.spi;

import org.jboss.ejb3.context.spi.SessionContext;

import javax.transaction.Transaction;

/**
 * EJB 3.0 FR 13.3.3:
 * A stateful session bean instance may, but is not required to, commit a started transaction before a busi-
 * ness method returns. If a transaction has not been completed by the end of a business method, the con-
 * tainer retains the association between the transaction and the instance across multiple client calls until
 * the instance eventually completes the transaction.
 *
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface StatefulContext extends SessionContext
{
   /**
    * Transient association with the transaction.
    */
   Transaction getTransaction();

   /**
    * @param tx tx to associate or null to disassociate
    */
   void setTransaction(Transaction tx);
}
