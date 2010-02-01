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

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Logger;

import org.neilja.net.interruptiblermi.InterruptibleRMISocketFactory;

/**
 * Implements the {@link org.neilja.net.interruptiblermi.demo.server.LockService} RMI server.
 * 
 * @author neilotoole@apache.org
 * 
 */
public class LockServiceServer implements LockService
{
	/**
	 * The RMI name which this server will be bound to.
	 */
	public static final String BINDING_NAME = "LockServiceServer";



	/**
	 * Start the RMI server.
	 */
	public static void main(String[] args)
	{
		try
		{
			new LockServiceServer();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}


	private final Random random = new Random();

	/**
	 * Flag to indicate that the server is shutting down.
	 */
	private boolean isShuttingDown = false;

	/**
	 * The "cookie" value for the current lock, always a positive integer. If the value is -1, that
	 * means that there is no current lock.
	 */
	private int lockCookie = -1;



	/**
	 * Create a new RMI server, binding this object under the name {@link #BINDING_NAME}.
	 */
	public LockServiceServer() throws RemoteException, MalformedURLException
	{
		Logger.global.info(Thread.currentThread().getName() + "  -> Starting RMI...");

		/*
		 * Create the registry on the standard port.
		 */
		LocateRegistry.createRegistry(Registry.REGISTRY_PORT);

		/*
		 * Create the RMI socket factory that provides our "interrupible RMI" support. Then export
		 * this object using the factory.
		 */
		final InterruptibleRMISocketFactory socketFactory = new InterruptibleRMISocketFactory();
		final LockService stub = (LockService) UnicastRemoteObject.exportObject(this, 0,
			socketFactory, socketFactory);

		/*
		 * Bind the stub using the binding name.
		 */
		Naming.rebind(BINDING_NAME, stub);
		Logger.global.info(Thread.currentThread().getName() + "  -> RMI Server started.");


		/*
		 * Listen for shell input (to shutdown server, etc.).
		 */

		new Thread(new Runnable()
		{

			public void run()
			{
				LockServiceServer.this.listenForShellInput();
			}

		}).start();


	}



	/**
	 * Listen for command line input to shutdown the server. If the user inputs "exit", the server
	 * will shutdown. Also, if the user types "unlock", the current lock will be released.
	 * 
	 */
	void listenForShellInput()
	{
		final Scanner scanner = new Scanner(System.in);

		String input = null;

		do
		{
			input = scanner.nextLine();

			if (input.equalsIgnoreCase("unlock"))
			{
				Logger.global.info(Thread.currentThread().getName()
					+ "  -> Manually releasing lock with value: " + this.lockCookie);
				releaseLock(this.lockCookie);
				continue;
			}
			else if (input.equalsIgnoreCase("exit"))
			{
				break;
			}
			else if (input.trim().length() == 0)
			{
				continue;
			}
			else
			{
				System.out.println("Unknown command. Type 'unlock' (and enter) to release lock.");
				System.out.println("Type 'exit' (and enter) to stop the server.");
			}
		}
		while (true);


		Logger.global.info(Thread.currentThread().getName() + "  -> Shutting down the server...");

		// time to shutdown...
		try
		{
			this.dispose();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}


	}



	/**
	 * Stops the RMI server.
	 */
	public synchronized void dispose() throws RemoteException, MalformedURLException, NotBoundException
	{
		this.isShuttingDown = true;

		Logger.global.info(Thread.currentThread().getName() + "  -> Disposing of RMI resources.");
		Naming.unbind(BINDING_NAME);
		UnicastRemoteObject.unexportObject(this, true);
		this.releaseLock(this.lockCookie);
	}



	/**
	 * Acquire the lock, blocking until the lock becomes available, and return a "lock cookie". This
	 * method can throw a RuntimeException if the calling thread's RMI socket has been closed while
	 * the thread was waiting, or if the server has started the shutdown process.
	 * 
	 * @see LockService#acquireLock()
	 */
	public synchronized int acquireLock()
	{
		Logger.global.info(Thread.currentThread().getName() + " -> entering wait loop.");


		while (this.lockCookie != -1)
		{
			/*
			 * If the lockCookie is set to a value other than -1, then the lock has already been
			 * assigned. This thread needs to wait until the lock has become available.
			 */
			try
			{
				/*
				 * Give up the monitor and wait.
				 */
				wait();

				/*
				 * Another thread has called #notify / #notifyAll, and this thread had the monitor.
				 * Typically this means that this thread now gets to acquire the lock, but first we
				 * have to check that this thread is not "zombie thread". First we check if the RMI
				 * socket that spawned this thread is still open. Then we check that the server
				 * isn't shutting down.
				 */
				if (InterruptibleRMISocketFactory.isCurrentRMIServerThreadSocketAlive() == false)
				{
					throw new RuntimeException("This thread's RMI socket is dead!");
				}
				if (this.isShuttingDown)
				{
					throw new RuntimeException("Server is shutting down forcefully!");
				}
			}
			catch (InterruptedException e)
			{
				// doesn't happen
				e.printStackTrace();
			}
		}

		/*
		 * Generate a random lock cookie value to return to the client.
		 */

		this.lockCookie = Math.abs(this.random.nextInt());
		Logger.global.info(Thread.currentThread().getName() + " -> returning lock cookie: "
			+ this.lockCookie);
		return this.lockCookie;
	}



	/**
	 * Release the lock held by the client with the given lock cookie.
	 * 
	 * @see LockService#releaseLock(int)
	 * @param lockCookie
	 *            the value previously returned by {@link #acquireLock()}
	 * @throws SecurityException
	 *             if lockCookie does not match the server's current lock cookie
	 */
	public synchronized void releaseLock(final int lockCookie) throws SecurityException
	{

		if (lockCookie != this.lockCookie)
		{
			throw new SecurityException(
				"The supplied lock cookie does not match the server's lock cookie.");
		}

		Logger.global.info(Thread.currentThread().getName()
			+ " -> releasing lock with lockCookie value: " + this.lockCookie);

		/*
		 * -1 indicates that the lock is now available.
		 */
		this.lockCookie = -1;

		/*
		 * Wake up any waiting threads and give them a chance to acquire the lock.
		 */
		this.notifyAll();
	}
}
