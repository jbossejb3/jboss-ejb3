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
package org.jboss.ejb3.tx.metadata;

import java.lang.annotation.Annotation;

import javax.ejb.ApplicationException;

import org.jboss.ejb3.annotation.impl.ApplicationExceptionImpl;
import org.jboss.ejb3.metadata.MetaDataBridge;
import org.jboss.metadata.ejb.spec.ApplicationExceptionMetaData;
import org.jboss.metadata.spi.signature.DeclaredMethodSignature;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class ApplicationExceptionMetaDataBridge implements MetaDataBridge<ApplicationExceptionMetaData>
{
   public <A extends Annotation> A retrieveAnnotation(Class<A> annotationClass, ApplicationExceptionMetaData metaData,
         ClassLoader classLoader)
   {
      if(annotationClass == ApplicationException.class && metaData != null)
      {
         return annotationClass.cast(new ApplicationExceptionImpl(metaData.isRollback()));
      }
      return null;
   }

   public <A extends Annotation> A retrieveAnnotation(Class<A> annotationClass, ApplicationExceptionMetaData metaData,
         ClassLoader classLoader, DeclaredMethodSignature method)
   {
      if(annotationClass == ApplicationException.class)
      {
         throw new IllegalArgumentException("Can't retrieve on a method");
      }
      return null;
   }
}
