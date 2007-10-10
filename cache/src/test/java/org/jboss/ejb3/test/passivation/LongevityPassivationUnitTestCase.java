/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.test.passivation;

import org.jboss.ejb3.cache.LongevityCache;
import org.jboss.ejb3.cache.impl.FileObjectStore;
import org.jboss.ejb3.cache.impl.SimpleLongevityCache;
import org.jboss.ejb3.cache.impl.SimplePassivatingCache;
import org.jboss.ejb3.test.cache.common.CacheTestCase;

/**
 * Test the passivation on a longevity cache.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class LongevityPassivationUnitTestCase extends CacheTestCase
{
   public void test1() throws Exception
   {
      MockBeanContainer container = new MockBeanContainer();
      FileObjectStore<MockBeanContext> store = new FileObjectStore<MockBeanContext>();
      store.setStorageDirectory("./target/tmp/passivation");
      store.start();
      SimplePassivatingCache<MockBeanContext> delegate = new SimplePassivatingCache<MockBeanContext>(container, container, store);
      delegate.setName("MockBeanContainer");
      delegate.setSessionTimeout(1);
      LongevityCache<MockBeanContext> cache = new SimpleLongevityCache<MockBeanContext>(delegate);
      cache.start();
      
      MockBeanContext obj = cache.create(null, null);
      Object key = obj.getId();
      
      cache.finished(obj);
      
      cache.release(obj);
      obj = null;
      
      wait(container);
      
      assertEquals("MockBeanContext should have been passivated", 1, container.passivations);
      
      obj = cache.get(key);
      
      assertEquals("MockBeanContext should have been activated", 1, container.activations);
      
      cache.finished(obj);
      
      sleep(3000);
      
      assertEquals("MockBeanContext should not have been passivated", 1, container.passivations);
      
      cache.release(obj);
      obj = null;
      
      wait(container);
      
      assertEquals("MockBeanContext should have been passivated", 2, container.passivations);
      
      cache.remove(key);
   }
}
