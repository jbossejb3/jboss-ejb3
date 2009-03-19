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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.jboss.ejb3.endpoint.Endpoint;

/**
 * An InvocationHandler adapter for an Endpoint.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class EndpointInvocationHandler implements InvocationHandler
{
   private static final Method METHOD_EQUALS;
   private static final Method METHOD_HASH_CODE;
   private static final Method METHOD_TO_STRING;
   
   static
   {
      try
      {
         METHOD_EQUALS = Object.class.getDeclaredMethod("equals", Object.class);
         METHOD_HASH_CODE = Object.class.getDeclaredMethod("hashCode");
         METHOD_TO_STRING = Object.class.getDeclaredMethod("toString");
      }
      catch (SecurityException e)
      {
         throw new RuntimeException(e);
      }
      catch (NoSuchMethodException e)
      {
         throw new RuntimeException(e);
      }
   }
   
   private Endpoint endpoint;
   private Serializable session;
   private Class<?> invokedBusinessInterface;
   
   /**
    * Creates an invocation handler.
    */
   public EndpointInvocationHandler(Endpoint endpoint, Serializable session, Class<?> invokedBusinessInterface)
   {
      assert endpoint != null : "endpoint is null";
      
      this.endpoint = endpoint;
      this.session = session; // can be null
      this.invokedBusinessInterface = invokedBusinessInterface; // can be null;
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj)
   {
      if(obj == this)
         return true;
      
      if(obj == null)
         return false;
      
      if(Proxy.isProxyClass(obj.getClass()))
         return equals(Proxy.getInvocationHandler(obj));
      
      // It might not be a JDK proxy that's handling this, so here we do a little trick.
      // Normally you would do:
      //      if(!(obj instanceof EndpointInvocationHandler))
      //         return false;
      if(!(obj instanceof EndpointInvocationHandler))
         return obj.equals(this);
      
      EndpointInvocationHandler other = (EndpointInvocationHandler) obj;
      
      if(!(other.endpoint.equals(this.endpoint)))
         return false;
      
      if(!equals(other.session, this.session))
         return false;
      
      if(!equals(other.invokedBusinessInterface, this.invokedBusinessInterface))
         return false;
      
      return true;
   }
   
   private static boolean equals(Object obj1, Object obj2)
   {
      if(obj1 == obj2)
         return true;
      
      if(obj1 == null || obj2 == null)
         return false;
      
      return obj1.equals(obj2);
   }
   
   @Override
   public int hashCode()
   {
      int hashCode = endpoint.hashCode();
      if(session != null)
         hashCode += (session.hashCode() << 2);
      if(invokedBusinessInterface != null)
         hashCode += (invokedBusinessInterface.hashCode() << 4);
      return hashCode;
   }
   
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      if(method.equals(METHOD_EQUALS))
         return equals(args[0]);
      if(method.equals(METHOD_HASH_CODE))
         return hashCode();
      if(method.equals(METHOD_TO_STRING))
         return toProxyString();
      return endpoint.invoke(session, invokedBusinessInterface, method, args);
   }
   
   public String toProxyString()
   {
      return "Proxy on " + toString();
   }
   
   @Override
   public String toString()
   {
      StringBuffer sb = new StringBuffer(super.toString());
      sb.append("{endpoint=" + endpoint);
      sb.append(",invokedBusinessInterface=" + invokedBusinessInterface);
      sb.append(",session=" + session);
      sb.append("}");
      return sb.toString();
   }
}
