package net.ddns.tuxchen;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.text.*;
import java.util.regex.*;

/**
 * This is the main class of the fun server
 * 
 * @author Tuxchen
 *
 */
public class CTF extends JFrame {
	private Container c;
	private JTabbedPane tabs;
	private final Font buttonFont = new Font("Arial", Font.BOLD, 16);
	private final Font textFont = new Font("Courier New", Font.PLAIN, 15);
	private ServerSocket server;
	private Socket client;
	private boolean isrun;
	private String msg;
	private PrintWriter write;
	private Socket sock;
	private Pattern pattern;
	private Matcher matcher;
	private List<String> banlist;
	private boolean boolchat;
	
	// Toolbar and the widgets of it
	private JToolBar tb;
	private JButton go, stop;
	private JComboBox port;
	private JCheckBox enableChat;
	private JComboBox cmb_maxUsers;
	private JLabel lbl_maxUsers;
	
	// Elements of tab 1
	private JPanel panel1;
	private JTextArea logarea;
	private JScrollPane sp;
	
	// Elements of tab 2
	private JPanel panel2;
	private JButton kick, ban, send;
	private JComboBox clients;
	private DefaultListModel<Socket> clientList;
	
	private static String winCode;
	private static String startPath;
	private static String secureFile;
	
	/**
	 * This is the thread, that handles the server protocol
	 */
	private class ServerProtocol extends Thread {
		private Socket client;
		private BufferedReader sin;
		private PrintWriter sout;
		private Date time;
		private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy, HH:mm:SS");
		private String command;
		private boolean run;
		// objects for file operations
		private File file;
		private FileWriter fw;
		private FileReader fr;
		private BufferedReader in;
		private BufferedWriter out;
		private String directory;
		private String filename;
		private String dir;
		private String msg;
		
		public ServerProtocol(Socket c) {
			client = c;
			run = true;
			
			try {
				sin = new BufferedReader(new InputStreamReader(client.getInputStream()));
				sout = new PrintWriter(client.getOutputStream(), true);
			}
			catch(IOException exp) {
				logarea.append("!!! " + exp.getMessage() + " !!!\r\n");
				logarea.setCaretPosition(logarea.getText().length());
				this.interrupt();
			}
		
			if(banlist.contains(client.getInetAddress().getHostAddress())) {
				sout.print("You were banned from the server!\r\n");
				sout.flush();
				run = false;
				clientList.removeElement(client.getInetAddress().getHostAddress());
				clients.removeItem(client);
				try {
					client.close();
					logarea.append(client.getInetAddress() + " disconnected\r\n");
					logarea.setCaretPosition(logarea.getText().length());
				}
				catch(IOException exp) {
					logarea.append("*** " + exp.getMessage() + " ***\r\n");
					logarea.setCaretPosition(logarea.getText().length());
				}
			}
			else {
				boolean isConnected = false;
				
				for(int i = 0; i < clientList.size(); i++) {
					if(clientList.get(i).getInetAddress().getHostAddress().equals(client.getInetAddress().getHostAddress())) {
						isConnected = true;
					}
				}
				
				if(!isConnected) {
					int maxUsers = Integer.parseInt((String)cmb_maxUsers.getSelectedItem());
					
					if(clientList.size() >= maxUsers) {
						sout.print("Maximal size of connected users are reached!\r\n");
						sout.flush();
						
						try {
							sout.close();
							sin.close();
							client.close();
						}
						catch(IOException e) {
							logarea.append("*** " + e.getMessage() + " ***\r\n");
							logarea.setCaretPosition(logarea.getText().length());
						}
					}
					else {
					
						clientList.addElement(client);
						
						clients.addItem(client);
					
						time = new Date();
						logarea.append(client.getInetAddress() + " has connected at " + sdf.format(time.getTime()) + "\r\n");
						logarea.setCaretPosition(logarea.getText().length());
					}
				}
				else {
					sout.print("You are connected already!\r\n");
					sout.flush();
					
					try {
						sout.close();
						sin.close();
						client.close();
					}
					catch(IOException e) {
						logarea.append("*** " + e.getMessage() + " ***\r\n");
						logarea.setCaretPosition(logarea.getText().length());
					}
				}
			}
		}
			
		public void run() {
			
			try {
				if(!client.isClosed()) {
					sout.print("*** Connection established! Enter \"help\" to see all commands! ***\r\n");
					sout.flush();
					directory = startPath;
					file = new File(directory);
					filename = "";
					dir = "";
					msg = "";
				
					pattern = Pattern.compile("([\\p{L}]+)\\s+([A-Za-zÄÖÜäöü\\W\\w]+)");
				}
				
				while(run) {
				
					sout.print("You> ");
					sout.flush();
					
					command = sin.readLine();
					
					try {
						matcher = pattern.matcher(command);
						matcher.find();
					}
					catch(Exception exp) {
						time = new Date();
						logarea.append(client.getInetAddress() + " is closed at " + sdf.format(time) + "\r\n");
						logarea.setCaretPosition(logarea.getText().length());
						clientList.removeElement(client);
						clients.removeItem(client);
						sin.close();
						sout.close();
						client.close();
						run = false;
						continue;
					}
										
					logarea.append(client.getInetAddress() + " has used \"" + command + "\"\r\n");
					logarea.setCaretPosition(logarea.getText().length());
					
					if(command.contains("help")) {
						sout.print("Command\t Arguments\t\t Job\r\n\r\n");
						sout.flush();
						
						sout.print("help\t ---\t\t Shows this help text\r\n");
						sout.flush();
						sout.print("close\t ---\t\t Ends the connection\r\n");
						sout.flush();
						sout.print("list\t ---\t\t Lists all files and directories in the directory\r\n");
						sout.flush();
						sout.print("create\t <name>\t\t Creates a new file with the given name\r\n");
						sout.flush();
						sout.print("cd\t <dir>\t\t Changes in the directory with the given name (enter \"cd goback\" to go to the parent directory)\r\n");
						sout.flush();
						sout.print("mkdir\t <dir>\t\t Creates a new directory with the given name\r\n");
						sout.flush();
						sout.print("lc\t ---\t\t Shows you all connected clients\r\n");
						sout.flush();
						sout.print("chat\t ---\t\t Let you chat with the server owner\r\n");
						sout.flush();
						sout.print("win\t <code>\t\t Enter the win code, you have found in a file, and you get a number you should write in the 4chan thread!\r\n");
						sout.flush();
						sout.print("read\t <name>\t\t Reads a file\r\n");
						sout.flush();
						sout.print("date\t ---\t\t Returns the current datetime of the computer the server is running on.\r\n");
						sout.flush();
						
					}
					else if(command.contains("list")) {
						
						sout.print("Type\t Element\r\n\r\n");
						sout.flush();
						
						for(String i : file.list()) {
							file = new File(directory + "/" + i);
							
							if(file.isDirectory()) {
								sout.print("d\t " + i + "\r\n");
								sout.flush();
							}
							else if(file.isFile()) {
								sout.print("f\t " + i + "\r\n");
								sout.flush();
							}
							else {
								sout.print("o\t " + i + "\r\n");
								sout.flush();
							}
						}
						
						sout.print("\r\n");
						sout.flush();
						
						sout.print("f = file; d = directory; o = others\r\n\r\n");
						sout.flush();
						
						file = new File(directory);
						
					}
					else if(command.contains("create")) {
						try {
						
							filename = matcher.group(2);
							
							file = new File(directory + "/" + filename);							
														
							String text = "";
							
							if(filename.contains("..")) {
								sout.print("Don't use \"..\" in your filename!\r\n");
								sout.flush();
							}
							else if(file.exists()) {
									sout.print(filename + " does still exist\r\n");
									sout.flush();
							}
							else {
								try {
									file = new File(directory + "/" + filename);
									file.createNewFile();
									fw = new FileWriter(file);
									out = new BufferedWriter(fw);
							
									sout.print("Enter \"stop\" to save the file!\r\n");
									sout.flush();
									do {
										sout.print("> ");
										sout.flush();
							
										text = sin.readLine();
							
										if(!text.equals("stop")) {
											out.write(text);
											out.newLine();
										}
							
									} while(!text.equals("stop"));
							
									out.close();
									fw.close();
								}
								catch(IOException e) {
									sout.print(e.getMessage() + "\r\n");
									sout.flush();
								}
								
							}
							
							logarea.append(client.getInetAddress() + " created the file " + filename + "\r\n");
							logarea.setCaretPosition(logarea.getText().length());
						
							file = new File(directory);
						}
						catch(IllegalStateException exp) {
							sout.print("Usage: create <name>\r\n");
							sout.flush();
						}
						
					}
					else if(command.contains("read")) {
						try {
							
							filename = matcher.group(2);
													
							file = new File(directory + "/" + filename);
						
							String t = "";
							
							if(file.exists()) {
								if(filename.contains("..")) {
									sout.print("Don't use \"..\" in your filename!\r\n");
									sout.flush();
								}
								else {
									if(file.isFile()) {
										
										try {
											fr = new FileReader(file);
											in = new BufferedReader(fr);
							
											sout.print("----- Start File -----\r\n");
											sout.flush();
											while((t = in.readLine()) != null) {
												sout.print(t + "\r\n");
												sout.flush();
											}
											sout.print("----- End File -----\r\n");
											sout.flush();
							
											in.close();
											fr.close();
							
											logarea.append(client.getInetAddress() + " read the file " + filename + "\r\n");
											logarea.setCaretPosition(logarea.getText().length());
										}
										catch(IOException e) {
											sout.print(e.getMessage() + "\r\n");
											sout.flush();
										}
									}
									else {
										sout.print(filename + " is a directory!\r\n");
										sout.flush();
									}
								}
							}
							else {
								sout.print("File doesn't exist!\r\n");
								sout.flush();
							}
						
							file = new File(directory);
						}
						catch(IllegalStateException exp) {
							sout.print("Usage: read <name>\r\n");
							sout.flush();
						}
												
					}
					else if(command.contains("cd")) {
						try {
							String temp = directory;
							
							file = new File(directory);
							
							dir = matcher.group(2);
							
							if(dir.contains("..")) {
								sout.print("Don't use \"..\" in your argument!\r\n");
								sout.flush();
							}
							else if(dir.equals("goback")) {
								if(!file.isDirectory()) {
									file = new File(file.getParent());
									directory = file.getParent();
									file = new File(directory);
									
									if(!file.getPath().contains(secureFile)) {
										sout.print("You can't get out from this directory!\r\n");
										sout.flush();
										file = new File(temp);
										directory = temp;
									}
									
									directory = file.getPath();
									logarea.append(client.getInetAddress() + " changed to the directory " + directory + "\n");
									logarea.setCaretPosition(logarea.getText().length());
								}
								else if(file.isDirectory()) {
									if(!file.getAbsolutePath().equals("/")) {
										file = new File(file.getParent());
									}
									else {
										sout.print("You are in the root directory!\r\n");
										sout.flush();
									}
									
									if(!file.getPath().contains(secureFile)) {
										sout.print("You can't get out from this directory!\r\n");
										sout.flush();
										file = new File(temp);
										directory = temp;
									}
									
									directory = file.getPath();
									logarea.append(client.getInetAddress() + " changed to the directory " + directory + "\n");
									logarea.setCaretPosition(logarea.getText().length());
								}
							}
							else {
								if(dir.charAt(0) == '/') {
									directory += dir;
								}
								else {
									directory += "/" + dir;
								}
								
								file = new File(directory);
								
								if(!file.isDirectory()) {
									if(!file.exists()) {
										sout.println(dir + " doesn't exist!\r\n");
										sout.flush();
										directory = temp;
										file = new File(temp);
									}
									else {
										sout.print(dir + " isn't a directory!\r\n");
										sout.flush();
										directory = temp;
										file = new File(temp);
									}
								}
								
								logarea.append(client.getInetAddress() + " changed to the directory " + directory + "\n");
								logarea.setCaretPosition(logarea.getText().length());
							}
						}
						catch(IllegalStateException exp) {
							sout.print("Usage: cd <Dir>\r\n");
							sout.flush();
						}
					}
					else if(command.contains("mkdir")) {
						try {
							dir = matcher.group(2);
							
							file = new File(directory + "/" + dir);
							
							if(dir.contains("..")) {
								sout.print("Don't use \"..\" in your argument!");
								sout.flush();
							}
							else if(file.exists()) {
								sout.print("Directory does still exist!\r\n");
								sout.flush();
								file = new File(directory);
							}
							else {
								if(file.mkdir()) {
									logarea.append(client.getInetAddress() + " created the directory " + file.getPath() + "\n");
								}
								
								file = new File(directory);
							}
						}
						catch(IllegalStateException exp) {
							sout.print("Usage: mkdir <Dir>\r\n");
							sout.flush();
						}
					}
					else if(command.contains("lc")) {
						try {
							sout.print("A list of all client IPs, which were currently connected\r\n\r\n");
							sout.flush();
							
							for(int i = 0; i < clientList.getSize(); i++) {
								sout.print(clientList.get(i).getInetAddress().getHostAddress() + "\n");
								sout.flush();
								
								
							}
							
							logarea.append(client.getInetAddress() + " got a list of all client IPs\n");
							logarea.setCaretPosition(logarea.getText().length());
						}
						catch(Exception exp) {
							logarea.append("*** " + exp.getMessage() + " ***\r\n");
							logarea.setCaretPosition(logarea.getText().length());
						}
					}
					else if(command.contains("chat")) {
						try {
							if(boolchat) {
								sout.print("message> ");
								sout.flush();
							
								msg = sin.readLine();
							
								if(!msg.equals("")) {
									if(msg.length() > 130) {
										sout.print("At the most just 130 characters are allowed!\r\n");
										sout.flush();
									}
									else {
										msg = JOptionPane.showInputDialog(c, msg);
							
										sout.print("Admin> " + msg + "\r\n");
										sout.flush();
									}
								}
							}
							else {
								sout.print("The chat-function is currently disabled!\r\n\r\n");
								sout.flush();
							}
							
							msg = "";
						}
						catch(Exception exp) {
							logarea.append("*** " + exp.getMessage() + " ***\r\n");
							logarea.setCaretPosition(logarea.getText().length());
						}
					}
					else if(command.contains("win")) {
						try {
							
							
							logarea.append(client.getInetAddress() + " has entered: " + matcher.group(2) + "\n");
							logarea.setCaretPosition(logarea.getText().length());
							
							if(winCode.equals(matcher.group(2))) {
								sout.print("You're right! Enter \"1337\" in the thread!\r\n");
							}
							else {
								sout.print("No, you're wrong!\r\n");
							}
							
						}
						catch(IllegalStateException exp) {
							sout.print("Usage: win <code>\r\n");
							sout.flush();
						}
					}
					else if(command.contains("date")) {
						time = new Date();
						
						sout.print("Current datetime of this computer: " + sdf.format(time) + "\r\n");
						sout.flush();
					}
					else if(command.contains("close")) {
						sout.print("Connection closed!\r\n");
						sout.flush();
						clientList.removeElement(client);
						clients.removeItem(client);
						time = new Date();
						logarea.append(client.getInetAddress() + " is closed at " + sdf.format(time) + "\r\n");
						logarea.setCaretPosition(logarea.getText().length());
						client.close();
						run = false;
					}
					else {
						sout.print("Invalid command!\r\n");
						sout.flush();
					}
				}
			}
			catch(IOException exp) {
				logarea.append(client.getInetAddress() + " " + exp.getMessage() + "\r\n");
				logarea.setCaretPosition(logarea.getText().length());
			}
		}
	}
	
	/**
	 * Thread for server running
	 */
	public class ServerRun extends Thread {
		
		public void run() {
			try {
				server = new ServerSocket(Integer.parseInt((String)port.getSelectedItem()));
			
				logarea.setText("Server was started on port " + port.getSelectedItem() + "\r\n");
				logarea.setCaretPosition(logarea.getText().length());
			
				while(isrun) {
					client = server.accept();
				
					new ServerProtocol(client).start();
				}
			}
			catch(IOException exp) {
				logarea.append("*** " + exp.getMessage() + " ***\r\n");
				logarea.setCaretPosition(logarea.getText().length());
			}
		}
	}
	
	/**
	 * Private class for all window events
	 */
	private class WindowEvents extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			try {
				if(server != null) {
					server.close();
					isrun = false;
				}
				JOptionPane.showMessageDialog(c, "Bye!");
			}
			catch(IOException exp) {
				logarea.append("*** " + exp.getMessage() + " ***\r\n");
				logarea.setCaretPosition(logarea.getText().length());
			}
		}
	}
	
	/**
	 * Private class for pressing buttons
	 */
	private class ClickButton implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == go) {
				isrun = true;
				new ServerRun().start();
			}
			else if(e.getSource() == stop) {
				try {
					server.close();
					for(int i = 0; i < clientList.getSize(); i++) {
						clientList.get(i).close();
					}
					isrun = false;
					clientList.removeAllElements();
					clients.removeAllItems();
					logarea.append("*** Server was closed! ***\r\n");
					logarea.setCaretPosition(logarea.getText().length());
				}
				catch(IOException exp) {
					logarea.append("*** " + exp.getMessage() + " ***\r\n");
					logarea.setCaretPosition(logarea.getText().length());
				}
			}
			else if(e.getSource() == send) {
				// not yet implemented
			}
			else if(e.getSource() == kick) {
				try {
					sock = (Socket) clients.getSelectedItem();
					write = new PrintWriter(sock.getOutputStream(), true);
					write.print("You were kicked from the server!\r\n");
					write.flush();
					write.close();
					sock.close();
					clients.removeItem(sock);
					clientList.removeElement(sock);
					sock = null;
				}
				catch(Exception exp) {
					logarea.append("*** " + exp.getMessage() + " ***\r\n");
					logarea.setCaretPosition(logarea.getText().length());
				}
			}
			else if(e.getSource() == ban) {
				try {
					sock = (Socket) clients.getSelectedItem();
					banlist.add(sock.getInetAddress().getHostAddress());
					write = new PrintWriter(sock.getOutputStream(), true);
					write.print("You were banned from the server!\r\n");
					write.flush();
					write.close();
					clients.removeItem(sock);
					clientList.removeElement(sock);
					sock = null;
				}
				catch(IOException exp) {
					logarea.append("*** " + exp.getMessage() + " ***\r\n");
					logarea.setCaretPosition(logarea.getText().length());
				}
			}
			else if(e.getSource() == send) {
				try {
					sock = (Socket) clients.getSelectedItem();
					write = new PrintWriter(sock.getOutputStream(), true);
					msg = JOptionPane.showInputDialog(c, "Your message: ");
					write.print(msg + "\r\n");
					write.close();
					sock = null;
				}
				catch(Exception exp) {
					logarea.append("*** " + exp.getMessage() + " ***\r\n");
					logarea.setCaretPosition(logarea.getText().length());
				}
			}
			else if(e.getSource() == enableChat) {
				boolchat = enableChat.isSelected();
			}
		}
	}
	
	/**
	 * The constructor creates the GUI
	 */
	public CTF(boolean visible) {
		c = getContentPane();
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(c, e.getMessage());
		}
		
		this.addWindowListener(new WindowEvents());
		
		tabs = new JTabbedPane();
		panel1 = new JPanel(new BorderLayout());
		panel2 = new JPanel(new GridLayout(2, 2, 5, 5));
		
		isrun = false;
		
		// Creates the toolbar
		
		tb = new JToolBar("Start and Stop");
		
		go = new JButton("Start");
		go.setFont(buttonFont);
		go.addActionListener(new ClickButton());
		
		stop = new JButton("Stopp");
		stop.setFont(buttonFont);
		stop.addActionListener(new ClickButton());
		
		String[] werte = new String[3000];
		
		for(int i = 1; i <= 3000; i++) {
			werte[i-1] = i + "";
		}
		
		port = new JComboBox(werte);
		port.setToolTipText("Select the specific port number");
		port.setSelectedItem("2000");
		port.setFont(buttonFont);
		
		enableChat = new JCheckBox("Chat");
		enableChat.setFont(buttonFont);
		enableChat.addActionListener(new ClickButton());
		
		String[] u_s = new String[20];
		
		for(int i = 0; i < u_s.length; i++) {
			u_s[i] = (i+1) + "";
		}
		
		lbl_maxUsers = new JLabel("  Max. Users: ");
		lbl_maxUsers.setFont(buttonFont);
		
		cmb_maxUsers = new JComboBox(u_s);
		cmb_maxUsers.setFont(buttonFont);
		cmb_maxUsers.setSelectedIndex(4);
		
		tb.add(go);
		tb.add(port);
		tb.add(enableChat);
		tb.add(lbl_maxUsers);
		tb.add(cmb_maxUsers);
		tb.add(Box.createHorizontalGlue());
		tb.add(stop);
		
		// Create the first tab
		
		tabs.addTab("Log", panel1);
		
		logarea = new JTextArea();
		logarea.setEditable(false);
		logarea.setLineWrap(true);
		logarea.setWrapStyleWord(true);
		logarea.setFont(textFont);
		sp = new JScrollPane(logarea);
		
		panel1.add(BorderLayout.CENTER, sp);
		
		// Create the second tab
		
		tabs.addTab("Clients", panel2);
		
		kick = new JButton("Kick");
		kick.setFont(buttonFont);
		kick.addActionListener(new ClickButton());
		
		ban = new JButton("Ban");
		ban.setFont(buttonFont);
		ban.addActionListener(new ClickButton());
		
		send = new JButton("Send");
		send.setFont(buttonFont);
		send.addActionListener(new ClickButton());
		
		clientList = new DefaultListModel<>();
		
		banlist = new ArrayList<>();
		
		clients = new JComboBox();
		clients.setBorder(new TitledBorder("All connected clients"));
		
		clients.setFont(textFont);
		
		panel2.add(clients);
		panel2.add(ban);
		panel2.add(kick);
		panel2.add(send);
		
		c.add(BorderLayout.NORTH, tb);
		c.add(BorderLayout.CENTER, tabs);
		
		if(visible) {
			setVisible(true);
		}
		else {
			boolchat = true;
			setVisible(false);
			isrun = true;
			new ServerRun().start();
		}
		
	}
	
	public static void main (String[] args) {
		
		if(args.length >= 3 && (args[1].equalsIgnoreCase("yes") || args[1].equalsIgnoreCase("no"))) {
			
			secureFile = args[0];
			startPath = args[2];
			
			if(args.length >= 4) {
				winCode = args[3];
			}
			else {
				winCode = "§§§!iMpOsSiBlE_!_wInCoDe!§§§";
			}
			
			if(secureFile.charAt(0) == '/' || secureFile.indexOf("C:\\") == 0) {
				
				if(args[1].equalsIgnoreCase("yes")) {
					CTF window = new CTF(true);
					window.setTitle("Capture the File - Server 1.0");
					window.setSize(600, 250);
					window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				}
				else if(args[1].equalsIgnoreCase("no")) {
					CTF window = new CTF(false);
				}
				else {
					System.out.println("Second argument must be 'yes' or 'no'!");
				}
				
			}
			else {
				System.out.println("Secure path has to be absolute!");
			}
		}
		else {
			System.out.println("Usage: java -jar CTF.jar <absolute path of secure folder> <visible:yes|no> <absolute path of start folder> [<win code>]");
		}
	}
}