/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.ejb3.interceptors.container;

/**
 * The intercepted component consisting of a bean instance and the interceptor
 * instances.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface BeanContext
{
   /**
    * @return the bean instance
    */
   Object getInstance();
   
   /**
    * Get the EJB3 interceptor of a certain type.
    *
    * @param interceptorClass   the interceptor type to get
    * @return   the interceptor
    * @throws IllegalArgumentException  if no interceptor of that class is associated
    */
   Object getInterceptor(Class<?> interceptorClass) throws IllegalArgumentException;
}
