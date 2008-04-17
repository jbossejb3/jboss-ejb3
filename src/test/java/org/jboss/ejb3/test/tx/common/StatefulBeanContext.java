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
package org.jboss.ejb3.test.tx.common;

import java.util.List;
import java.util.UUID;

import org.jboss.aop.metadata.SimpleMetaData;
import org.jboss.ejb3.cache.Identifiable;
import org.jboss.ejb3.interceptors.container.DummyBeanContext;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class StatefulBeanContext<T> extends DummyBeanContext<T>
   implements Identifiable, org.jboss.ejb3.tx.container.StatefulBeanContext<T>
{
   private Object id = UUID.randomUUID();
   private SimpleMetaData metaData = new SimpleMetaData();
   
   /**
    * @param instance
    * @param interceptors
    */
   public StatefulBeanContext(T instance, List<Object> interceptors)
   {
      super(instance, interceptors);
   }

   public Object getId()
   {
      return id;
   }

   public SimpleMetaData getMetaData()
   {
      return metaData;
   }
}
