/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.ejb3.tx2.spi;

import javax.ejb.ApplicationException;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionManager;
import java.lang.reflect.Method;

/**
 * A {@link TransactionalComponent} represents the runtime component of a EJB. It is meant to provide access to EJB
 * metadata for transaction management interceptors.
 * <p/>
 * Author : Jaikiran Pai
 */
public interface TransactionalComponent
{

   /**
    * Returns the {@link TransactionAttributeType} applicable to the passed <code>method</code>.
    * If the passed <code>method</method> is null, then the {@link TransactionAttributeType} applicable for lifecycle
    * callback invocations on this {@link TransactionalComponent} will be returned.
    * <p/>
    * If there is no explicit {@link TransactionAttributeType} specified for the passed <code>method</code> then this method
    * must return the default applicable {@link TransactionAttributeType}. This method must *not* return a null value.
    *
    * @param method The method for which the {@link TransactionAttributeType} is being queried. Can be null, if the query
    *               is for lifecycle callback invocation.
    * @return
    */
   TransactionAttributeType getTransactionAttributeType(Method method);

   /**
    * Returns the {@link ApplicationException} applicable for the passed <code>exceptionClass</code>.
    * If the passed <code>exceptionClass</code> doesn't represent an {@link ApplicationException} then this method
    * returns null.
    *
    * @param exceptionClass
    * @return
    */
   ApplicationException getApplicationException(Class<?> exceptionClass);

   /**
    * Returns the transaction timeout, in seconds, applicable for the passed <code>method</code>.
    *
    * @param method The method for which the transaction timeout is being queried
    * @return
    */
   int getTransactionTimeout(Method method);

   /**
    * Returns the {@link TransactionManager} of this {@link TransactionalComponent component}
    *
    * @return
    */
   TransactionManager getTransactionManager();
}
