/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.endpoint;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * An endpoint is capable of handling invocation on an EJB instance.
 * 
 * An endpoint might be session aware, in which case an session has to be
 * obtained from the session factory. This session can then be used to call
 * upon the endpoint.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public interface Endpoint
{
   /**
    * The SessionFactory associated with this Endpoint, if the Endpoint is session aware.
    * 
    * @throws IllegalStateException if this Endpoint is not session aware
    * @return the associated session factory
    */
   SessionFactory getSessionFactory() throws IllegalStateException;
   
   /**
    * Invoke a method on an EJB endpoint.
    * 
    * @param session the identification of the EJB instance to invoke the method upon
    *   or null if the endpoint doesn't support sessions.
    * @param invokedBusinessInterface the invokedBusinessInterface or null if not known.
    * @param method the method to invoke on the EJB instance, note that if 
    *   invokedBusinessInterface is specified then the declaring class of the 
    *   {@code Method} object must be an instance of
    *   the specified {@code Class} invokedBusinessInterface.
    * @param args an array of objects containing the values of the
    *   arguments passed in the method invocation on the proxy instance,
    *   or {@code null} if interface method takes no arguments.
    *   Arguments of primitive types are wrapped in instances of the
    *   appropriate primitive wrapper class, such as
    *   {@code java.lang.Integer} or {@code java.lang.Boolean}.
    * 
    * @return the return value of the invoked {@code Method} method.
    * @throws Throwable the exception to throw from the method
    *   invocation on the EJB instance.
    */
   Object invoke(Serializable session, Class<?> invokedBusinessInterface, Method method, Object args[]) throws Throwable;
   
   /**
    * @return true if this Endpoint is session aware
    */
   boolean isSessionAware();
}
