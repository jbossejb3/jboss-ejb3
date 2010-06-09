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
package org.jboss.ejb3.timerservice.integration.test.autotimer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Singleton;
import javax.ejb.Timer;

/**
 * TimeoutTrackerForSLSB
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
public class TimeoutTrackerForSLSB
{
   private Map<String, Integer> numTimeouts = new HashMap<String, Integer>();;

   private Map<String, List<Date>> timeouts = new HashMap<String, List<Date>>();

   public int getNumberOfTimeouts(String timeoutMethodName)
   {
      return this.numTimeouts.get(timeoutMethodName);
   }

   public List<Date> getTimeouts(String methodName)
   {
      return this.timeouts.get(methodName);
   }

   public void trackTimeout(String methodName, Date when)
   {
      List<Date> timeoutsForMethod = this.timeouts.get(methodName);
      if (timeoutsForMethod == null)
      {
         timeoutsForMethod = new ArrayList<Date>();
      }
      timeoutsForMethod.add(when);

      Integer numTimeoutForMethod = this.numTimeouts.get(methodName);
      if (numTimeoutForMethod == null)
      {
         numTimeoutForMethod = 0;
      }
      this.numTimeouts.put(methodName, numTimeoutForMethod + 1);
   }

}
