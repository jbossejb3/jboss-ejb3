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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jboss.aop.AspectManager;
import org.jboss.aop.microcontainer.beans.metadata.AspectManagerAwareBeanMetaDataFactory;
import org.jboss.beans.metadata.plugins.AbstractClassLoaderMetaData;
import org.jboss.beans.metadata.plugins.AbstractValueMetaData;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.BeanMetaDataFactory;
import org.jboss.beans.metadata.spi.ClassLoaderMetaData;
import org.jboss.beans.metadata.spi.ValueMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.dependency.spi.ScopeInfo;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.deployer.AbstractSimpleVFSRealDeployer;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.plugins.dependency.AbstractKernelControllerContext;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.metadata.spi.repository.MutableMetaDataRepository;
import org.jboss.metadata.spi.scope.CommonLevels;
import org.jboss.metadata.spi.scope.Scope;
import org.jboss.metadata.spi.scope.ScopeKey;

/**
 * Deployer for deployments containing AOP metadata. It makes sure that the metadata is deployed to the 
 * right aop domain in the case of scoped classloaders. The AOP metadata is stripped out of the deployment
 * to avoid deploying it twice.
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 * @deprecated @see EJBTHREE-1605
 */
@Deprecated
public abstract class AbstractAopMetaDataDeployer<T> extends AbstractSimpleVFSRealDeployer<T>
{
   private AspectManager aspectManager;
   private KernelController controller;
   private MyBeanMetaDataDeployer beanMetaDataDeployer = new MyBeanMetaDataDeployer();
   private static int sequence;
   
   
   public AbstractAopMetaDataDeployer(Class<T> input)
   {
      super(input);
      super.setStage(DeploymentStages.POST_CLASSLOADER);
      super.setOutput(AopMetaDataDeployerOutput.class);
   }

   /**
    * Get the aspectManager.
    * 
    * @return the aspectManager.
    */
   public AspectManager getAspectManager()
   {
      return aspectManager;
   }

   /**
    * Set the aspectManager.
    * 
    * @param aspectManager the aspectManager.
    */
   public void setAspectManager(AspectManager aspectManager)
   {
      this.aspectManager = aspectManager;
   }
   
   /**
    * Set the kernel.
    * 
    * @param kernel the kernel
    */
   public void setKernel(Kernel kernel)
   {
      this.controller = kernel.getController();
   }
   
   /**
    * Method for subclasses to call upon deployment 
    */
   @Override
   public void deploy(VFSDeploymentUnit unit, T deployment) throws DeploymentException
   {
      log.debug("Deploying " + unit + " " + deployment);
      
      AopMetaDataDeployerOutput output = new AopMetaDataDeployerOutput();
      unit.addAttachment(AopMetaDataDeployerOutput.class, output);
      
      if (extractAopBeanMetaDataFactories(unit, deployment, output))
      {
         AspectManager correctManager = unit.getAttachment(AspectManager.class);
         log.debug("Got AspectManager attachment " + correctManager + " for " + unit);
         if (correctManager != aspectManager)
         {
            int sequence = getSequence();
            String scopedManagerName = registerScopedManagerBean(sequence, unit, correctManager, output);
            massageScopedDeployment(sequence, unit, deployment, output, scopedManagerName);
         }
      }
      
      try
      {
         deployBeans(unit, output);
      }
      catch (Throwable t)
      {
         unregisterScopedManagerBean(output.getScopedAspectManagerBeanName(), false);
         if (t instanceof DeploymentException)
         {
            throw (DeploymentException)t;
         }
         else
         {
            throw new DeploymentException(t);
         }
      }
      log.debug("Finished deploying " + unit);
   }

   
   /**
    * Method for subclasses to call upon undeployment 
    */
   @Override
   public void undeploy(VFSDeploymentUnit unit, T deployment)
   {
      log.debug("Undeploying " + unit + " " + deployment);
      
      AopMetaDataDeployerOutput output = unit.getAttachment(AopMetaDataDeployerOutput.class);
      
      undeployBeans(unit, output);
   }

   protected abstract List<BeanMetaDataFactory> getFactories(T deployment);
   
   private boolean extractAopBeanMetaDataFactories(VFSDeploymentUnit unit, T deployment, AopMetaDataDeployerOutput output)
   {
      log.debug("Extracting aop bean metadata factories for  " + unit);
      List<AspectManagerAwareBeanMetaDataFactory> aopFactories = new ArrayList<AspectManagerAwareBeanMetaDataFactory>();
      
      List<BeanMetaDataFactory> factories = getFactories(deployment);
      if (factories != null && factories.size() > 0)
      {
         for (Iterator<BeanMetaDataFactory> it = factories.iterator() ; it.hasNext() ; )
         {
            BeanMetaDataFactory factory = it.next();
            if (factory instanceof AspectManagerAwareBeanMetaDataFactory)
            {
               it.remove();
               aopFactories.add((AspectManagerAwareBeanMetaDataFactory)factory);
            }
         }     
      }
      
      if (aopFactories.size() > 0)
      {
         output.setFactories(aopFactories);
         return true;
      }
      return false;
   }
   
   private String registerScopedManagerBean(int sequence, VFSDeploymentUnit unit, AspectManager scopedManager, AopMetaDataDeployerOutput output) throws DeploymentException
   {
      String name = "ScopedManager_" + getSequence() + "_" + unit.getName();
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(name, scopedManager.getClass().getName());
      unit.getMutableMetaData().addMetaData(scopedManager, AspectManager.class);
      
      try
      {
         controller.install(builder.getBeanMetaData(), scopedManager);
         return name;
      }
      catch (Throwable e)
      {
         throw new DeploymentException("Error registering scoped manager" + name + " " + scopedManager, e);
      }
   }
 
   private synchronized int getSequence()
   {
      return ++sequence;
   }
   
   private void unregisterScopedManagerBean(String name, boolean logError)
   {
      if (name == null)
      {
         return;
      }
      
      try
      {
         controller.uninstall(name);
      }
      catch(Throwable t)
      {
         if (logError)
         {
            log.debug("Error unregistering scoped aspect manager " + name, t);
         }
      }
   }
   
   private void massageScopedDeployment(int sequence, VFSDeploymentUnit unit, T deployment, AopMetaDataDeployerOutput output, String scopedManagerName) throws DeploymentException
   {
      log.debug("Massaging scoped deployment " + unit + " setting manager to " + scopedManagerName);
      String domainName = getDomainName(unit);
      output.setScopedInformation(scopedManagerName, domainName, sequence);
      unit.getMutableMetaData().addAnnotation(output.getScopeAnnotation());
   }
   
   private String getDomainName(VFSDeploymentUnit unit)
   {
      Module module = unit.getTopLevel().getAttachment(Module.class);
      if (module != null && !module.getDeterminedDomainName().equals(ClassLoaderSystem.DEFAULT_DOMAIN_NAME))
      {
         return module.getDeterminedDomainName();
      }
      
      return null;
   }
   
   private void deployBeans(VFSDeploymentUnit unit, AopMetaDataDeployerOutput output) throws DeploymentException
   {
      List<BeanMetaData> aopBeans = output.getBeans();
      List<BeanMetaData> done = new ArrayList<BeanMetaData>();
      try
      {
         if (aopBeans != null && aopBeans.size() > 0)
         {
            for (BeanMetaData bean : aopBeans)
            {
               //Register the component deployment context so we get the correct mutable metadata scope
               //This has been replaced by a "fake" implementation to avoid the overhead of the real one
               //which registers everything in JMX, which is pointless since we throw it away immediately
               FakeComponentUnit componentUnit = deployComponentDeploymentContext(unit, bean);
               try
               {
                  beanMetaDataDeployer.deploy(componentUnit, bean);
                  done.add(bean);
               }
               finally
               {
                  //Unregister the component deployment context so that this bean does not get deployed
                  //again by the real BeanMetaDataDeployer
                  undeployComponentDeploymentContext(componentUnit, bean);
               }
            }
         }
      }
      catch (Throwable t)
      {
         for (int i = done.size() - 1 ; i >= 0 ; i--)
         {
            try
            {
               beanMetaDataDeployer.undeploy(unit, done.get(i));
               controller.uninstall(done.get(i));
            }
            catch (Throwable e)
            {
               log.debug("Error undeploying " + done.get(i) + " for " + unit);
            }
         }
         throw new DeploymentException(t);
      }
   }
   
   private void undeployBeans(VFSDeploymentUnit unit, AopMetaDataDeployerOutput output)
   {
      if (output != null)
      {
         List<BeanMetaData> aopBeans = output.getBeans();
         if (aopBeans != null && aopBeans.size() > 0)
         {
            for (int i = aopBeans.size() - 1 ; i >= 0 ; i--)
            {
               BeanMetaData bean = aopBeans.get(i);
               beanMetaDataDeployer.undeploy(unit, bean);
            }
         }
      }
   }
   
   /**
    * Wrap the deployment unit in a component deployment unit similar to what the KernelDeploymentDeployer does
    */
   private FakeComponentUnit deployComponentDeploymentContext(VFSDeploymentUnit unit, BeanMetaData deployment)
   {
      //This used to make the following calls which has the overhead of registering the unit in JMX.
      //All we really want are the scopes, which is handled by FakeComponentUnit
//      DeploymentUnit componentUnit = unit.addComponent(deployment.getName());
//      componentUnit.addAttachment(BeanMetaData.class.getName(), deployment);
//      return componentUnit;
      return new FakeComponentUnit(unit, deployment);
   }
   
   /**
    * Undeploy the component deployment unit similar to what the KernelDeploymentDeployer does 
    */
   private void undeployComponentDeploymentContext(FakeComponentUnit unit, BeanMetaData bean)
   {
      unit.cleanup();
   }
   
   private class MyBeanMetaDataDeployer
   {   
      /**
       * Copied from the real org.jboss.deployers.vfs.deployer.kernel.BeanMetaDataDeployer
       */
      private void deploy(FakeComponentUnit unit, BeanMetaData deployment) throws DeploymentException
      {
         // No explicit classloader, use the deployment's classloader
         if (deployment.getClassLoader() == null)
         {
            try
            {
               // Check the unit has a classloader
               unit.getClassLoader();
               // TODO clone the metadata?
               deployment.setClassLoader(new DeploymentClassLoaderMetaData(unit));
            }
            catch (Exception e)
            {
               log.debug("Unable to retrieve classloader for deployment: " + unit.getName() + " reason=" + e.toString());
            }
         }
         KernelControllerContext context = new AbstractKernelControllerContext(null, deployment, null);
         //Make sure that the metadata from the deployment gets put into the context
         ScopeInfo scopeInfo = context.getScopeInfo();
         if (scopeInfo != null)
         {
            mergeScopes(scopeInfo.getScope(), unit.getScope());
            mergeScopes(scopeInfo.getMutableScope(), unit.getMutableScope());
         }
         
//         KernelControllerContext context = new AbstractKernelControllerContext(null, deployment, null);
//         ScopeInfo scopeInfo2 = context.getScopeInfo();
//         if (scopeInfo2 != null)
//         {
//            ScopeKey key = unit.getScope().clone();
//            key.removeScopeLevel(CommonLevels.INSTANCE);
//            key.addScope(CommonLevels.INSTANCE, deployment.getName());
//            
//            ScopeKey mutable = new ScopeKey();
//            key.addScope(CommonLevels.INSTANCE, deployment.getName());
//            
//            mergeScopes(scopeInfo2.getScope(), key);
//            mergeScopes(scopeInfo2.getMutableScope(), mutable);
//         }
         
         try
         {
            //System.out.println("==============> Installing " + context.getName());
            controller.install(context);
         }
         catch (Throwable t)
         {
            throw DeploymentException.rethrowAsDeploymentException("Error deploying: " + deployment.getName(), t);
         }
         
         
      }
   
      /**
       * Copied from the real org.jboss.deployers.vfs.deployer.kernel.BeanMetaDataDeployer
       * Merge scope keys.
       *
       * @param contextKey the context key
       * @param unitKey the unit key
       */
      protected void mergeScopes(ScopeKey contextKey, ScopeKey unitKey)
      {
         if (contextKey == null)
            return;
         if (unitKey == null)
            return;

         Collection<Scope> unitScopes = unitKey.getScopes();
         if (unitScopes == null || unitScopes.isEmpty())
            return;

         for (Scope scope : unitScopes)
            contextKey.addScope(scope);
      }

      /**
       * Copied from the real org.jboss.deployers.vfs.deployer.kernel.BeanMetaDataDeployer
       */
      private void undeploy(DeploymentUnit unit, BeanMetaData deployment)
      {
         try
         {
            controller.uninstall(deployment.getName());
            
            // Remove any classloader metadata we added (not necessary if we clone above)
            ClassLoaderMetaData classLoader = deployment.getClassLoader();
            if (classLoader instanceof DeploymentClassLoaderMetaData)
               deployment.setClassLoader(null);
         }
         catch(Throwable t)
         {
            log.info("Error undeploying " + deployment + " for " + unit);
         }
      }
   }
   
   /**
    * Copied from BeanMetaDataDeployer
    */
   private static class DeploymentClassLoaderMetaData extends AbstractClassLoaderMetaData
   {
      /** The serialVersionUID */
      private static final long serialVersionUID = 1L;
      
      /** The deployment unit */
      private FakeComponentUnit unit;

      /**
       * Create a new DeploymentClassLoaderMetaData.
       * 
       * @param unit the deployment unit
       */
      public DeploymentClassLoaderMetaData(FakeComponentUnit unit)
      {
         if (unit == null)
            throw new IllegalArgumentException("Null unit");
         this.unit = unit;
      }
      
      @Override
      public ValueMetaData getClassLoader()
      {
         return new AbstractValueMetaData(unit.getClassLoader());
      }
   }
   
   /**
    * BeanMetaDatadeployer uses AbstractComponentUnit per bean, but that has the overhead of
    * registering things in JMX. Create a Fake one here to encapsulate the methods that we use
    * without registering things in JMX
    * 
    * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
    * @version $Revision: 1.1 $
    */
   private static class FakeComponentUnit
   {
      VFSDeploymentUnit parent;
      BeanMetaData bmd;
      ScopeKey scope;
      ScopeKey mutableScope;
      
      FakeComponentUnit(VFSDeploymentUnit parent, BeanMetaData bmd)
      {
         this.parent = parent;
         this.bmd = bmd;
      }
      
      ScopeKey getScope()
      {
         if (scope == null)
         {
            ScopeKey key = parent.getScope().clone();
            key.removeScopeLevel(CommonLevels.INSTANCE);
            key.addScope(CommonLevels.INSTANCE, bmd.getName());
         }
         return scope;
      }
      
      ScopeKey getMutableScope()
      {
         if (mutableScope == null)
         {
            mutableScope = new ScopeKey();
            mutableScope.addScope(CommonLevels.INSTANCE, bmd.getName());
         }
         return mutableScope;
      }

      ClassLoader getClassLoader()
      {
         return parent.getClassLoader();
      }
      
      String getName()
      {
         return bmd.getName();
      }
      
      void cleanup()
      {
         MutableMetaDataRepository repository = null; 
         DeploymentUnit unit = parent;
         while (repository == null && unit != null)
         {
            repository = unit.getAttachment(MutableMetaDataRepository.class);
            unit = parent.getParent();
         }
         if (repository == null)
         {
            return;
         }
         
         try
         {
            ScopeKey scope = getScope();
            repository.removeMetaDataRetrieval(scope);
         }
         catch (Throwable ignored)
         {
         }

         try
         {
            ScopeKey scope = getMutableScope();
            repository.removeMetaDataRetrieval(scope);
         }
         catch (Throwable ignored)
         {
         }
      }
   }
}
