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
package org.jboss.ejb3.interceptors.effigy;

import org.jboss.ejb3.effigy.EnterpriseBeanEffigy;
import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.model.InterceptionModel;

/**
 * Transforms Effigy metadata into Interceptors metadata.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class Transformer
{
   private EnterpriseBeanEffigy enterpriseBeanEffigy;
   private InterceptorMetadata<ClassMetadata<?>> beanClassInterceptorMetadata;
   private InterceptionModel<ClassMetadata<?>, ?> interceptionModel;

   /**
    * Both a message driven and a session bean can have interceptors.
    *
    * @param enterpriseBeanEffigy
    */
   public Transformer(EnterpriseBeanEffigy enterpriseBeanEffigy)
   {
      this.enterpriseBeanEffigy = enterpriseBeanEffigy;
      this.beanClassInterceptorMetadata = new EnterpriseBeanInterceptorMetadata(enterpriseBeanEffigy);
      this.interceptionModel = new EnterpriseBeanInterceptionModel(enterpriseBeanEffigy);
   }

   public InterceptorMetadata<ClassMetadata<?>> getBeanClassInterceptorMetadata()
   {
      return beanClassInterceptorMetadata;
   }

   public InterceptionModel<ClassMetadata<?>, ?> getInterceptionModel()
   {
      return interceptionModel;
   }
}
