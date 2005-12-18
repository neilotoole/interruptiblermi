/**
 * Copyright 2005 Neil O'Toole - neilotoole@apache.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.neilja.net.interruptiblermi.demo.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface to acquire and release an exclusive lock. When a client thread
 * calls {@link #acquireLock()}, the method will block until the lock becomes
 * available. When the exclusive lock has been acquired, the method returns
 * an integer "lock cookie" which is a sort of security token to identify the lock.
 * The lock may be released by calling {@link #releaseLock(int)} with the valid
 * "lock cookie".
 * 
 * @author neilotoole@apache.org
 *
 */
public interface LockService extends Remote
{
	/**
	 * Acquire the exclusive lock, blocking until the lock becomes available, and
	 * returning a "lock cookie" uniquely identifying the exclusive lock.
	 * @return the "lock cookie" for this lock.
	 * @throws RemoteException if an RMI-related exception occurs
	 * @throws RuntimeException if the server shuts down while the calling thread
	 * is blocking in the method, of if the calling client has shut down the RMI conneciton.
	 */
	int acquireLock() throws RemoteException;
	
	/**
	 * Release the exclusive lock, supplying the lock cookie returned by {@link #acquireLock()}.
	 * @param lockCookie the value returned by {@link #acquireLock()}
	 * @throws RemoteException if an RMI-related exception occurs
	 * @throws SecurityException if the supplied lock cookie is not the value associated
	 * with the current lock.
	 */
	void releaseLock(final int lockCookie) throws RemoteException, SecurityException;
}
