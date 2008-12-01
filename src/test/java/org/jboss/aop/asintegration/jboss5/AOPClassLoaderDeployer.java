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

import org.jboss.aop.AspectManager;
import org.jboss.aop.Domain;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 * @deprecated @see EJBTHREE-1605
 */
@Deprecated
public class AOPClassLoaderDeployer extends AbstractRealDeployer
{
   AspectManager aspectManager;
   
   public AOPClassLoaderDeployer()
   {
      setStage(DeploymentStages.CLASSLOADER);

      //This makes it come after the ClassLoaderDeployer
      addInput(ClassLoader.class);
   }

   public AspectManager getAspectManager()
   {
      return aspectManager;
   }

   public void setAspectManager(AspectManager aspectManager)
   {
      this.aspectManager = aspectManager;
   }

   @Override
   protected void internalDeploy(DeploymentUnit unit) throws DeploymentException
   {
      //Scoped AOP deployments are only available when deployed as part of a scoped sar, ear etc.
      //It can contain an aop.xml file, or it can be part of a .aop file

      AspectManager manager = aspectManager;
      Domain domain = AOPClassLoaderInitializer.initializeForUnit(unit);
      if (domain != null)
      {
         manager = domain;
      }
      log.debug("Adding AspectManager attachment " + manager + " for " + unit);
      unit.addAttachment(AspectManager.class, manager);
   }

   @Override
   public void internalUndeploy(DeploymentUnit unit)
   {
      AOPClassLoaderInitializer.unregisterLoaders(aspectManager, unit);
   }
   
   

}
