/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.ejb3.concurrency.spi;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Class that represents a time period in a specified time unit
 *
 * @author Stuart Douglas
 */
public final class TimePeriod implements Serializable
{
   private final long value;
   private final TimeUnit timeUnit;

   public TimePeriod(long value, TimeUnit timeUnit)
   {
      this.value = value;
      this.timeUnit = timeUnit;
   }

   public TimeUnit getTimeUnit()
   {
      return timeUnit;
   }

   public long getValue()
   {
      return value;
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TimePeriod that = (TimePeriod) o;

      if (value != that.value) return false;
      if (timeUnit != that.timeUnit) return false;

      return true;
   }

   @Override
   public int hashCode()
   {
      int result = (int) (value ^ (value >>> 32));
      result = 31 * result + (timeUnit != null ? timeUnit.hashCode() : 0);
      return result;
   }

   @Override
   public String toString()
   {
      return "TimePeriod{" +
            "timeUnit=" + timeUnit +
            ", value=" + value +
            '}';
   }
}
