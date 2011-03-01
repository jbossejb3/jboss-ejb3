/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.ejb3.cache.legacy;

import org.jboss.ejb3.cache.Identifiable;

/**
 * A legacy construct to help migration.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Deprecated
public interface StatefulBeanContext extends Identifiable {
    boolean getCanPassivate();

    boolean getCanRemoveFromCache();

    StatefulContainer getContainer();

    StatefulBeanContext getUltimateContainedIn();

    boolean isInUse();

    boolean isMarkedForPassivation();

    boolean isMarkedForReplication();

    boolean isRemoved();

    long lastUsed();

    void markForPassivation();

    void postActivate();

    void prePassivate();

    void setInUse(boolean inUse);

    void setMarkedForReplication(boolean markForReplication);
}
