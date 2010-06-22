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

import java.net.URL;

import org.jboss.deployers.client.spi.main.MainDeployer;
import org.jboss.deployers.vfs.spi.client.VFSDeployment;
import org.jboss.deployers.vfs.spi.client.VFSDeploymentFactory;
import org.jboss.logging.Logger;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * Deploy a resource.
 * 
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class AspectDeployment
{
   private static final Logger log = Logger.getLogger(AspectDeployment.class);
   
   private MainDeployer mainDeployer;
   private String resource;
   
   private VFSDeployment deployment;
   
   private static URL getResource(String name)
   {
      return Thread.currentThread().getContextClassLoader().getResource(name);
   }
   
   public void setMainDeployer(MainDeployer mainDeployer)
   {
      this.mainDeployer = mainDeployer;
   }
   
   public void setResource(String resource)
   {
      this.resource = resource;
   }
   
   public void start() throws Exception
   {
      if(mainDeployer == null)
         throw new IllegalStateException("mainDeployer is not set");
      if(resource == null)
         throw new IllegalStateException("resource is null");
      
      VirtualFile root = VFS.getRoot(getResource(resource));
      this.deployment = VFSDeploymentFactory.getInstance().createVFSDeployment(root);
      log.info("Deploying " + deployment);
      mainDeployer.deploy(deployment);
   }
   
   public void stop() throws Exception
   {
      mainDeployer.undeploy(deployment);
   }
}
