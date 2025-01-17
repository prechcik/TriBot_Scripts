package scripts;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.tribot.api.General;
import org.tribot.api.input.Mouse;
import org.tribot.api.interfaces.Clickable;
import org.tribot.api.util.Screenshots;
import org.tribot.api2007.Banking;
import org.tribot.api2007.Camera;
import org.tribot.api2007.ChooseOption;
import org.tribot.api2007.Equipment;
import org.tribot.api2007.Equipment.SLOTS;
import org.tribot.api2007.Game;
import org.tribot.api2007.Interfaces;
import org.tribot.api2007.Inventory;
import org.tribot.api2007.Login;
import org.tribot.api2007.Login.LOGIN_MESSAGE;
import org.tribot.api2007.Login.STATE;
import org.tribot.api2007.NPCs;
import org.tribot.api2007.Player;
import org.tribot.api2007.Players;
import org.tribot.api2007.Trading;
import org.tribot.api2007.Trading.WINDOW_STATE;
import org.tribot.api2007.WebWalking;
import org.tribot.api2007.WorldHopper;
import org.tribot.api2007.types.RSArea;
import org.tribot.api2007.types.RSInterface;
import org.tribot.api2007.types.RSInterfaceMaster;
import org.tribot.api2007.types.RSItem;
import org.tribot.api2007.types.RSNPC;
import org.tribot.api2007.types.RSPlayer;
import org.tribot.api2007.types.RSTile;
import org.tribot.script.Script;
import org.tribot.script.ScriptManifest;
import org.tribot.util.Util;
import scripts.NWorldHopper;

@ScriptManifest(authors = { "Prech" }, category = "Money Making", name = "BH - Shopper - Tree Gnome Stronghold", description = "Ready to earn TONS of money as a gnome? Check it out!")
public class shopper extends Script {

	private RSArea area_ge = new RSArea(new RSTile(3161,3493), new RSTile(3168,3486));
	private RSTile GETile = new RSTile(3164,3487,0);
	private RSArea fguild_area = new RSArea(new RSTile(2605,3393,0), new RSTile(2617,3385,0));
	private RSArea gatearea = new RSArea(new RSTile(2460,3382,0), new RSTile(2462,3377,0));
	private RSTile gatetile = new RSTile(2461,3385,0);
	private RSArea gtarea = new RSArea(new RSTile(2467,3493,0), new RSTile(2464,3497,0));
	private RSTile gtladder_tile = new RSTile(2466,3495);
	private RSArea upstairs_area = new RSArea(new RSTile(2467,3493,1), new RSTile(2464,3497,1));
	private RSArea shop_area = new RSArea(new RSTile(2460,3489,2), new RSTile(2470,3480,2));
	private RSTile banktile = new RSTile(2449,3482,1);
	private RSArea varrockarea = new RSArea(new RSTile(3207,3434,0), new RSTile(3219,3423,0));
	private Instant screenshottime = null;
	private int[] p2pworlds = { 302,303,304,305,306,307,309,310,311,312,313,314,315,317,318,319,320,321,322,323,324,327,328,329,330,331,332,333,334,336,338,339,340,341,342,343,344,346,347,348,350,351,352,354,355,356,357,358,359,360,362,367,368,369,370,374,375,376,377,378,386,387,388,389,390,395,421,422,424,444,445,446,464,465,466,491,492,493,494,495,496,513,514,515,516,517,518,519,520,521,522,523,524,525 };
	private ArrayList<Integer> usedworlds = new ArrayList<Integer>();
	private String[] buylist = {
			"Bronze arrow|0", // Itemname|WhenToStop
			"Bronze bolts|0",
			"Bronze arrowtips|0",
			"Iron arrowtips|0",
			"Steel arrowtips|0",
			"Mithril arrowtips|0"
	};
	
	private enum status {
		IDLE, MULETASK, BANK, TOSHOP, BUY, WORLDHOP, SCREENSHOT
	}
	
	private status current_status;
	private int playergold = 0;
	private boolean finishedbuying = false;
	private boolean banked = false;
	private boolean firstwindow_offered = false;
	private int online = 0;
	private long starttime = 0;
	private int amuletcharges = 0;
	private boolean requestamulet = false;
	private boolean requestrunes = false;
	private int runeamount = 0;
	private int[] selectedset;
	
	// SETTINGS
	private int moneytograb = 1000000;
	private int minmoney = 2000;
	
	
	
	
	
	
	
	@Override
	public void run() {
		usedworlds.clear();
		int[][] worldsets = splitArray(p2pworlds, 10);
		String worldset = JOptionPane.showInputDialog("Which worldset to use? (1-10), you can use ranges like 2-4");
		if (worldset != null || worldset != "") {
		if (worldset.contains("-")) {
			String[] wsets = (worldset.split("\\-"));
			int wmin = Integer.parseInt(wsets[0])-1;
			int wmax = Integer.parseInt(wsets[1])-1;
			ArrayList<Integer> sets = new ArrayList<Integer>();
			for (int i=wmin; i<=wmax; i++) {
				for(int w : worldsets[i]) {
					sets.add(w);
				}
			}
			selectedset = sets.stream().mapToInt(Integer::valueOf).toArray();
			println("Selected worldsets: " + (wmin+1) + "-" + (wmax+1) + ". (" + Arrays.toString(selectedset) + ")");
		} else {
		int wset = Integer.parseInt(worldset)-1;
		selectedset = worldsets[wset];
		println("Selected worldset: " + worldset + ". (" + Arrays.toString(selectedset) + ")");
		}
		
		starttime = Instant.now().toEpochMilli();
		screenshottime = Instant.now();
		
		if (Login.getLoginState() == STATE.INGAME) {
			botstart();
			try {
				
				takeScreenshot();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				println(e);
			}
		}
		
		while(true) {
			current_status = getStatus();
			if (Login.getLoginState() != STATE.INGAME) {
				online = 0;
			} else {
				online = 1;
			}
			checkdb();
			switch(current_status) {
			case TOSHOP:
				//println("Walking to shop..");
				RSTile dest = shop_area.getRandomTile();
				WebWalking.walkTo(dest);
				//println("Done");
				break;
			case BANK:
				//println("Going to bank..");
				if (WebWalking.walkTo(banktile)) {
					if (!Banking.isBankScreenOpen()) {
						Banking.openBank();
					} else {
						Banking.depositAllExcept("Coins");
					}
				} else {
					WebWalking.walkTo(banktile);
				}
				break;
			case MULETASK:
				
				RSItem[] amulet = Equipment.find(SLOTS.AMULET);
				if (amulet.length > 0) {
					String amname = amulet[0].getDefinition().getName().toString();
					amname = amname.replaceAll("\\)", "");
					String[] am = amname.split("\\(");
					
					if (am.length > 1) {
						amuletcharges = Integer.parseInt(am[1]);
					} else {
						amuletcharges = 0;
					}
					if (amuletcharges == 0) {
						Equipment.remove(SLOTS.AMULET);
						requestamulet = true;
					} else {
						requestamulet = false;
					}
					
				}
				
				String mulename = dbrequest();
				if (!banked && !varrockarea.contains(Player.getRSPlayer()) && !area_ge.contains(Player.getRSPlayer())) {
					//println("Checking bank before going to mule..");
					if (WebWalking.walkTo(banktile)) {
						
						if (Banking.openBank()) {
							RSInterface bankwindow = Interfaces.get(12,24);
							RSItem[] teletabs = Banking.find("Varrock teleport");
							if (teletabs.length > 0) {
								runeamount = teletabs[0].getStack();
							} else {
								runeamount = 0;
							}
							RSItem[] invtabs = Inventory.find("Varrock teleport");
							if (invtabs.length > 0) {
								Banking.withdrawItem(invtabs[0], 1);
								runeamount += invtabs[0].getStack()-1;
							}
							if (runeamount == 0) {
								requestrunes = true;
							}
							//println("I have " + runeamount + " Varrock teleports and " + amuletcharges + " charges left in my Skills amulet.");
							if (bankwindow.click()) {
								for (String item : buylist) {
									String[] itemdb = item.split("\\|");
									RSItem[] bankitem = Banking.find(itemdb[0]);
									if (bankitem.length > 0) {
										Banking.withdrawItem(bankitem[0], bankitem[0].getStack());
									}
								}
								Banking.close();
								banked = true;
								break;
							}
						}
					}
				} else {
					
					if (!mulename.isEmpty()) {
						if (!area_ge.contains(Player.getRSPlayer())) {
							
								RSItem[] teletab = Inventory.find("Varrock teleport");
							if (teletab.length > 0) {
								println("Clicking teletab");
								teletab[0].click("Break " + teletab[0].getDefinition().getName().toString());
								General.sleep(General.random(5500, 10500));
								
								println("Walking");
								WebWalking.walkTo(GETile);
								break;
							}
							
							
							
							} else {
								
								RSPlayer[] targetplayer = Players.find(mulename);
								if (targetplayer.length > 0) {
									if (Trading.getWindowState() == null) {
										if (targetplayer[0].isOnScreen() && !targetplayer[0].isMoving() && targetplayer[0].isClickable()) {
											Camera.turnToTile(targetplayer[0].getPosition());
											targetplayer[0].click("Trade with " + targetplayer[0].getName());
										}
									} else {
										if (Trading.getWindowState() == WINDOW_STATE.FIRST_WINDOW) {
											if (!firstwindow_offered) {
											if (Inventory.open()) {
												RSItem[] inv = Inventory.getAll();
												for (RSItem invitem : inv) {
													if (invitem.getDefinition().getName().toString() != "Coins") {
														if (!invitem.getDefinition().getName().toString().equals("Varrock teleport"))
														Trading.offer(invitem, invitem.getStack());
													}
												}
												General.sleep(1000);
												Trading.accept();
											}
											} else {
												Trading.accept();
											}
										} else if (Trading.getWindowState() == WINDOW_STATE.SECOND_WINDOW) {
											if (Trading.accept()) {
												firstwindow_offered = false;
												//println("Got my moneyz, time to return to shop..");
												
												
												if (Equipment.getItem(SLOTS.AMULET) != null) {
													RSItem am = Equipment.getItem(SLOTS.AMULET);
													am.click("Fishing Guild");
													General.sleep(General.random(1500, 3000));
													current_status = status.TOSHOP;
													break;
												} else {
													RSItem[] invam = Inventory.find("Skills necklace(6)");
													if (invam.length > 0) {
														invam[0].click("Wear");
														RSItem am = Equipment.getItem(SLOTS.AMULET);
														am.click("Fishing Guild");
														General.sleep(General.random(1500, 3000));
														current_status = status.TOSHOP;
														break;
													}
												}
												break;
											}
										}
									}
								} else {
									// Waiting for mule
								}
							}
					} else {
						//println("Calling for mule..");
					}
				}
				
				break;
				
				
			case WORLDHOP:
				//println("Trying to hop a world..");
				int rworld = selectedset[General.random(0, selectedset.length-1)];
				if (!usedworlds.contains(rworld)) {
					int curworld = Game.getCurrentWorld();
						if (Game.getCurrentWorld() != rworld) {
							println("Hopping to " + rworld);
							if (WorldHopper.openWorldSelect()) {
								
								if (NWorldHopper.changeWorld(rworld)) {
									//println("Hopped");
									usedworlds.add(Game.getCurrentWorld());
								}
							}
						if (Login.getLoginResponse().contains("Too many login")) {
							this.setLoginBotState(false);
							println("Too many login attempts.. Waiting 30 seconds before logging back in..");
							General.sleep(30000);
							this.setLoginBotState(true);
						}
						finishedbuying = false;
						}
						break;
				}
				if (usedworlds.size() == selectedset.length) {
					usedworlds.clear();
					println("Hopped all worlds, going for next round..");
				}
				
				
				break;
			case BUY:
				RSInterface buywindow = Interfaces.get(300,16);
				if (buywindow != null) {
					finishedbuying = false;
					for (String item : buylist) {
						String[] itemdb = item.split("\\|");
						if (itemdb.length > 0) {
							String buylistname = itemdb[0];
							int buylistcount = Integer.parseInt(itemdb[1]);
							RSInterface[] itemoffers = buywindow.getChildren();
							if (!itemoffers.equals(null)) {
									for (RSInterface child : itemoffers) {
										String name = child.getComponentName();
										name = name.replace("<col=ff9040>", "");
										name = name.replace("</col>", "");
										int count = child.getComponentStack();
										if (name != "") {
											if (name.equals(buylistname)) {
												while (count > buylistcount && !Inventory.isFull()) {
													Mouse.setSpeed(3000);
													count = buysellitem(child, count-buylistcount, buylistcount, "buy");
													Mouse.setSpeed(100);
												}
											}
										}
									}
							}
						}
					}
					finishedbuying = true;
					RSInterface closebtn = Interfaces.get(300, 1, 11);
					closebtn.click();
					
					break;
					
					
				} else {
					RSNPC[] targetnpc = NPCs.findNearest("Gulluck");
					if (targetnpc.length > 0) {
						targetnpc[0].click("Trade " + targetnpc[0].getName());
					}
				}
				
				break;
			case SCREENSHOT:
				try {
					takeScreenshot();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					println(e);
				}
				break;
			case IDLE:
				
				
				break;
			}
			
			General.sleep(General.random(500, 1500));
		}
		
		}
	}
	
	private int buysellitem(Clickable item, int amountleft, int stopamount, String method) {
		Point itempos = new Point();
		int itemam = 0;
		if (method == "buy") {
			RSInterface itemm = (RSInterface) item;
			Rectangle itemmbox = itemm.getAbsoluteBounds();
			int x = (int) itemmbox.getCenterX();
			int y = (int) itemmbox.getCenterY();
			int rx = General.random(x-10, x+10);
			int ry = General.random(y-10, y+10);
			itempos = new Point(rx, ry);
			itemam = itemm.getComponentStack();
		} else if (method == "sell") {
			RSItem invitem = (RSItem) item;
			Rectangle invitembox = invitem.getArea();
			int x = (int) invitembox.getCenterX();
			int y = (int) invitembox.getCenterY();
			int rx = General.random(x-10, x+10);
			int ry = General.random(y-10, y+10);
			itempos = new Point(rx, ry);
			itemam = invitem.getStack();
		}
		if (amountleft > 0 && itemam > 0) {
			Point buypos = new Point();
			String option = "";
			if ( amountleft >= 50) {
				buypos = new Point((int) itempos.getX(),(int) itempos.getY()+85);
				option = "50";
			} else if (amountleft >= 10 && amountleft < 50) {
				buypos = new Point((int) itempos.getX(),(int) itempos.getY()+70);
				option = "10";
			} else if (amountleft >= 5 && amountleft < 10) {
				buypos = new Point((int) itempos.getX(),(int) itempos.getY()+55);
				option = "5";
			} else if (amountleft < 5 && amountleft > 0) {
				buypos = new Point((int) itempos.getX(),(int) itempos.getY()+40);
				option = "1";	
			}
			Mouse.click(itempos, 3);
			General.sleep(35);
			if (ChooseOption.isOpen()) {
				if (ChooseOption.isOptionValid("Buy " + option)) {
					if (itemam >= Integer.parseInt(option)) {
						Mouse.click(buypos, 1);
						amountleft -= Integer.parseInt(option);
					}
				} else if (ChooseOption.isOptionValid("Sell " + option)) {
					if (itemam >= Integer.parseInt(option)) {
						Mouse.click(buypos, 1);
						amountleft -= Integer.parseInt(option);
					}
				} else {
					ChooseOption.close();
				}
			} else {
				Mouse.click(itempos, 3);
				General.sleep(35);
			}
			
		}
			return amountleft;
		} 
	
	private String dbrequest() {
		
		try {
			String pname = Player.getRSPlayer().getName().toString();
			int currentworld = Game.getCurrentWorld();
			pname = pname.replace(" ", "%20");
			pname = pname.replaceAll("\\W", "%20");
			String locationn = "Grand%20Exchange";
			ArrayList<String> ritems = new ArrayList<String>();
			ritems.add("Coins_"+moneytograb);
			if (requestamulet) {
				ritems.add("Skills%20necklace(6)_1");
			}
				ritems.add("Varrock%20teleport_1");
	        
		String webPage = "http://localhost/botnetwork/bhrequest.php?bot=" + pname + "&request=bank&world=" + currentworld + "&loc=" + locationn + "&requestitems=" + (String) ritems.toString().replaceAll("\\,\\ ", ",%20");
		
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
		println("Exception occurred at dbrequest." + e);
		return "";
		}
	}
	
	private String checkdb() {
		try {
			String pname = Player.getRSPlayer().getName().toString().replaceAll("\\W", "%20");
    	String webPage = "http://localhost/botnetwork/bhstatus.php?bot=" + pname + "&type=buyer&online=" + online + "&status=" + current_status.toString() + "&stime=" + starttime/1000 + "&botloc=Tree%20Gnome%20Village";
    	
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
    		
    	println("Exception occurred at connectdb." + e);
    	return "";
    	}

	}
	
	public void takeScreenshot() throws IOException {
		BufferedImage screenshot = Screenshots.getScreenshotImage();
		if (screenshot != null) {
			String pname = Player.getRSPlayer().getName().toString();
			pname = pname.replaceAll("\\W", " ");
			String dir = Util.getWorkingDirectory() + "\\botscreenshots\\" + pname + "\\";
			long noww = Instant.now().toEpochMilli()/1000;
			String filename = noww + ".png";
			
			File directory = new File(dir);
			if (!directory.exists()) {
				directory.mkdirs();
				File file = new File(dir + filename);
				if (file != null) {
					if (ImageIO.write(screenshot, "PNG", file)) {
						screenshottime = Instant.now();
					}
				}
			} else {
				File filee = new File(dir + filename);
				if (filee != null) {
					if (ImageIO.write(screenshot, "PNG", filee)) {
						screenshottime = Instant.now();
					}
				}
			}
		}
	}
	
	private status getStatus() {
		if (Duration.between(screenshottime, Instant.now()).toMinutes() >= 10 ) {
			return status.SCREENSHOT;
		}
		if (Equipment.getItem(SLOTS.AMULET) != null && Equipment.getItem(SLOTS.AMULET).getDefinition().getName().equals("Skills necklace")) {
			Equipment.remove(SLOTS.AMULET);
		}
		if (Inventory.open()) {
		playergold = Inventory.getCount("Coins");
		if (playergold <= minmoney) {
			return status.MULETASK;
		}
		if (Inventory.isFull()) {
			return status.BANK;
		}
		if (!shop_area.contains(Player.getRSPlayer())) {
			return status.TOSHOP;
		} else {
			if (finishedbuying == false) {
				return status.BUY;
			} else if (finishedbuying == true){
				return status.WORLDHOP;
			}
		}
		}
		return status.IDLE;
	}
	
	public static int[][] splitArray(int[] arrayToSplit, int chunkSize){
	    if(chunkSize<=0){
	        return null;  // just in case :)
	    }
	    // first we have to check if the array can be split in multiple 
	    // arrays of equal 'chunk' size
	    int rest = arrayToSplit.length % chunkSize;  // if rest>0 then our last array will have less elements than the others 
	    // then we check in how many arrays we can split our input array
	    int chunks = arrayToSplit.length / chunkSize + (rest > 0 ? 1 : 0); // we may have to add an additional array for the 'rest'
	    // now we know how many arrays we need and create our result array
	    int[][] arrays = new int[chunks][];
	    // we create our resulting arrays by copying the corresponding 
	    // part from the input array. If we have a rest (rest>0), then
	    // the last array will have less elements than the others. This 
	    // needs to be handled separately, so we iterate 1 times less.
	    for(int i = 0; i < (rest > 0 ? chunks - 1 : chunks); i++){
	        // this copies 'chunk' times 'chunkSize' elements into a new array
	        arrays[i] = Arrays.copyOfRange(arrayToSplit, i * chunkSize, i * chunkSize + chunkSize);
	    }
	    if(rest > 0){ // only when we have a rest
	        // we copy the remaining elements into the last chunk
	        arrays[chunks - 1] = Arrays.copyOfRange(arrayToSplit, (chunks - 1) * chunkSize, (chunks - 1) * chunkSize + rest);
	    }
	    return arrays; // that's it
	}
	
	
private void botstart() {
		
		try {
			String pname = Player.getRSPlayer().getName().toString();
    		pname = pname.replace(" ", "%20");
    		pname = pname.replaceAll("\\W", "%20");
    	String webPage = "http://localhost/botnetwork/bhstart.php?bot=" + pname + "&type=buyer&world=" + Game.getCurrentWorld() + "&status=" + current_status;
    	
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
    	} catch (Exception e) {
    	println("Exception occurred at botstart." + e);
    	}
	}
	
	
	
	
	

}
