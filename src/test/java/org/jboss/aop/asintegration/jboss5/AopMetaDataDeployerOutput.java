/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.aop.asintegration.jboss5;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.aop.microcontainer.beans.Aspect;
import org.jboss.aop.microcontainer.beans.metadata.AspectManagerAwareBeanMetaDataFactory;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.PropertyMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 * @deprecated @see EJBTHREE-1605
 */
@Deprecated
class AopMetaDataDeployerOutput
{
   List<AspectManagerAwareBeanMetaDataFactory> factories;
   List<BeanMetaData> beans;
   boolean scoped;
   String scopedAspectManagerBeanName;
   String domainName;
   int sequence;
   ClassLoaderDomainScope scopeAnnotation;
   
   AopMetaDataDeployerOutput()
   {
      
   }

   String getScopedAspectManagerBeanName()
   {
      return scopedAspectManagerBeanName;
   }

   ClassLoaderDomainScope getScopeAnnotation()
   {
      return scopeAnnotation;
   }
   
   void setFactories(List<AspectManagerAwareBeanMetaDataFactory> factories)
   {
      this.factories = factories;
   }
   
   void setScopedInformation(String scopedAspectManagerBeanName, String domainName, int sequence)
   {
      scoped = true;
      this.scopedAspectManagerBeanName = scopedAspectManagerBeanName;
      this.domainName = domainName;
      this.sequence = sequence;
      if (domainName == null)
      {
         throw new IllegalStateException("Should not have null domainName for scoped bean");
      }
      scopeAnnotation = new ClassLoaderDomainScopeImpl(domainName);
   }
   
   List<BeanMetaData> getBeans()
   {
      if (beans == null && factories != null && factories.size() > 0)
      {
         beans = new ArrayList<BeanMetaData>();
         
         for (AspectManagerAwareBeanMetaDataFactory factory : factories)
         {
            if (scopedAspectManagerBeanName != null)
            {
               factory.setManagerBean(scopedAspectManagerBeanName);
               factory.setManagerProperty(null);
            }

            List<BeanMetaData> mybeans = factory.getBeans();
            if (mybeans != null && mybeans.size() > 0)
            {
               for (BeanMetaData bean : mybeans)
               {
                  massageScopedBean(bean);
               }
               beans.addAll(mybeans);
            }
         }
      }
      return beans;
   }

   private void massageScopedBean(BeanMetaData bean)
   {
      if (scoped)
      {         
         String name = bean.getName();
         String newName = "ScopedAlias_" + sequence + "_" + name;
      
         BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(bean);
         
         //Set the alias to the original name
         builder.addAnnotation(new AliasesImpl(name));
         
         //Set the domain name
         builder.addAnnotation(scopeAnnotation);
      
         //set the new name used by the controller for managing beans
         builder.setName(newName);
         
         //Debug stuff
         if (bean.getBean().equals(Aspect.class.getName()))
         {
            Object scope = null;
            Set<PropertyMetaData> properties = bean.getProperties();
            for (PropertyMetaData property : properties)
            {
               if (property.getName().equals("scope"))
               {
                  scope = property.getValue().getUnderlyingValue();
               }
            }
         }
      }
   }
}
