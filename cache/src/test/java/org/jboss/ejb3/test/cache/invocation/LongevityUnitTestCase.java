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
package org.jboss.ejb3.test.cache.invocation;

import org.jboss.ejb3.cache.Cache;
import org.jboss.ejb3.cache.LongevityCache;
import org.jboss.ejb3.cache.StatefulObjectFactory;
import org.jboss.ejb3.cache.impl.EntryStateCache;
import org.jboss.ejb3.cache.impl.SimpleLongevityCache;

import junit.framework.TestCase;

/**
 * The release of a bean is at tx completion. In the mean time
 * I can have multiple invocations on a bean.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision$
 */
public class LongevityUnitTestCase extends TestCase
{
   private Cache<MockIdentifiable> delegate;
   private LongevityCache<MockIdentifiable> cache;
   private Object key;
   
   /**
    * After setUp you have a delegate, a cache and a key.
    */
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      StatefulObjectFactory<MockIdentifiable> factory = new MockStatefulObjectFactory();
      this.delegate = new EntryStateCache<MockIdentifiable>(factory);
      this.cache = new SimpleLongevityCache<MockIdentifiable>(delegate);
      cache.start();
      
      MockIdentifiable bean = cache.create(null, null);
      this.key = bean.getId();
      cache.finished(bean);
      cache.release(bean);
   }
   
   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
   }
   
   public void test1()
   {
      // tx.begin();
      
      MockIdentifiable bean = cache.get(key);
      
      // blah blah
      
      cache.finished(bean);
      bean = null;
      
      // ...
      
      // next invocation
      
      bean = cache.get(key);
      
      // blah blah 2
      
      cache.finished(bean);
      
      // tx.commit() or rollback()
      
      cache.release(bean);
   }
   
   public void testDoubleGet()
   {
      // start
      
      MockIdentifiable bean = cache.get(key);
      assertNotNull(bean);
      
      try
      {
         cache.get(key);
         fail("expected IllegalStateException");
      }
      catch(IllegalStateException e)
      {
         // okay
      }
   }

   public void testReleaseBeforeFinish()
   {
      // start
      
      MockIdentifiable bean = cache.get(key);
      assertNotNull(bean);
      
      try
      {
         cache.release(bean);
         fail("expected IllegalStateException");
      }
      catch(IllegalStateException e)
      {
         // okay
      }
   }
}
