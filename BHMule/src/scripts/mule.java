package scripts;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.tribot.api.General;
import org.tribot.api.input.Mouse;
import org.tribot.api.util.Screenshots;
import org.tribot.api2007.Banking;
import org.tribot.api2007.Camera;
import org.tribot.api2007.Game;
import org.tribot.api2007.Interfaces;
import org.tribot.api2007.Inventory;
import org.tribot.api2007.Login;
import org.tribot.api2007.Login.STATE;
import org.tribot.api2007.Player;
import org.tribot.api2007.Players;
import org.tribot.api2007.Trading;
import org.tribot.api2007.Walking;
import org.tribot.api2007.WebWalking;
import org.tribot.api2007.WorldHopper;
import org.tribot.api2007.types.RSArea;
import org.tribot.api2007.types.RSItem;
import org.tribot.api2007.types.RSPlayer;
import org.tribot.api2007.types.RSTile;
import org.tribot.script.Script;
import org.tribot.script.ScriptManifest;
import org.tribot.script.interfaces.Arguments;
import org.tribot.script.interfaces.MessageListening07;
import org.tribot.util.Util;
import scripts.NWorldHopper;

@ScriptManifest(authors = { "Prech" }, category = "Tools", name = "BH - Mule Host")
public class mule extends Script implements MessageListening07 {
	
	private String playername = Player.getRSPlayer().getName();
	private String serverIP = "localhost/botnetwork/";
	private int worldid = 0;
	private JFrame mainwindow;
	private MuleGUI window;
	private Instant starttime;
	private Instant updatetime;
	private status curr_status;
	private String DBResponse;
	private String targetid = "";
	private String targetlocation = "";
	private String targetrequest = "";
	private String targetstatus = "";
	private int targetworld = 0;
	private boolean available = true;
	private boolean inplace = false;
	private int trademoney = 0;
	private ArrayList<String> tradereceived;
	private ArrayList<String> tradegiven;
	private String itemsrequested = "";
	private int amountrequested = 0;
	private String[] itemsrequested_db = {};
	private String[] ilist;
	private int offeredgold = 0;
	private boolean bankchecked = false;
	private Instant vaulttimer;
	private ArrayList<String> bankitems_arrlist;
	private boolean startvault = true;
	private boolean aftertrade = false;
	private int loginrequest = 0;
	private int online = 0;
	private String traderequest_name = "";
	private RSTile gepos1 = new RSTile(3164,3487);
	private RSTile gepos2 = new RSTile(3167,3488);
	private RSTile gepos3 = new RSTile(3165,3492);
	private RSTile gepos4 = new RSTile(3162,3489);
	private RSTile[] gepos = { gepos2, gepos3, gepos4 };
	private Instant screenshottime;
	private int screenshotminutes = 5;
	private int off = 0;
	private boolean itemsoffered = false;
	
	private enum locations {
			VARROCK_SQUARE, GE, LUMBRIDGE, EDGEVILLE
	}
	private RSArea area_ge = new RSArea(new RSTile(3167,3487), new RSTile(3162,3492));
	private RSArea area_varrocksq = new RSArea(new RSTile(3209,3432), new RSTile(3217,3423));
	private RSTile tile_ge = new RSTile(3164,3484);
	private RSTile tile_varrocksq = new RSTile(3217,3423);
	
	private enum status {
		IDLE, CHECKDB, GOTOLOCATION, EXCHANGE, WORLDSWITCH, VAULT, RANDOMDROP
	}

	@Override
	public void run() {
		worldid = Game.getCurrentWorld();
		starttime = Instant.now();
		updatetime = starttime;
		vaulttimer = Instant.now();
		screenshottime = Instant.now();
		// TODO Auto-generated method stub
		mainwindow = new JFrame("BotHeaven - Shop Buyer v0.1");
		window = new MuleGUI();
		mainwindow.setContentPane(window.jPanel2);
		
		JButton loginbtn = window.loginbtn;
		loginbtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Login.login();
			}
		});
		JButton logoutbtn = window.logoutbtn;
		logoutbtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Login.logout();
			}
		});
		
		
		mainwindow.pack();
		mainwindow.setResizable(false);
		mainwindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainwindow.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent windowEvent) {
		        if (JOptionPane.showConfirmDialog(mainwindow, 
		            "Are you sure you want to stop the script?", "Exit script?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
		            mainwindow.setVisible(false);
		            mainwindow.dispose();
		        }
		    }
		});
		mainwindow.setVisible(true);
		botstart();
	
		
		
		while (mainwindow.isShowing()) {
			Mouse.setSpeed(300);
			worldid = Game.getCurrentWorld();
			window.worldid.setText(Integer.toString(worldid));
			
			if (Login.getLoginState() != STATE.INGAME) {
				window.loginbtn.setEnabled(true);
				window.logoutbtn.setEnabled(false);
			} else {
				window.loginbtn.setEnabled(false);
				window.logoutbtn.setEnabled(true);
			}
			
			curr_status = getStatus();
			
			switch(curr_status) {
			case CHECKDB:
				if (Login.getLoginState() == Login.STATE.INGAME || Login.getLoginState() == Login.STATE.WELCOMESCREEN) {
					online = 1;
				} else {
					online = 0;
				}
				window.botstatus.setText("Checking database..");
				DBResponse = checkdb();
				//println("DBResponse: '" + DBResponse + "'");
				if (DBResponse != null && !DBResponse.isEmpty()) {
					this.setLoginBotState(true);
					String[] temparr = DBResponse.split("\\|");
					if (temparr.length > 1) {
					targetid = temparr[0];
					targetrequest = temparr[1];
					targetworld = Integer.parseInt(temparr[2]);
					targetlocation = temparr[3];
					itemsrequested = temparr[4];
					amountrequested = Integer.parseInt(temparr[5]);
					loginrequest = Integer.parseInt(temparr[6]);
					//println("Logreq: " + loginrequest);
					//println("IReq: '" + itemsrequested + "'");
					itemsrequested = itemsrequested.replaceAll("\\[", "");
					itemsrequested = itemsrequested.replaceAll("\\]", "");
					itemsrequested = itemsrequested.replaceAll("\\, ", ",");
					//println("Ireqqq: '" + itemsrequested + "'");
			        //println("BuyArr: " + array[1].toString());
			        ilist = itemsrequested.split("\\,");
			        
			        
			        	if (loginrequest == 1) {
			        		if (Login.getLoginState() == STATE.LOGINSCREEN || Login.getLoginState() == STATE.UNKNOWN) {
			        			Login.login();
								loginrequest = 0;
			        		} else {
			        			loginrequest = 0;
			        		}
			        	}
			        
					available = false;
					//println("Bot " + targetid + " needs to " + targetrequest + " at " + targetlocation + ", world: " + targetworld);
					//println("Items requested: " + Arrays.toString(ilist));
					//println("Amount requested: " + amountrequested);
					} else {
						if (!DBResponse.equals("empty")) {
						loginrequest = Integer.parseInt(DBResponse);
						if (loginrequest == 1) {
			        		if (Login.getLoginState() == STATE.LOGINSCREEN || Login.getLoginState() == STATE.UNKNOWN) {
			        			Login.login();
								loginrequest = 0;
			        		} else {
			        			loginrequest = 0;
			        		}
			        	}
						}
					}
				} else {
					available = true;
				}
				updatetime = Instant.now();
				General.sleep(General.random(200, 700));
				break;
			case GOTOLOCATION:
				this.setLoginBotState(true);
				WebWalking.setUseAStar(true);
				window.botstatus.setText("Walking to meeting point..");
				if (targetlocation.equals("Varrock square")) {
					println("Walking to Varrock SQ");
					WebWalking.walkTo(tile_varrocksq);
					General.sleep(General.random(700, 1800));
				}
				if (targetlocation.equals("Grand Exchange")) {
					println("Walking to GE");
					WebWalking.walkTo(gepos[General.random(0, 2)]);
					General.sleep(General.random(700, 1800));
				}
				break;
			case EXCHANGE:
				this.setLoginBotState(true);
				//window.botstatus.setText("Exchanging target bot..");
				RSPlayer[] targetplayer = Players.find(targetid);
				window.request.setText(targetrequest + "Bot: " + targetid);
				window.location.setText(targetlocation);
				ArrayList<String> reqitems = new ArrayList<String>(Arrays.asList(ilist));
				for (String ritem : ilist) {
					if (ritem != null) {
					String[] ritemdb = ritem.split("\\_");
					String ritemname = ritemdb[0];
					if (Inventory.find(ritemname).length < 1) {
						String tempi = ritemname + "_" + ritemdb[1];
						reqitems.remove(tempi);
						ilist = reqitems.toArray(ilist);
					}
					}
				}

				
				if (targetplayer.length > 0) {
					
					if (Trading.getWindowState() == null) {
						window.botstatus.setText("Target player in place, attempting to trade");
						if ((Trading.getWindowState() != Trading.WINDOW_STATE.FIRST_WINDOW && Trading.getWindowState() != Trading.WINDOW_STATE.SECOND_WINDOW) && !targetplayer[0].isMoving()) {
							Camera.turnToTile(targetplayer[0]);
							targetplayer[0].click("Trade with " + targetplayer[0].getName());
							offeredgold = 0;
						}
						General.sleep(General.random(1000,2000));
					} else {
						RSItem[] offered = Trading.getOfferedItems(false);
					if (Trading.getWindowState() == Trading.WINDOW_STATE.FIRST_WINDOW) {
						//println("Trade window opened");
						RSItem[] pinv = Inventory.getAll();
						window.botstatus.setText("Offering items");
						
						
						
						
						off = offered.length;
						if (itemsoffered == false) {
							//println("Offered > ilist");
							for (String iname : ilist) {
								if (iname != null) {
								offered = Trading.getOfferedItems(false);
										String[] idb = iname.split("\\_");
										if (idb.length > 0) {
											String itbname = idb[0];
											int ramount = Integer.parseInt(idb[1]);
											//println("Item: " + itbname + ", amount: " + ramount);
											RSItem[] tmpitem = Inventory.find(itbname);
											
											if (tmpitem.length > 0) {
												if (ramount > tmpitem[0].getStack()) {
													int tmpamount = tmpitem[0].getStack();
													Trading.offer(tmpitem[0], tmpamount);
													off++;
												} else {
													Trading.offer(tmpitem[0], ramount);
													off++;
												}
											} else {
												off++;
											}
										}
								}
									}
							itemsoffered = true;
						}
										
									
							
							
							offered = Trading.getOfferedItems(false);
							//println("Offered: " + off);
							General.sleep(200,250);
						if (ilist.length < 1) {
							Trading.accept();
						}
						if (Trading.hasAccepted(true)) {
							Trading.accept();
							General.sleep(200,500);
						}
					}
					if (Trading.getWindowState() == Trading.WINDOW_STATE.SECOND_WINDOW) {
						//println("Second trade window");
						offered = Trading.getOfferedItems(false);
						//println("Offered: " + off);
						//println("Ilist: " + ilist.length);
						if (itemsoffered && Trading.hasAccepted(true)) {
							tradereceived = new ArrayList<String>();
							tradegiven = new ArrayList<String>();
							RSItem[] tradeitems = Trading.getOfferedItems(true);
							String[] titems;
							for (RSItem titem : tradeitems) {
								tradereceived.add(titem.getDefinition().getName()+"|"+titem.getStack());
							}
							RSItem[] giving = Trading.getOfferedItems(false);
							for (RSItem gitem : giving) {
								tradegiven.add(gitem.getDefinition().getName()+"|"+gitem.getStack());
							}
							if (Trading.accept()) {
								//println("Successfully traded!");
								clearqueue(tradereceived, tradegiven);
								//println("Added to database!");
								available = true;
								inplace = false;
								aftertrade = true;
								itemsoffered = false;
								General.sleep(700);
							}
						}
						if (ilist.length < 1) {
							if (Trading.accept()) {
								//println("Successfully traded!");
								clearqueue(tradereceived, tradegiven);
								//println("Added to database!");
								available = true;
								inplace = false;
								aftertrade = true;
								itemsoffered = false;
								General.sleep(700);
							}
						}
						
					}
					break;
					}
				} else {
					window.botstatus.setText(("Target player not in place yet, waiting.."));
					if (Banking.isBankScreenOpen()) Banking.close();
					if (!area_ge.contains(Player.getPosition())) Walking.walkTo(gepos[General.random(0, 2)]);
					General.sleep(General.random(800, 1600));
					break;
				}
			case WORLDSWITCH:
				this.setLoginBotState(true);
				if (Banking.isBankScreenOpen()) Banking.close();
				if (Game.getCurrentWorld() != targetworld) {
					if (Login.getLoginState() == STATE.INGAME) {
					window.botstatus.setText("Switching to world " + targetworld + ".");
					NWorldHopper.changeWorld(targetworld);
					General.sleep(1500);
					} else {
						window.botstatus.setText("Handling login screen..");
						if (Login.login()) {
							window.botstatus.setText("Switching to world " + targetworld + ".");
							NWorldHopper.changeWorld(targetworld);
							General.sleep(1500);
						}
					}
					break;
				} else {
					break;
				}
			case VAULT:
				window.botstatus.setText("Checking bank & inventory and logging to database..");
				updatevault();
				General.sleep(2000);
				break;
			case RANDOMDROP:
				RSPlayer[] target = Players.find(targetid);
				if (target.length > 0) {
					if (Trading.getWindowState() == null) {
						window.botstatus.setText("Someone tries to trade me...");
						if ((Trading.getWindowState() != Trading.WINDOW_STATE.FIRST_WINDOW && Trading.getWindowState() != Trading.WINDOW_STATE.SECOND_WINDOW) && !target[0].isMoving() && area_ge.contains(target[0])) {
							Camera.turnToTile(target[0]);
							target[0].click("Trade with " + target[0].getName());
							offeredgold = 0;
						}
						General.sleep(General.random(1000,2000));
					} else {
						if (Trading.getWindowState() == Trading.WINDOW_STATE.FIRST_WINDOW) {
							println("Trade window opened");
							if (Trading.hasAccepted(true)) {
								Trading.accept();
								General.sleep(200,500);
							}
						}
						if (Trading.getWindowState() == Trading.WINDOW_STATE.SECOND_WINDOW) {
							println("Second trade window");
							ArrayList<String> offered = new ArrayList<String>();
							tradereceived = new ArrayList<String>();
							for (RSItem offlist : Trading.getOfferedItems(false)) {
								offered.add(offlist.getDefinition().getName().toString());
							}
							RSItem[] tradeitems = Trading.getOfferedItems(true);
							String[] titems;
							for (RSItem titem : tradeitems) {
								tradereceived.add(titem.getDefinition().getName()+"|"+titem.getStack());
							}
							if (Trading.accept()) {
								println("Successfully traded!");
								clearqueue(tradereceived, offered);
								println("Added to database!");
								available = true;
								inplace = false;
								aftertrade = true;
								traderequest_name = "";
								General.sleep(700);
							}
						}
						break;
					}
				} else {
					window.botstatus.setText(("Lost sight of the player.."));
					traderequest_name = "";
					General.sleep(General.random(800, 1600));
					break;
				}
				break;
			case IDLE:
				window.request.setText("none");
				window.location.setText("none");
				window.botstatus.setText("Idle");
				this.setLoginBotState(false);
				if (available && Login.getLoginState() == STATE.INGAME) {
					General.sleep(General.random(1200, 1900));
					Login.logout();
				}
				break;
			}
			
			General.sleep(General.random(600, 1600));
			
		}
		
	}
	
	
	
	private status getStatus() {
		
		
		
		
		
		if (Duration.between(updatetime, Instant.now()).toMillis()/1000 > 5) {
			return status.CHECKDB;
		}
		if (available == true) {
			
			if (aftertrade == true || (Duration.between(starttime, Instant.now()).toMillis()/1000 < 60 && startvault == true)) { // 5 minutes or script running less than minute
				if (Login.getLoginState().equals(Login.STATE.INGAME)) { 
					return status.VAULT;
				} else {
					window.botstatus.setText("Logging in..");
					if (Login.login()) {
						return status.VAULT;
					}
				}
			}
			
			
			if (traderequest_name != "") {
				return status.RANDOMDROP;
			}
			
		} else {
			
			if (targetworld != worldid && (Trading.getWindowState() != Trading.WINDOW_STATE.FIRST_WINDOW || Trading.getWindowState() != Trading.WINDOW_STATE.SECOND_WINDOW)) {
				//println("Not at target world");
				return status.WORLDSWITCH;
			} else {
				//println("At target world");
				if (inplace) {
					//println("In place");
					return status.EXCHANGE;
				}
			}
			//println("Target location: " + targetlocation);
			if (targetlocation.equals("Varrock square")) {
				if (!area_varrocksq.contains(Player.getRSPlayer())) {
					inplace = false;
					//println("Not in Varrock SQ");
					return status.GOTOLOCATION;
				} else {
					//println("At Varrock SQ");
					inplace = true;
				}
			}
			if (targetlocation.equals("Grand Exchange")) {
				if (!area_ge.contains(Player.getRSPlayer())) {
					inplace = false;
					//println("Not at GE");
					return status.GOTOLOCATION;
				} else {
					//println("At GE");
					inplace = true;
				}
			}
			
			
			
			
			
			
			
		}
		
		
		
		return status.IDLE;
	}
	
	
	private void botstart() {
		
		try {
			String pname = playername;
    		pname = pname.replace(" ", "%20");
    		pname = pname.replaceAll("\\W", "%20");
    	String webPage = "http://" + serverIP + "/bhstart.php?bot=" + pname + "&type=mule&world=" + worldid + "&status=" + curr_status;
    	
    	//println("Updatepage: " + webPage);
    	URL url = new URL(webPage);
    	URLConnection urlConnection = url.openConnection();
    	InputStream is = urlConnection.getInputStream();
    	InputStreamReader isr = new InputStreamReader(is);

    	int numCharsRead;
    	char[] charArray = new char[1024];
    	StringBuffer sb = new StringBuffer();
    	while ((numCharsRead = isr.read(charArray)) > 0) {
    	sb.append(charArray, 0, numCharsRead);
    	}
    	String result = sb.toString();
    	//log("Got new item price: " + result);
    	//log(result);
    	if (result == "1") {
    		println("Connected to DB!");
    	}
    	} catch (Exception e) {
    	println("Exception occurred at updatedbstatus." + e);
    	}
	}
	
private String checkdb() {
		worldid = Game.getCurrentWorld();
		try {
			String pname = playername;
    		pname = pname.replace(" ", "%20");
    		pname = pname.replaceAll("\\W", "%20");
    	String webPage = "http://" + serverIP + "bhcheck.php?bot=" + pname + "&world=" + worldid + "&status=" + curr_status + "&online=" + online;
    	//println("Webpage: " + webPage);
    	//println("Updatepage: " + webPage);
    	URL url = new URL(webPage);
    	URLConnection urlConnection = url.openConnection();
    	InputStream is = urlConnection.getInputStream();
    	InputStreamReader isr = new InputStreamReader(is);

    	int numCharsRead;
    	char[] charArray = new char[1024];
    	StringBuffer sb = new StringBuffer();
    	while ((numCharsRead = isr.read(charArray)) > 0) {
    	sb.append(charArray, 0, numCharsRead);
    	}
    	String result = sb.toString();
    	//log("Got new item price: " + result);
    	//log(result);
    	
    		return result;
    	
    	} catch (Exception e) {
    	println("Exception occurred at updatedbstatus." + e);
    	}
		
		return "";
	}

private void clearqueue(ArrayList<String> itemsreceived, ArrayList<String> itemsgiven) {
	
	try {
		String pname = playername;
		pname = pname.replace(" ", "%20");
		pname = pname.replaceAll("\\W", "%20");
		String targetname = targetid.replace(" ", "%20");
		targetname = targetid.replaceAll("\\W", "%20");
		String itemsrecstring = itemsreceived.toString().replaceAll(" ", "%20");
		String itemsgivstring = itemsgiven.toString().replaceAll(" ", "%20");
	String webPage = "http://" + serverIP + "bhclear.php?bot=" + pname + "&target=" + targetname + "&received=" + itemsrecstring + "&given=" + itemsgivstring;
	//println("Webpage: " + webPage);
	//println("Updatepage: " + webPage);
	URL url = new URL(webPage);
	URLConnection urlConnection = url.openConnection();
	InputStream is = urlConnection.getInputStream();
	InputStreamReader isr = new InputStreamReader(is);

	int numCharsRead;
	char[] charArray = new char[1024];
	StringBuffer sb = new StringBuffer();
	while ((numCharsRead = isr.read(charArray)) > 0) {
	sb.append(charArray, 0, numCharsRead);
	}
	String result = sb.toString();
	//println("Res: " + result);
	//log("Got new item price: " + result);
	//log(result);
	
	} catch (Exception e) {
	println("Exception occurred at clearqueue." + e);
	}
}


private String updatevault() {
	
	
	
	RSItem[] pinv = Inventory.getAll();
	ArrayList<String> pinv_arrlist = new ArrayList<String>();
	for (RSItem pinv_item : pinv) {
		pinv_arrlist.add(pinv_item.getDefinition().getName().toString() + "|" + pinv_item.getStack());
	}
	String pname = playername;
	pname = pname.replace(" ", "%20");
	pname = pname.replaceAll("\\W", "%20");
	String bankstring = "[]";
	String invstring = pinv_arrlist.toString().replaceAll("\\ ", "%20");
	try {
	
	String webPage = "http://" + serverIP + "bhvault.php?bot=" + pname + "&bank=" + bankstring + "&inv=" + invstring;
	//println("Webpage: " + webPage);
	//println("Updatepage: " + webPage);
	URL url = new URL(webPage);
	URLConnection urlConnection = url.openConnection();
	InputStream is = urlConnection.getInputStream();
	InputStreamReader isr = new InputStreamReader(is);

	int numCharsRead;
	char[] charArray = new char[1024];
	StringBuffer sb = new StringBuffer();
	while ((numCharsRead = isr.read(charArray)) > 0) {
	sb.append(charArray, 0, numCharsRead);
	}
	String result = sb.toString();
	//log("Got new item price: " + result);
	//log(result);
	vaulttimer = Instant.now();
	startvault = false;
	aftertrade = false;
		return result;
	
	} catch (Exception e) {
	println("Exception occurred at vault." + e);
	}
	
	
	return "";
}

@Override
public void tradeRequestReceived(String name) {
	if (available) {
		traderequest_name = name;
		targetid = name;
	}
}



@Override
public void clanMessageReceived(String arg0, String arg1) {
	// TODO Auto-generated method stub
	
}



@Override
public void duelRequestReceived(String arg0, String arg1) {
	// TODO Auto-generated method stub
	
}



@Override
public void personalMessageReceived(String arg0, String arg1) {
	// TODO Auto-generated method stub
	
}



@Override
public void playerMessageReceived(String arg0, String arg1) {
	// TODO Auto-generated method stub
	
}



@Override
public void serverMessageReceived(String arg0) {
	// TODO Auto-generated method stub
	
}

	
	
}
