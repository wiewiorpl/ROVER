package rover;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

/**
 * @author Kamil Uszyński, Tomasz Bayer
 * Tested, working properly.
 * Do not touch!
 */

public class Base {
	public static Data obj = new Data();
	public static volatile boolean atWork = true;
	public static volatile String roverIP;
	static Scanner scan = new Scanner(System.in);
	public static volatile String dataFile = "data.txt";

	@SuppressWarnings("resource")
	public static void main(String[] args) throws ClassNotFoundException, IOException {

		Thread manual = new Thread() {

			public void run() {

				Data oobj = new Data();
				boolean atWork = true;
				System.out.println("Manual Override: Connecting...");

				try {

					System.out.print("Manual Override: ");
					Socket c = connect(Base.roverIP, 10001);
					ObjectOutputStream out = new ObjectOutputStream(c.getOutputStream());
					String command = "";

					while (atWork) {

						System.out.println("Manual Override: Waiting...");

						switch (command) {

						case "return": {
							System.out.println("Manual Override: Requesting emergency return");
							oobj.setC("return");
							out.writeObject(oobj);
							ObjectInputStream in = new ObjectInputStream(c.getInputStream());
							oobj = (Data) in.readObject();
							List<String> temp = oobj.getM();
							System.out.println("Manual Override: Data Received");
							Writer output = new BufferedWriter(new FileWriter(dataFile, true));
							for (String entry : temp) {
								System.out.println(entry);
								output.append(entry + "\n");
							};
							output.close();
							System.out.println("Manual Override: Emregency return routine enabled");
							System.out.println("Manual Override: Entering power saving");
							atWork = false;
							break;
						}

						case "datadl": {
							System.out.println("Manual Override: Requesting data");
							oobj.setC("datadl");
							out.writeObject(oobj);
							ObjectInputStream in = new ObjectInputStream(c.getInputStream());
							oobj = (Data) in.readObject();
							List<String> temp = oobj.getM();
							System.out.println("Manual Override: Data Received");
							Writer output = new BufferedWriter(new FileWriter(dataFile, true));
							for (String entry : temp) {
								System.out.println(entry);
								output.append(entry + "\n");
							};
							output.close();
							System.out.println("Manual Override: Data Saved");
							command = "";
							break;
						}

						default: {
							command = scan.nextLine();
							break;

						}
						}
					}

				} 
				
				catch (IOException e) {
					
					System.out.println("Manual Override: Rover offline");
				
				} 
				
				catch (ClassNotFoundException e) {
					
					System.out.println("There was an error accessing .class file");
				
				}

			}
		};

		Thread auto = new Thread() {
			
			public void run() {

				try {
					
					System.out.println("Auto Transfer: Connecting...");
					System.out.print("Auto Transfer: ");
					Socket c = connect(Base.roverIP, 10002);
					Data aobj = new Data();
					System.out.println("Auto Transfer: Waiting...");
					ObjectInputStream in = new ObjectInputStream(c.getInputStream());
					
					while (atWork == true) {

							aobj = (Data) in.readObject();
							List<String> temp = aobj.getM();
							System.out.println("Auto Transfer: Data Received");
							
							Writer output = new BufferedWriter(new FileWriter(dataFile, true));
							for (String entry : temp) {
								output.append(entry + "\n");
							}
							output.close();
							System.out.println("Auto Transfer: Data Saved \n");
							in = new ObjectInputStream(c.getInputStream());
					}

				} 
				
				catch (IOException e) {
					System.out.println("Auto Transfer: Rover offline");

				} 
				
				catch (ClassNotFoundException e) {

					System.out.println("Auto Transfer: Rover software error");

				}
			}
		};

		init();
		System.out.println("");
		System.out.println("Connect to Rover: ");
		roverIP = scan.nextLine();
		System.out.println("Rover IP Address: " + roverIP);

		auto.start();
		manual.start();

	}

	public static Socket connect(String ip, int port) {

		Socket s = null;

		try {

			s = new Socket(ip, port);
			System.out.println("Connected");

		} 
		catch (IOException e) {
			
			System.out.println("Failed to connect");

		}

		return s;

	}

	public static void init() {
		
		System.out.println("Init: startup configuration");
		File file = new File(dataFile);
		FileOutputStream out = null;

		if (file.exists()) {
			
			System.out.println("Init: Found data backup file: " + dataFile);

		} else {
			
			System.out.println("Init: Data backup file not found! Creating file: " + dataFile);

			try {

				out = new FileOutputStream(dataFile);
				
			} catch (IOException e) {
				
				System.out.println("Init: Error creating file");

			} finally {
				
				if (out != null) {
					
					try {
						
						out.close();
						
					} catch (IOException e) {
						
						System.out.println("Init: Error closing file");

					}
				}
			}
		}
	}
}

