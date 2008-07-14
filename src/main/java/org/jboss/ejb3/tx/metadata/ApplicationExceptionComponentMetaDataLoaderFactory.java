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

import java.util.List;

import org.jboss.ejb3.metadata.ComponentMetaDataLoaderFactory;
import org.jboss.ejb3.metadata.MetaDataBridge;
import org.jboss.ejb3.metadata.plugins.loader.BridgedMetaDataLoader;
import org.jboss.ejb3.metadata.spi.signature.ClassSignature;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.ApplicationExceptionMetaData;
import org.jboss.metadata.spi.retrieval.MetaDataRetrieval;
import org.jboss.metadata.spi.scope.ScopeKey;
import org.jboss.metadata.spi.signature.Signature;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class ApplicationExceptionComponentMetaDataLoaderFactory implements ComponentMetaDataLoaderFactory<JBossEnterpriseBeanMetaData>
{
   private List<MetaDataBridge<ApplicationExceptionMetaData>> defaultBridges;

   public ApplicationExceptionComponentMetaDataLoaderFactory(List<MetaDataBridge<ApplicationExceptionMetaData>> defaultBridges)
   {
      assert defaultBridges != null : "defaultBridges is null";
      assert !defaultBridges.isEmpty() : "defaultBridges is empty"; // equally stupid
      this.defaultBridges = defaultBridges;
   }
   
   public MetaDataRetrieval createComponentMetaDataRetrieval(JBossEnterpriseBeanMetaData metaData, Signature signature,
         ScopeKey key, ClassLoader classLoader)
   {
      if(signature instanceof ClassSignature)
      {
         ApplicationExceptionMetaData appExMetaData = findApplicationException(metaData, signature.getName());
         if(metaData != null)
            return new BridgedMetaDataLoader<ApplicationExceptionMetaData>(key, appExMetaData, classLoader, defaultBridges);
      }
      return null;
   }
   
   private ApplicationExceptionMetaData findApplicationException(JBossEnterpriseBeanMetaData metaData, String name)
   {
      return metaData.getEjbJarMetaData().getAssemblyDescriptor().getApplicationExceptions().get(name);
   }
}
