package org.neilja.net.interruptiblermi.demo;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import org.neilja.net.interruptiblermi.demo.client.LockServiceClientUI;
import org.neilja.net.interruptiblermi.demo.server.LockServiceServer;

/**
 * Entry point to the demo. Use {@link #main(String[])} to start both client and server.
 * 
 * @author neilotoole@apache.org
 * 
 */
public class InterruptibleRMIDemo
{

	/**
	 * Starts both the client UI and RMI server.
	 */
	public static void main(String[] args)
	{

		new LockServiceClientUI();

		new Thread(new Runnable()
		{

			public void run()
			{
				try
				{
					new LockServiceServer();
				}
				catch (RemoteException e)
				{
					e.printStackTrace();
				}
				catch (MalformedURLException e)
				{
					e.printStackTrace();
				}
			}

		}).start();
	}

}
