/**
 * File			: Client.java
 * Package		: chat.client
 * Classes		: Client
 * Description	: Client's class with only a socket connection to the server
 * 
 * Author		: Mathias Knoll
 * Year			: 2009 de-secured in 2014
 * 
 * FH JOANNEUM - Kapfenberg - 2009
 */
package chat.client;

// Imports
import java.io.*;
import java.net.*;

/**
 * Client:
 * This class connects to the server with a certain port.
 * @author Mathias Knoll
 */
public class Client extends Thread {
	
	// Default values
	static String HOST = "localhost";
	static int PORT = 8888;

	// Host
	private String sHost = Client.HOST;
	// Port
	private int iPort = Client.PORT;
	// Socket
	private Socket socketClient = null;
	// Input reader 
	private BufferedReader readerInput = null;
	// Output stream
	private PrintStream streamOut = null;
	// Reader for input line
	private BufferedReader readerLine = null;
	// Flag for stopping thread
	private boolean bStopped = false;	
	
	/**
	 * Main method
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Host and port have to be delivered
		if(args.length != 2){
			System.out.println("Please deliver arguments for host and port!");
		}else{
			// Initialize Client!
			Client client = new Client(args[0],Integer.parseInt(args[1]));
			client.initialize();
		}	
	}
	
	/**
	 * Constructor which generates keys at first
	 */
	public Client(String sHost, int iPort){
		
		// Get host
		this.sHost = sHost;
		
		// Get port
		this.iPort = iPort;
		
	}
	
	/**
	 * Establish communication
	 */
	private void initialize(){
		
		// Outgoing line of text
		String sLine = null;
		
		try {		
			// Establish socket to server
			this.socketClient = new Socket(this.sHost, this.iPort);
			
			// Reading input from system.in
			this.readerLine = 
				new BufferedReader(
					new InputStreamReader(
						System.in));
			
			// Stream out
			this.streamOut = new PrintStream(this.socketClient.getOutputStream());
		
			// Read input from socket
			this.readerInput = 
				new BufferedReader(
					new InputStreamReader(
							this.socketClient.getInputStream()));
	
		} catch (UnknownHostException e) {
			System.err.println(
				"Don't know about host " + 
				Client.HOST);
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println(
				"Couldn't get I/O for the connection to the host " + 
				Client.HOST);
			e.printStackTrace();
		}
		
		// If the sockets as well as the streams are defined we'll start the
		// thread
		if (
			this.socketClient != null && 
			this.streamOut != null && 
			this.readerInput != null) {
			
			try {
				// Start thread
				this.start();
				
				// Send read line
				while (!this.bStopped) {
					
					sLine = this.readerLine.readLine();
					
					// Deliver stream to client thread
					this.streamOut.println(sLine);
					
				}
				
				this.streamOut.close();
				this.readerInput.close();
				this.socketClient.close();
				
			} catch (IOException e) {
				System.err.println("IOException: " + e);
			}
		}
	}

	/**
	 * Running the thread reading 
	 */
	public void run() {
		
		// Incoming line of text
		String line = null;

		try {
			while ((line = this.readerInput.readLine()) != null) {
						
				System.out.println(line);
			
				// If message contains a 'bye' message- we'll leave the thread
				if (line.indexOf("*** Bye") != -1)
					break;
			}
			bStopped = true;
		} catch (IOException e) {
			System.out.println("Error: Server connection failed! Exiting!");
			// e.printStackTrace();
		}
	}

	
}
