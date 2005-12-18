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
package org.neilja.net.interruptiblermi.demo.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.Naming;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.neilja.net.interruptiblermi.InterruptibleRMIThreadFactory;
import org.neilja.net.interruptiblermi.demo.server.LockService;


/**
 * View-Controller for the "Locker" tool containing the UI to acquire/release the lock. The
 * constructor creates the UI widgets, which are all contained within a JInternalFrame instance
 * accessible via {@link #getFrame()}. The {@link #addComponentListeners()} method is invoked from
 * the constructor to add action listeners etc. to the UI components. The action listeners usually
 * invoke one of the <code>executeXXX</code> methods, which contain the calls to the "business
 * operations" (acquiring the lock, releasing the lock, etc.). The results of the business
 * operations are communicated back to the UI via the <code>handleXXX</code> methods, which update
 * the UI accordingly. The {@link #dispose()} method releases resources held by this object.
 * 
 * @author neilotoole@apache.org
 * 
 */
class LockerViewController
{
	/**
	 * The color green as used in the UI.
	 */
	static final Color DARK_GREEN = new Color(0, 150, 0);

	/**
	 * The color orange as used in the UI.
	 */
	static final Color DARK_ORANGE = new Color(150, 80, 0);


	/**
	 * The controllerCount variable is used to offset each subsequently created window so that new
	 * windows don't hide the old ones.
	 */
	private static int controllerCount = 0;

	private JInternalFrame frame;
	private JButton lockButton;
	private JButton unlockButton;
	private JButton cancelLockingButton;
	private JLabel infoLabel;


	private Thread lockingThread;
	private int currentLockCookie = -1;

	private JProgressBar progressBar;
	private final LockServiceClientUI parent;



	/**
	 * Create a new View-Controller object.
	 */
	LockerViewController(final LockServiceClientUI parent)
	{
		this.parent = parent;
		this.frame = new JInternalFrame("Lock Thingy");
		this.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		// set the window to show a little offset from the previous window
		this.frame.setBounds(10 + (controllerCount * 5), 10 + (controllerCount % 3) * 140, 400, 130);
		++controllerCount;
		this.frame.setClosable(true);
		this.frame.setIconifiable(true);
		this.frame.setMaximizable(false);
		this.frame.setResizable(false);

		final JPanel buttonPanel = new JPanel();

		this.lockButton = new JButton("Acquire Lock");

		this.cancelLockingButton = new JButton("Cancel Locking");
		this.cancelLockingButton.setEnabled(false);

		this.unlockButton = new JButton("Release Lock");
		this.unlockButton.setEnabled(false);


		this.infoLabel = new JLabel("You do not currently have the lock.");
		
		Border spacerBorder = BorderFactory.createEmptyBorder(3, 3, 3, 3);
		Border innerBorder = BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), spacerBorder);
		
		this.infoLabel.setBorder(BorderFactory.createCompoundBorder(
			spacerBorder, innerBorder));
		
		
		
		buttonPanel.add(this.lockButton);
		buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(this.cancelLockingButton);
		buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(this.unlockButton);

		this.frame.getContentPane().add(buttonPanel, BorderLayout.NORTH);
		this.frame.getContentPane().add(this.infoLabel, BorderLayout.SOUTH);


		this.progressBar = new JProgressBar(0, 100);
		this.progressBar.setValue(0);
		this.progressBar.setIndeterminate(true);
		this.progressBar.setStringPainted(false);
		this.progressBar.setString("");
		this.progressBar.setVisible(false);

		final JPanel progressPanel = new JPanel();
		progressPanel.add(this.progressBar);
		progressPanel.setMinimumSize(new Dimension(160, 20));

		this.frame.getContentPane().add(progressPanel, BorderLayout.CENTER);

		this.addComponentListeners();
		this.frame.setVisible(true);


	}



	/**
	 * Add the action listeners etc. to the UI components.
	 * 
	 */
	@SuppressWarnings("unused")
	private void addComponentListeners()
	{
		this.frame.addInternalFrameListener(new InternalFrameAdapter()
		{
			@Override
			public void internalFrameClosing(final InternalFrameEvent e)
			{
				dispose();

			}
		});

		this.lockButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				executeAcquireLock();
			}
		});

		this.unlockButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				executeReleaseLock();
			}
		});


		this.cancelLockingButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				executeCancelAcquiringLock();
			}
		});
	}



	private void executeAcquireLock()
	{
		this.lockButton.setEnabled(false);
		this.infoLabel.setForeground(Color.BLACK);
		this.infoLabel.setText("Trying to acquire lock!");
		this.progressBar.setVisible(true);


		final Runnable r = new Runnable()
		{
			public void run()
			{
				try
				{
					final LockService lockServiceProxy = (LockService) Naming.lookup("///LockServiceServer");

					final int lockCookie = lockServiceProxy.acquireLock();


					handleLockAcquired(lockCookie);

				}
				catch (final Exception e)
				{
					final boolean lockingWasInterrupted = Thread.interrupted();


					if (lockingWasInterrupted)
					{
						handleLockingWasInterrupted();
					}
					else
					{
						handleException(e);
					}

				}
			}
		};

		this.lockingThread = InterruptibleRMIThreadFactory.getInstance().newThread(r);

		this.lockingThread.start();

		this.cancelLockingButton.setEnabled(true);

	}



	private synchronized void handleLockingWasInterrupted()
	{
		SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{
				LockerViewController.this.progressBar.setVisible(false);
				LockerViewController.this.cancelLockingButton.setEnabled(false);

				LockerViewController.this.infoLabel.setForeground(DARK_ORANGE);
				LockerViewController.this.infoLabel.setText("Locking was cancelled - you don't have the lock");
				LockerViewController.this.lockButton.setEnabled(true);
			}

		});
	}



	private synchronized void handleException(final Exception e)
	{
		SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{
				JOptionPane.showInternalMessageDialog(LockerViewController.this.frame, e,
					"Exception!", JOptionPane.ERROR_MESSAGE);
				LockerViewController.this.frame.dispose();
			}

		});

	}



	private synchronized void handleLockAcquired(final int lockCookie)
	{
		SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{
				LockerViewController.this.cancelLockingButton.setEnabled(false);
				LockerViewController.this.progressBar.setVisible(false);
				LockerViewController.this.currentLockCookie = lockCookie;
				LockerViewController.this.lockingThread = null;
				LockerViewController.this.infoLabel.setForeground(DARK_GREEN);
				LockerViewController.this.infoLabel.setText("You currently have the lock! [value="
					+ LockerViewController.this.currentLockCookie + "]");

				LockerViewController.this.unlockButton.setEnabled(true);
			}

		});


	}



	private synchronized void handleLockReleased()
	{
		SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{
				LockerViewController.this.currentLockCookie = -1;
				LockerViewController.this.infoLabel.setForeground(Color.BLACK);
				LockerViewController.this.infoLabel.setText("You have released the lock.");
				LockerViewController.this.lockButton.setEnabled(true);
			}

		});
	}



	void dispose()
	{
		LockerViewController.this.frame.dispose();

		if (this.lockingThread != null)
		{
			this.lockingThread.interrupt();
		}
		if (LockerViewController.this.currentLockCookie != -1)
		{
			executeReleaseLock();
		}

		this.parent.lockerControllerClosing(this);
	}



	private synchronized void executeReleaseLock()
	{
		this.unlockButton.setEnabled(false);

		final Runnable r = new Runnable()
		{
			public void run()
			{
				try
				{
					final LockService lockServiceProxy = (LockService) Naming.lookup("///LockServiceServer");

					lockServiceProxy.releaseLock(LockerViewController.this.currentLockCookie);

					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							handleLockReleased();
						}

					});
				}
				catch (final Exception e)
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							handleException(e);
						}

					});
				}
			}
		};

		Thread unlockingThread = InterruptibleRMIThreadFactory.getInstance().newThread(r);
		unlockingThread.start();

	}



	private synchronized void executeCancelAcquiringLock()
	{
		if (this.lockingThread == null)
		{
			throw new IllegalStateException("Locking Thread is null!");
		}

		this.lockingThread.interrupt();

	}



	JInternalFrame getFrame()
	{
		return this.frame;
	}

}
