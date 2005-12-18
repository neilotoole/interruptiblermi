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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * 
 * Demo client application. Use {@link #main(String[])} to start the client.
 * 
 * 
 * @author neilotoole@apache.org
 *
 */
public class LockServiceClientUI
{

	/**
	 * Start the client UI.
	 */
	public static void main(String[] args)
	{
		/*
		 * UI code should run on the swing thread
		 */
		SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{

				new LockServiceClientUI();
			}

		});
		

	}


	private JDesktopPane desktop;
	private JFrame window;
	private JMenuItem newLockerMenuItem;
	private JMenuItem quitMenuItem;
	private JMenuItem selectedLafMenuItem;

	/**
	 * A list of the active Locker thingys. This list is maintained
	 * so that the the Locker thingys can be shutdown individually
	 * when the quit command is called.
	 */
	private List<LockerViewController> lockerControllers;
	

	public LockServiceClientUI()
	{
        try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		
		this.lockerControllers = new ArrayList<LockerViewController>();
		// set the title
		this.window = new JFrame("LockService Client");


		
		
		// the controller will add a listener later
		this.window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.window.addWindowListener(new WindowAdapter()
		{
		
			@Override @SuppressWarnings("unused")
			public void windowClosing(final WindowEvent e)
			{
				executeShutdown();
			}
		
		});
		
		this.window.setJMenuBar(this.createMenuBar());
		this.desktop = new JDesktopPane();
		this.desktop.setPreferredSize(new Dimension(640, 480));
		this.window.setContentPane(this.desktop);
		
		

		
		this.window.pack();
		this.window.setVisible(true);
		

		this.executeNewLockerViewController();
		this.executeNewLockerViewController();

		
	}



	private JMenuBar createMenuBar()
	{
		final JMenuBar menuBar = new JMenuBar();

		// Set up the menus.
		final JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_D);
		menuBar.add(fileMenu);
		menuBar.add(this.createLookAndFeelMenu());
		// Set up the "New Record" item
		this.newLockerMenuItem = new JMenuItem("New Lock Thingy");

		this.newLockerMenuItem.addActionListener(new ActionListener()
		{
			@SuppressWarnings("unused") 
			public void actionPerformed(final ActionEvent e)
			{
				executeNewLockerViewController();
			}
		});
		fileMenu.add(this.newLockerMenuItem);

		// Set up the "Quit" item
		this.quitMenuItem = new JMenuItem("Quit");
		this.quitMenuItem.addActionListener(new ActionListener()
		{
			@SuppressWarnings("unused") 
			public void actionPerformed(final ActionEvent e)
			{
				executeShutdown();
			}

		});
		fileMenu.add(this.quitMenuItem);

		return menuBar;
	}

	/**
     * Create and return the Look & Feel menu
     */
    private JMenu createLookAndFeelMenu() {
        JMenu lafMenu = new JMenu("Look & Feel");

        final UIManager.LookAndFeelInfo[] lafis = UIManager
                .getInstalledLookAndFeels();

        /*
         * For each look and feel, create a menu item. Associate an action with
         * each menu item (to execute the Look & Feel change). If the menu item
         * is for the current L&F, then set that menu item to selected.
         */

        for (UIManager.LookAndFeelInfo lafi : lafis) {
            final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(lafi
                    .getName());
            final String lafClass = lafi.getClassName();
            lafMenu.add(menuItem);

            if (lafClass
                    .equals(UIManager.getLookAndFeel().getClass().getName())) {
                menuItem.setSelected(true);
                this.selectedLafMenuItem = menuItem;
            }

            menuItem.addActionListener(new ActionListener() {
                @SuppressWarnings("unused")
                public void actionPerformed(final ActionEvent e) {
                    try {
                        if (LockServiceClientUI.this.selectedLafMenuItem != menuItem) {
                            /*
                             * Deselect the previous L&F menu item if
                             * appropriate
                             */
                        	LockServiceClientUI.this.selectedLafMenuItem
                                    .setSelected(false);
                        }

                        /*
                         * Select the current L&F menu item
                         */
                        LockServiceClientUI.this.selectedLafMenuItem = menuItem;
                        LockServiceClientUI.this.selectedLafMenuItem.setSelected(true);
                        UIManager.setLookAndFeel(lafClass);
                        SwingUtilities
                                .updateComponentTreeUI(LockServiceClientUI.this.window);
                        // update the preferences
   
                    } catch (final Exception ex) {
                        // should never happen
                        ex.printStackTrace();
                    }
                }
            });
        }

        return lafMenu;
    }

	/**
	 * Create a new locker thingy and display it on the desktop.
	 *
	 */
	private void executeNewLockerViewController()
	{
		final LockerViewController lc = new LockerViewController(this);
		this.lockerControllers.add(lc);
		this.desktop.add(lc.getFrame());
		try
		{
			lc.getFrame().setSelected(true);
		}
		catch (final PropertyVetoException e)
		{
			// never happens
		}
	}



	private void executeShutdown()
	{
		this.window.dispose();
		
		for(Iterator<LockerViewController> iter = this.lockerControllers.iterator(); iter.hasNext();)
		{
			LockerViewController lc = iter.next();
			iter.remove();
			lc.dispose();
		}
	}


	/**
	 * Called by a LockerViewController when it is exiting/dying.
	 * @param exitingLockViewController
	 */
	void lockerControllerClosing(LockerViewController exitingLockViewController)
	{
		this.lockerControllers.remove(exitingLockViewController);
	}
}
