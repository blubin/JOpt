/*
 * Copyright (c) 2005-2017 Benjamin Lubin
 * Copyright (c) 2005-2017 The President and Fellows of Harvard College
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * - Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.harvard.econcs.jopt.solver.server.cplex;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.7 $ on $Date: 2013/12/03 23:24:23 $
 * @since Apr 12, 2004
 **/
public enum CPLEXInstanceManager {
    INSTANCE;
    private static final Logger LOGGER = LoggerFactory.getLogger(CPLEXInstanceManager.class);

    private int numSimultaneous = 100;
    private BlockingQueue<IloCplex> available = new LinkedBlockingQueue<IloCplex>();
    private AtomicInteger inUseCount = new AtomicInteger();

    public void setNumSimultaneous(int numSimultaneous) {
        this.numSimultaneous = numSimultaneous;
    }

    public IloCplex checkOutCplex() {
        IloCplex cplex = available.poll();
        if (cplex == null) {
            if (inUseCount.getAndIncrement() < numSimultaneous) {
                available.offer(createCplex());
            }
            inUseCount.decrementAndGet();
            try {
                cplex = available.take();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while trying to get IloCPlex, resetting", e);
                throw new RuntimeException(e);
            }
        }
        inUseCount.incrementAndGet();
        return cplex;
    }

    private IloCplex createCplex() {
        for (int i = 0; i < 10; i++) {
            try {
                return new IloCplex();
            } catch (IloException ex) {
                if (i < 9) {
                    LOGGER.warn("Could not get CPLEX instance, retrying", ex);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    LOGGER.warn("Could not get CPLEX instance, giving up", ex);
                    return null;
                }
            }
        }
        return null;
    }

    public void checkInCplex(IloCplex cplex) {
        if (cplex == null) {
            return;
        }
        try {
            cplex.getParameterSet().clear();
            cplex.clearCallbacks();
            cplex.clearModel();
        } catch (IloException e) {
            LOGGER.error("Exception clearing model: " + e.getMessage(), e);

            cplex.end();
            inUseCount.decrementAndGet();
            return;
        }
        available.offer(cplex);
        inUseCount.decrementAndGet();
    }
}
