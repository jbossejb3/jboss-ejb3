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
package org.jboss.ejb3.endpoint.reflect;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.jboss.ejb3.endpoint.Endpoint;

/**
 * Create an ordinary Proxy for an Endpoint.
 * 
 * This is more an utility class for unit testing, than something that's actually
 * usable in real scenarios. The assumption is that users of an Endpoint will
 * use EndpointInvocationHandler.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class EndpointProxy
{
   public static <T> T newProxyInstance(ClassLoader loader, Serializable session, Class<T> businessInterface, Endpoint endpoint)
   {
      // it's bound to happen anyway
      if(businessInterface == null)
         throw new NullPointerException("businessInterface is null");
      InvocationHandler handler = new EndpointInvocationHandler(endpoint, session, businessInterface);
      Class<?> interfaces[] = { businessInterface };
      return businessInterface.cast(Proxy.newProxyInstance(loader, interfaces, handler));
   }
}
