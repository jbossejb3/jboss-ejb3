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
package org.jboss.ejb3.tx2.spi;

import java.io.Serializable;

/**
 * Class that stores the details of application exceptions
 *
 * @see javax.ejb.ApplicationException
 * @author Stuart Douglas
 */
public final class ApplicationExceptionDetails implements Serializable
{

   private final boolean rollback;
   private final boolean inherited;

   public ApplicationExceptionDetails(boolean rollback, boolean inherited)
   {
      this.rollback = rollback;
      this.inherited = inherited;
   }

   public boolean isInherited()
   {
      return inherited;
   }

   public boolean isRollback()
   {
      return rollback;
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ApplicationExceptionDetails that = (ApplicationExceptionDetails) o;

      if (inherited != that.inherited) return false;
      if (rollback != that.rollback) return false;

      return true;
   }

   @Override
   public int hashCode()
   {
      int result = (rollback ? 1 : 0);
      result = 31 * result + (inherited ? 1 : 0);
      return result;
   }

   @Override
   public String toString()
   {
      return "ApplicationExceptionDetails{" +
            "inherited=" + inherited +
            ", rollback=" + rollback +
            '}';
   }
}
