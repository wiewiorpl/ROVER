
package rover;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Writer;

/**
 * @author Kamil Uszyński, Tomasz Bayer Tested, working properly. Do not touch
 *         except for marked places!
 */

public class Rover {

	public static volatile boolean atWork = true;
	public static volatile boolean atWorkArduino = false;
	public static volatile List<String> measurements = new ArrayList<String>();
	public static volatile boolean connected;
	public static volatile String backupFile = "backup.txt";
	public static volatile String comPort = ""; // Do edycji
	public static volatile int comBaud = 9600; // Nie zmieniaj, polecimy po defaulcie dla bezpieczeństwa.

	@SuppressWarnings("unused")
	public static void main(String[] args) throws ClassNotFoundException, IOException {

		System.out.println("Current IP: " + InetAddress.getLocalHost().getHostAddress());

		Thread manual = new Thread() {

			@SuppressWarnings("resource")
			public void run() {

				try {

					ServerSocket ss = new ServerSocket(10001);
					System.out.println("Manual Override: Listening for connections on port 10001...");
					Socket s = ss.accept();
					System.out.println("Manual Override: Incoming connection on port 10001");
					ObjectInputStream in = new ObjectInputStream(s.getInputStream());
					connected = true;
					Data oobj = (Data) in.readObject();
					String command = oobj.c;

					while (atWork) {

						switch (command) {

						case "return": {

							System.out.println("Manual Override: Emergency return to base.");
							System.out.println("Manual Override: Uploading data.");
							oobj.setC("dataul");
							oobj.setM(measurements);
							ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
							out.writeObject(oobj);
							Rover.atWork = false;
							Rover.atWorkArduino = false;
							System.out.println("Manual Override: Data upload complete");
							System.out.println("Manual Override: Entering power saving mode");
							break;

						}

						case "datadl": {

							System.out.println("Manual Override: Data request");
							oobj.setM(measurements);
							Writer output;
							output = new BufferedWriter(new FileWriter(backupFile, true));

							for (String entry : measurements) {

								output.append(entry);

							}

							output.close();
							System.out.println("Manual Override: Uploading Data");
							ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
							out.writeObject(oobj);
							System.out.println("Manual Override: Done uploading, clearing data buffer");
							measurements.clear();
							command = "";
							break;

						}

						default: {

							System.out.println("Manual Override: Waiting...");
							oobj = (Data) in.readObject();
							command = oobj.getC();

						}
						}
					}

					System.out.println("Manual Override: Entering offline mode");

				} catch (ClassNotFoundException | IOException e) {

					System.out.println("Manual Override: Rover offline");

				}
			}
		};

		Thread auto = new Thread() {

			@SuppressWarnings("resource")
			public void run() {

				try {

					ServerSocket ss = new ServerSocket(10002);
					System.out.println("Auto Transfer: Listening for connections on port 10002...");
					Socket s = ss.accept();
					atWorkArduino = true;

					System.out.println("Auto Transfer: Incoming connection on port 10002");
					Data aobj = new Data();

					while (atWork) {

						if (measurements.size() > 29) {

							aobj.setC("data");
							aobj.setM(measurements);
							System.out.println("Auto Transfer: Uploading data...");
							ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
							out.writeObject(aobj);
							System.out.println("Auto Transfer: Upload Done");
							Writer output;
							output = new BufferedWriter(new FileWriter(backupFile, true));

							for (String entry : measurements) {

								output.append(entry + "\n");

							}

							output.close();
							System.out.println("Auto transfer: Data backup saved");
							measurements.clear();

						}
					}

				} catch (IOException e) {

					System.out.println("Auto Transfer: Rover offline");

				}
			}
		};

		Thread arduino = new Thread() {

			public void run() {
				while (true) {
					String s = "";
					StringBuilder sb = new StringBuilder();
					while (atWorkArduino) {

						SerialPort comPort = SerialPort.getCommPort("/dev/ttyACM0");
						comPort.openPort();

						try {

							while (true) {

								boolean all = true;
								boolean newstr = true;
								while (newstr) {
									byte[] readBuffer = new byte[comPort.bytesAvailable()];
									int numRead = comPort.readBytes(readBuffer, readBuffer.length);

									for (byte b : readBuffer) {
										char c = (char) b;
										if (c != '\n') {

											sb.append(c);

										} else {

											s = sb.toString();
											System.out.println(s);
											measurements.add(s);
											sb = new StringBuilder();
										}
									}
								}
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
						comPort.closePort();
					}
				}
			};
		};
		init();
		auto.start();
		manual.start();
		arduino.start(); // Odkomentuj po stworzeniu kodu dla arduino

	}

	public static void init() {

		System.out.println("Init: startup configuration");
		File file = new File(backupFile);
		FileOutputStream out = null;

		if (file.exists()) {

			System.out.println("Init: Found data backup file: " + backupFile);

		} else {

			System.out.println("Init: Data backup file not found! Creating file: " + backupFile);

			try {

				out = new FileOutputStream(backupFile);

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
