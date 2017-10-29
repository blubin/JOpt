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
package edu.harvard.econcs.jopt.solver.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.MIPException;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A special kind of SolverServer that knows about a bunch of
 * other Solver servers and can load balance among them.
 * 
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.5 $ on $Date: 2010/10/28 00:11:26 $
 * @since Apr 30, 2004
 **/
public class SolverLoadBalancer extends UnicastRemoteObject
		implements ISolverServer {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3545516201121167412L;

	private static final Logger logger = LogManager.getLogger(SolverLoadBalancer.class);
	
	private Properties props;
	
	private List clients = new ArrayList();
	private List tryLater = new ArrayList();
	
	/**
	 * Create a new Server
	 */
	public static void createServer(int port, Properties props) throws MIPException {
		try {
			logger.info("Binding load balancer to port: " + port);
			Registry localreg = LocateRegistry.createRegistry(port);
			SolverLoadBalancer server = new SolverLoadBalancer(port, props);
			localreg.bind(NAME, server);
		} catch (AccessException e) {
			throw new MIPException("Access", e);
		} catch (AlreadyBoundException e) {
			throw new MIPException("AlreadyBound",e);
		} catch (RemoteException e) {
			throw new MIPException("RemoteException",e);
		}
	}

	protected SolverLoadBalancer(int port, Properties props) throws RemoteException {
		super(port);
		this.props = props;
		createClients();
	}

	private void createClients() {
		for (int i=0; true; i++) {
			String hostKey = Integer.toString(i)+".host";
			String portKey = Integer.toString(i)+".port";
			String host = props.getProperty(hostKey);
			String portStr = props.getProperty(portKey);
			if (host==null | portStr==null) {
				return;
			}
			int p = Integer.parseInt(portStr);
			SolverClient cl = null;
			try {
				cl = createSolverClient(host, p);
			} catch (MIPException e) {
				logger.warn("Could not connect to: " + host + ":" + p + " Error: " + e.getMessage());
				tryLater.add(new ServerLoc(host, p));
			}
			if (cl != null) {
				clients.add(cl);
			}
		}
	}
		
	protected synchronized int getNumClients() {
		return clients.size();
	}
	
	protected synchronized SolverClient getClient(int idx) {
		if (idx > clients.size()-1 || idx < 0) {
			return null;
		}
		return (SolverClient)clients.get(idx);
	}
	
	protected synchronized void removeClient(SolverClient cl) {
		clients.remove(cl);
		tryLater.add(new ServerLoc(cl.getHost(), cl.getPort()));
	}
	
	/** 
	 * Try to move the contents of the tryLater list back to the
	 * clients list
	 * @return true iff a client was re-added to the clients list.
	 */
	protected synchronized boolean retryDeadDrones() {
		boolean ret = false;
		for (Iterator iter = tryLater.iterator(); iter.hasNext(); ) {
			ServerLoc loc = (ServerLoc)iter.next();
			logger.info("Trying to reconnect to: " + loc);
			SolverClient cl = null;
			try {
				cl = createSolverClient(loc.host, loc.port);
			} catch (MIPException e) {
				logger.warn("Could not connect to: " + loc + " Error: " + e.getMessage());
			}
			if (cl != null) {
				clients.add(cl);
				iter.remove();
				ret |= true;
			}
		}
		return ret;
	}
	
	/**
	 * @see edu.harvard.econcs.jopt.solver.server.ISolverServer#getSolver()
	 */
	public IRemoteMIPSolver getSolver() throws RemoteException {
		String client = "Unknown";
		try {
			client = getClientHost();
		} catch (ServerNotActiveException e) {
			logger.warn("Could not get client host: " + e.getMessage());
		}
		logger.info("Creating Load Balancing Remote Solver for: " + client);
		return new BalancingRemoteMIPSolver();
	}
	
	private class BalancingRemoteMIPSolver extends UnicastRemoteObject implements IRemoteMIPSolver {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4050197531583590452L;
		public BalancingRemoteMIPSolver() throws RemoteException{
		    /* keep compiler happy */
		}
		public IMIPResult solve(byte[] serializedMip) throws MIPException /*, RemoteException */{
			initStartingPlace();
			while (true) {
				SolverClient cl = getNextClient();
				if (cl == null) {
					throw new MIPException("Could not find a solver to solve problem: all servers down");
				}
				logger.info("Attempting to solve using: " + cl.getHost() + ":" + cl.getPort());
				try {
					long time = System.currentTimeMillis();
					IMIPResult ret = cl.solve(serializedMip);
					time = System.currentTimeMillis() - time;
					logger.info("MIP solved in " + time + " by " + cl.getHost() + ":" + cl.getPort());
					return ret;
				} catch (MIPException e) {
					Throwable t = e.getCause();
					if (t instanceof RemoteException) {
						logger.error("Remote Exception", t);
						clientDead(cl);
					} else {
						logger.error("Exception from solver", e);
						throw e;
					}
				}
			}
		}
		private int curSpot = 0;
		private int activeRemaining = 0;
		private void initStartingPlace() {
			// randomly re-lookup now and again)
			if (getRand(10) == 0) {
				retryDeadDrones();
			}
			
			int numClients = getNumClients();
			curSpot = getRand(numClients);
			activeRemaining = numClients;
		}
		
		private SolverClient getNextClient() {
			int numClients = getNumClients();
			if (activeRemaining > numClients) {
				activeRemaining = numClients;
			}
			if (activeRemaining == 0) {
				if (retryDeadDrones()) {
					initStartingPlace();
				} else {
					return null;
				}
			}
			while (getNumClients()>0) {
				SolverClient ret = getClient(curSpot);
				curSpot = (curSpot++)%getNumClients();
				if (ret != null) {
					activeRemaining--;
					return ret;
				}
			}
			return null;
		}
		
		private void clientDead(SolverClient cl) {
			logger.warn("Client died: " + cl.getHost() + ":" + cl.getPort());
			removeClient(cl);
			curSpot--;
		}
	}
	
	/** returns a value between 0 and range -1 or -1 if range==0**/
	protected int getRand(int range) {
		if (range == 0) {
			return -1;
		}
		//Dumb random function, but its all we need for our purposes.
		return (int) ((System.currentTimeMillis()/1000) % range);
	}
	
	private static class ServerLoc {
		public String host;
		public int port;
		public ServerLoc(String host, int port) {
			this.host = host;
			this.port = port;
		}
		public String toString() {
			return host + ":" + port;
		}
	}
	
	private SolverClient createSolverClient(String host, int port) {
		return new SolverClient(host, port);
	}
	
	public static void main(String argv[]) throws IOException {
		if (argv.length != 2) {
			logger.error("Usage: <port> <config file>");
			System.exit(1);
		}
		int port = Integer.parseInt(argv[0]);
		Properties pr = new Properties();
		pr.load(new FileInputStream(argv[1]));
		createServer(port, pr);
	}
}
