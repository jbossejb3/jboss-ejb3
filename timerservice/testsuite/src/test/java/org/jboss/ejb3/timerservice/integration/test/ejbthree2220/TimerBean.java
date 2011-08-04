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
package org.jboss.ejb3.timerservice.integration.test.ejbthree2220;

import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * TimerBean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@Remote(TimerTester.class)
@RemoteBinding(jndiBinding = TimerBean.JNDI_NAME)
public class TimerBean implements TimerTester {

    private static Logger logger = Logger.getLogger(TimerBean.class);

    public static final String JNDI_NAME = "ejbthree-2220-timer-bean-remote";

    @Resource
    private TimerService timerService;

    private int numTimeouts;

    private List<Date> timeouts = new ArrayList<Date>();

    @Override
    public void createIntervalTimer(long initialDuration, long intervalDuration, Serializable info) {
        TimerConfig config = new TimerConfig();
        config.setPersistent(false);
        config.setInfo(info);
        logger.info("Creating interval timer with initialDuration = " + initialDuration + " interval duration = " + intervalDuration + " and info = " + info);
        this.timerService.createIntervalTimer(initialDuration, intervalDuration, config);
    }

    @Timeout
    public void onTimeout() {
        this.timeouts.add(new Date());
        this.numTimeouts++;
    }

    @Override
    public Date getFirstTimeout() {
        if (this.timeouts.isEmpty()) {
            return null;
        }
        return this.timeouts.get(0);
    }
}
