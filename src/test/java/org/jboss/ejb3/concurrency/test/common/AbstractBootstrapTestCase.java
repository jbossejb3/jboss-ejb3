/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.concurrency.test.common;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Properties;

import org.jboss.bootstrap.microcontainer.ServerImpl;
import org.jboss.bootstrap.spi.ServerConfig;
import org.jboss.bootstrap.spi.microcontainer.MCServer;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.client.spi.main.MainDeployer;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.vfs.spi.client.VFSDeployment;
import org.jboss.deployers.vfs.spi.client.VFSDeploymentFactory;
import org.jboss.logging.Logger;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public abstract class AbstractBootstrapTestCase
{
   private static Logger log = Logger.getLogger(AbstractBootstrapTestCase.class);
   
   private static MCServer server;
   private static MainDeployer mainDeployer;
   
   @AfterClass
   public static void afterClass() throws Exception
   {
      if(server != null)
      {
         if(server.isStarted())
            server.shutdown();
      }
   }
   
   @BeforeClass
   public static void beforeClass() throws Exception
   {
      server = new ServerImpl();
      
      Properties props = new Properties();
      String dir = mkdir("target/bootstrap");
      mkdir("target/bootstrap/server/default");
      //mkdir("target/bootstrap/server/default/deploy");
      mkdir("target/bootstrap/server/default/data");
      mkdir("target/bootstrap/server/default/log");
      mkdir("target/bootstrap/server/default/tmp");
      mkdir("target/bootstrap/server/default/tmp/deploy");
      mkdir("target/bootstrap/server/default/tmp/native");
      log.info("dir = " + dir);
      props.put(ServerConfig.HOME_DIR, dir);
      props.put(ServerConfig.SERVER_CONFIG_URL, findDir("src/test/resources/conf"));
      server.init(props);
      
      server.start();
      
      mainDeployer = (MainDeployer) server.getKernel().getController().getContext("MainDeployer", ControllerState.INSTALLED).getTarget();
      
//      URL url = new File("src/main/resources").toURI().toURL();
//      log.debug("url = " + url);
//      deploy(url);
      
      // TODO: hack, use something similar to deploy directory
//      Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources("META-INF/jboss-beans.xml");
//      while(urls.hasMoreElements())
//      {
//         URL u = urls.nextElement();
//         URLConnection c = u.openConnection();
//         // skip src/test/resources (urgh)
//         if(!(c instanceof JarURLConnection))
//            continue;
//         JarURLConnection connection = (JarURLConnection) c;
//         URL jarFileURL = connection.getJarFileURL();
//         deploy(jarFileURL);
//      }
      
      // TODO: another hack that simulates profile service going through deploy dir
      VirtualFile deployDir = VFS.getRoot(findDirURI("src/test/resources/deploy"));
      List<VirtualFile> candidates = deployDir.getChildren();
      for(VirtualFile candidate : candidates)
      {
         deploy(candidate);
      }
      
//      // Ehr, to deploy the persistence unit defined in src/test/resources (it's not a hack! it's a feature!)
//      deploy(AbstractBootstrapTestCase.class.getProtectionDomain().getCodeSource().getLocation());
   }
   
//   protected static void deploy(URL url) throws DeploymentException, IOException
//   {
//      VirtualFile root = VFS.getRoot(url);
//      VFSDeployment deployment = VFSDeploymentFactory.getInstance().createVFSDeployment(root);
//      mainDeployer.deploy(deployment);
//      mainDeployer.checkComplete(deployment);
//   }
   
   protected static void deploy(VirtualFile file) throws DeploymentException
   {
      VFSDeployment deployment = VFSDeploymentFactory.getInstance().createVFSDeployment(file);
      mainDeployer.deploy(deployment);
      mainDeployer.checkComplete(deployment);
   }
   
   private static String findDir(String path) throws IOException
   {
      return findDirURI(path).toString();
   }
   
   private static URI findDirURI(String path) throws IOException
   {
      File file = new File(path);
      boolean success = file.isDirectory();
      if(!success)
         throw new IOException("failed to find " + path);
      return file.toURI();
   }
   
   public <T> T getBean(String name, Class<T> expectedType)
   {
      // FIXME: check state
      return expectedType.cast(server.getKernel().getController().getContext(name, null).getTarget());
   }
   
   private static String mkdir(String path) throws IOException
   {
      File file = new File(path);
      boolean success = file.mkdirs() || file.isDirectory();
      if(!success)
         throw new IOException("failed to create " + path);
      return file.getAbsolutePath();
   }
}
