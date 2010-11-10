/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @authors tag. See the copyright.txt in the
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
package org.jboss.ejb3.effigy;

/**
 * The ApplicationExceptionEffigy declares an application
 * exception.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface ApplicationExceptionEffigy
{
   /**
    * The exception class. When the container receives
    * an exception of this type, it is required to
    * forward this exception as an application exception
    * to the client regardless of whether it is a checked
    * or unchecked exception.
    */
   Class<?> getExceptionClass();

   /**
    * An optional inherited element. If this element is
    * set to true, subclasses of the exception class type
    * are also automatically considered application
    * exceptions (unless overriden at a lower level).
    * If set to false, only the exception class type is
    * considered an application-exception, not its
    * exception subclasses. If not specified, this
    * value defaults to true.
    * @since 3.1
    */
   boolean isInherited();

   /**
    * An optional rollback element. If this element is
    * set to true, the container must rollback the current
    * transaction before forwarding the exception to the
    * client.  If not specified, it defaults to false.
    */
   boolean isRollback();
}
