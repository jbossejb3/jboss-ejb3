/*
* JBoss, Home of Professional Open Source
* Copyright 2005, Red Hat Middleware LLC., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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

import java.util.List;

import org.jboss.beans.metadata.spi.BeanMetaDataFactory;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.kernel.spi.deployment.KernelDeployment;

/**
 * Deployer for Aspects in a -beans.xml
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @deprecated @see EJBTHREE-1605
 */
@Deprecated
public class BeansDeploymentAopMetaDataDeployer extends AbstractAopMetaDataDeployer<KernelDeployment>
{
   public BeansDeploymentAopMetaDataDeployer()
   {
      super(KernelDeployment.class);
   }

   @Override
   public void deploy(VFSDeploymentUnit unit, KernelDeployment kernelDeployment) throws DeploymentException
   {
      super.deploy(unit, kernelDeployment);
   }
   
   @Override
   public void undeploy(VFSDeploymentUnit unit, KernelDeployment kernelDeployment)
   {
      super.undeploy(unit, kernelDeployment);
   }


   @Override
   protected List<BeanMetaDataFactory> getFactories(KernelDeployment kernelDeployment)
   {
      return kernelDeployment.getBeanFactories();
   }
   
}
