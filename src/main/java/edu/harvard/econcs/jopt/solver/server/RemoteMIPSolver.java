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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.IMIPSolver;
import edu.harvard.econcs.jopt.solver.MIPException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A remote solver that solves a MIP based on a local IMIPSolver
 * 
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.7 $ on $Date: 2010/10/28 00:11:26 $
 * @since Apr 12, 2004
 **/
public class RemoteMIPSolver extends UnicastRemoteObject implements IRemoteMIPSolver {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3257572810506646068L;

	private static final Logger logger = LogManager.getLogger(RemoteMIPSolver.class);
	
	private IMIPSolver solver;
	
	public RemoteMIPSolver(int port, IMIPSolver solver) throws RemoteException {
		super(port);
		this.solver = solver;
	}
	
	/**
	 * @see edu.harvard.econcs.jopt.solver.server.IRemoteMIPSolver#solve(byte[])
	 */
	public IMIPResult solve(byte[] serializedMip) throws MIPException /*, RemoteException */ {
		String client = "Unknown";
		try {
			client = getClientHost();
		} catch (ServerNotActiveException e) {
			logger.warn("Could not get client host: " + e.getMessage());
		}
		ObjectInputStream ois;
		IMIP mipObj = null;
		try {
			logger.trace("Begin de-serialization of " + serializedMip.length + " bytes from " + client);
			long time = System.currentTimeMillis();
			ois = new ObjectInputStream(new ByteArrayInputStream(serializedMip));
			mipObj = (IMIP)ois.readObject();
			time = System.currentTimeMillis() - time;
			logger.trace("Finished de-serialiation in " + time + " millis.");
		} catch (IOException e) {
			throw new MIPException("Serialization error", e);
		} catch (ClassNotFoundException e) {
			throw new MIPException("Serialization error", e);			
		}
		long time = System.currentTimeMillis();
		IMIPResult ret = solver.solve(mipObj);
		time = System.currentTimeMillis() - time;
		logger.trace("Finished solving MIP for '" + client + "' in " + time + "millis, sending results back");
		return ret;
	}
}
