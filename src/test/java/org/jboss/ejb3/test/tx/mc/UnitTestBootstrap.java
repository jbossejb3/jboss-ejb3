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
package org.jboss.ejb3.test.tx.mc;

import java.net.URL;

import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.kernel.plugins.bootstrap.basic.BasicBootstrap;
import org.jboss.kernel.plugins.deployment.xml.BasicXMLDeployer;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.kernel.spi.deployment.KernelDeployment;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class UnitTestBootstrap extends BasicBootstrap
{
   /** The deployer */
   protected BasicXMLDeployer deployer;
   
   /**
    * Create a new bootstrap
    * 
    * @throws Throwable 
    */
   public UnitTestBootstrap() throws Throwable
   {
      super();
      bootstrap();
   }
   
   protected void bootstrap() throws Throwable
   {
      super.bootstrap();
      
      deployer = new BasicXMLDeployer(getKernel());
      
      /*
      Runtime.getRuntime().addShutdownHook(new Shutdown());
      
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      for (Enumeration<URL> e = cl.getResources(StandaloneKernelConstants.DEPLOYMENT_XML_NAME); e.hasMoreElements(); )
      {
         URL url = e.nextElement();
         deploy(url);
      }
      for (Enumeration<URL> e = cl.getResources("META-INF/" + StandaloneKernelConstants.DEPLOYMENT_XML_NAME); e.hasMoreElements(); )
      {
         URL url = e.nextElement();
         deploy(url);
      }
      
      // Validate that everything is ok
      deployer.validate();
      */
   }
   
   /**
    * Deploy a url
    *
    * @param url the deployment url
    * @throws Throwable for any error  
    */
   public void deploy(URL url) throws Throwable
   {
      KernelDeployment deployment = deployer.deploy(url);
      deployer.validate(deployment);
   }

   private KernelController getController()
   {
      return getKernel().getController();
   }
   
   /**
    * @param name
    */
   public <T> T lookup(String name, Class<T> expectedType) throws Throwable
   {
      KernelController controller = getController();
      ControllerContext context = controller.getContext(name, null);
      controller.change(context, ControllerState.INSTALLED);
      if(context.getError() != null)
         throw context.getError();
      
      if(context.getState() != ControllerState.INSTALLED) {
         System.err.println(context.getDependencyInfo().getUnresolvedDependencies(null));
      }
      // TODO: it can be stalled because of dependencies
      assert context.getState() == ControllerState.INSTALLED;
      
      return expectedType.cast(context.getTarget());
   }
   
   /**
    * Shutdown the UnitBootStrap.
    */
   public void shutdown()
   {
      deployer.shutdown();
   }
   
   /**
    * Undeploy a url
    * 
    * @param url the deployment url
    */
   public void undeploy(URL url)
   {
      try
      {
         deployer.undeploy(url);
      }
      catch (Throwable t)
      {
         log.warn("Error during undeployment: " + url, t);
      }
   }
}
