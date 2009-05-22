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

import org.jboss.ejb3.test.tx.mc.UnitTestBootstrap;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public abstract class AbstractTxTestCase
{
   // don't muck into this field
   protected static UnitTestBootstrap bootstrap = null;
   
   @AfterClass
   public static void afterClass()
   {
      if(bootstrap != null)
         bootstrap.shutdown();
   }
   
   @BeforeClass
   public static void beforeClass() throws Throwable
   {
      // JBossXB is very strict with xml content ordering. This
      // is a workaround to prevent failures of testcase because
      // of xml contents
      System.setProperty("xb.builder.useUnorderedSequence", "true");
      
      bootstrap = new UnitTestBootstrap();
      bootstrap.deploy(getResource("instance/classloader.xml"));
      bootstrap.deploy(getResource("instance/aop.xml"));
      bootstrap.deploy(getResource("instance/deployers.xml"));
      bootstrap.deploy(getResource("instance/beans.xml"));
   }
   
   protected static URL getResource(String name)
   {
      return Thread.currentThread().getContextClassLoader().getResource(name);
   }
}
