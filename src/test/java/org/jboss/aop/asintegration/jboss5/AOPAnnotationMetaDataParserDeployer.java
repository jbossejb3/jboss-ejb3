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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javassist.bytecode.ClassFile;

import org.jboss.aop.AspectAnnotationLoader;
import org.jboss.aop.microcontainer.beans.metadata.AOPDeployment;
import org.jboss.aop.microcontainer.beans.metadata.AspectManagerAwareBeanMetaDataFactory;
import org.jboss.aop.microcontainer.beans.metadata.MicrocontainerAnnotationLoaderStrategy;
import org.jboss.beans.metadata.spi.BeanMetaDataFactory;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VirtualFileFilter;
import org.jboss.virtual.VisitorAttributes;
import org.jboss.virtual.plugins.context.jar.JarUtils;
import org.jboss.virtual.plugins.vfs.helpers.FilterVirtualFileVisitor;
import org.jboss.virtual.plugins.vfs.helpers.SuffixesExcludeFilter;

/**
 * Reads the annotations and converts them into AOP metadata to be deployed properly 
 * by the AOPDeploymentAopMetaDataDeployer
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 * @deprecated @see EJBTHREE-1605
 */
@Deprecated
public class AOPAnnotationMetaDataParserDeployer extends AbstractDeployer
{
   public AOPAnnotationMetaDataParserDeployer(int xmlParserOrder)
   {
      super.setOutput(AOPDeployment.class);
      super.setStage(DeploymentStages.PARSE);
      //Make this come after the AOPXMLMetaDataParserDeployer
      super.setRelativeOrder(xmlParserOrder + 1);
   }

   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      // Ignore non-vfs deployments
      if (unit instanceof VFSDeploymentUnit == false)
      {
         log.trace("Not a vfs deployment: " + unit.getName());
         return;
      }
      // See if the suffix matches the .aop requirement
      if (unit.getSimpleName().endsWith(".aop") == false)
      {
         log.trace("Unit name does not end in .aop: " + unit.getSimpleName());
         return;
      }
      internalDeploy((VFSDeploymentUnit)unit);
   }
   
   private void internalDeploy(VFSDeploymentUnit unit) throws DeploymentException
   {
      MicrocontainerAnnotationLoaderStrategy strategy = new MicrocontainerAnnotationLoaderStrategy(); 
      AspectAnnotationLoader loader = new AspectAnnotationLoader(null, strategy);
      
      List<VirtualFile> files = getClasses(unit);
      for(VirtualFile file : files)
      {
         try
         {
            ClassFile cf = loadClassFile(file);
            log.debug("Deploying possibly annotated class " + cf.getName());
            loader.deployClassFile(cf);
         }
         catch (Exception e)
         {
            throw new DeploymentException("Error reading annotations for " + file, e);
         }
      }
      
      List<AspectManagerAwareBeanMetaDataFactory> factories = strategy.getFactories();
      
      AOPDeployment deployment = unit.getAttachment(AOPDeployment.class);
      if (factories != null && factories.size() > 0)
      {
         if (deployment == null)
         {
            deployment = new AOPDeployment();
            unit.addAttachment(AOPDeployment.class.getName(), deployment, AOPDeployment.class);
         }
         if (deployment.getBeanFactories() == null)
         {
            deployment.setBeanFactories(new ArrayList<BeanMetaDataFactory>());
         }
         deployment.getBeanFactories().addAll(factories);
      }  
   }

   private ClassFile loadClassFile(VirtualFile file)
   {
      DataInputStream din = null;
      ClassFile cf = null;
      try
      {
         InputStream in = file.openStream();
         din = new DataInputStream(new BufferedInputStream(in));
         cf = new ClassFile(din);
      }
      catch (IOException e)
      {
         throw new RuntimeException("Error reading " + file, e);
      }
      finally
      {
         try
         {
            din.close();
         }
         catch (IOException ignored)
         {
         }
      }
      
      return cf;
   }

   private List<VirtualFile> getClasses(VFSDeploymentUnit unit)
   {
      VisitorAttributes va = new VisitorAttributes();
      va.setLeavesOnly(true);
      ClassFileFilter filter = new ClassFileFilter();
      SuffixesExcludeFilter noJars = new SuffixesExcludeFilter(JarUtils.getSuffixes());
      va.setRecurseFilter(noJars);
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter, va);

      for (VirtualFile vf : unit.getClassPath())
      {
         try
         {
            vf.visit(visitor);
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
      }
      return visitor.getMatched();

   }
   
   private static class ClassFileFilter implements VirtualFileFilter
   {
      public boolean accepts(VirtualFile file)
      {
         try
         {
            return file.isLeaf() && file.getName().endsWith(".class");
         }
         catch (IOException e)
         {
            throw new RuntimeException("Error visiting file: " + file.getName(), e);
         }
      }
   }
}
