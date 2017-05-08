/*
 * Copyright (c) 2005
 *	The President and Fellows of Harvard College.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package edu.harvard.econcs.jopt.solver.server.cplex;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.harvard.econcs.jopt.solver.MIPException;
import edu.harvard.econcs.util.Log;

/**
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.7 $ on $Date: 2013/12/03 23:24:23 $
 * @since Apr 12, 2004
 **/
public class InstanceManager {
	private static Log log = new Log(InstanceManager.class);

	private static int numSimultaneous = 25;
	private static InstanceManager instance = new InstanceManager();
	
	private Set<IloCplex> available = new HashSet();
	private Set<IloCplex> inUse = new HashSet();
	
	private InstanceManager() {}

	public static InstanceManager getInstance() {
		return instance;
	}
	
	public static void setNumSimultaneous(int numSimultaneous) {
		InstanceManager.numSimultaneous = numSimultaneous;
	}

	public synchronized IloCplex checkOutCplex() {
		while (notAvailable()) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				log.error("Interrupted while trying to get IloCPlex, resetting", e);
				for(Iterator iter = inUse.iterator(); iter.hasNext(); ) {
					IloCplex c = (IloCplex)iter.next();
					try  {
						c.end();
					} catch(RuntimeException ex) {
						log.error("Exception trying to close CPLEX", ex);
					}
				}
				inUse.clear();
			}
		}
		
		IloCplex cplex = getCplex();
		inUse.add(cplex);
		return cplex;
	}
	
	private boolean notAvailable() {
		return inUse.size() >= numSimultaneous;
	}

	private IloCplex getCplex() {
		for(int i=0; i<100; i++) {
			if (!available.isEmpty()) {
				IloCplex ret = (IloCplex)available.iterator().next();
				available.remove(ret);
				return ret;
			}
			if(available.size()+inUse.size()<numSimultaneous) {
				IloCplex ret = createCplex();
				if (ret!=null) {
					return ret;
				}
			}
		}
		throw new MIPException("Could not obtain cplex instance");
	}
	
	private IloCplex createCplex() {
		for (int i=0; i<10; i++) {
			try {
				IloCplex cplex;
				cplex = new IloCplex();
				return cplex;
			} catch (IloException ex) {
				if(i<9) {
					log.warn("Could not get CPLEX instance, retrying", ex);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					log.warn("Could not get CPLEX instance, giving up", ex);
					return null;
				}
			}
		}
		return null;
	}
	
	public synchronized void checkInCplex(IloCplex cplex) {
		if (cplex == null) {
			return;
		}
		try {
			cplex.clearModel();
		} catch (IloException e) {
			log.error("Exception clearing model: " + e.getMessage(), e);
			cplex.end();
			inUse.remove(cplex);
			this.notify();
			return;
		}
		inUse.remove(cplex);
		available.add(cplex);
		this.notify();
	}
}
