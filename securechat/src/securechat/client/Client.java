/**
 * File			: Client.java
 * Package		: securechat.client
 * Classes		: Client
 * Description	: Client's class with only a socket connection to the server
 * 
 * Author		: Mathias Knoll
 * Year			: 2009
 * Subject		: Applied Cryptography (William Farrelly)
 * 
 * FH JOANNEUM - Kapfenberg - 2009
 */
package securechat.client;

// Imports
import java.io.*;
import java.net.*;
import java.security.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import securechat.tools.CryptoTools;

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
	@SuppressWarnings("unused")
	private String sHost = Client.HOST;
	// Port
	@SuppressWarnings("unused")
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
	// Flag for secure transfer
	private boolean bIsSecure = false;	
	// RSA Key
	private KeyPair keyPair = null;	
	// Public key
	@SuppressWarnings("unused")
	private String keyPublicClientThread = null;	
	// Session Key
	private SecretKey keySession = null;	
	// Verification
	private boolean bIsVerified = false;	
	
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
		
		try {
			// Create keys for client
			this.createKeys();
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Key generation failed:");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Key saving failed:");
			e.printStackTrace();
		}
	}
	
	/**
	 * Establish communication
	 */
	private void initialize(){
		
		// Outgoing line of text
		String sLine = null;
		
		try {		
			// Establish socket to server
			this.socketClient = new Socket(Client.HOST, Client.PORT);
			
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

					// If secured transfer is established:
					if(
						this.bIsSecure && 
						this.bIsVerified && 
						this.keySession != null){
						
						try {
							sLine = 
								CryptoTools.getHex(
									CryptoTools.encryptData(
										this.keySession, 
										sLine.getBytes()));
						} catch (Exception e) {
							sLine = "*** Encrypting failed! ***";
						}	
						
					}
					
					// Deliver stream to client thread
					this.streamOut.println(sLine);
				
					// Handle operation codes (/secure)
					if(sLine.equals("/secure")){
						try {
							this.streamOut.println(
								"[public_key]" +
								CryptoTools.getHex(
									this.keyPair.getPublic().getEncoded()));
							this.bIsSecure = true;
						} catch (Exception e) {
							this.streamOut.println("[public_key]FAIL");
							System.out.println(
								"[Client] [public_key] Exception: "+
								e.getMessage());
						}
					}
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
				
				// If secured transfer is established:
				if(
					this.bIsSecure && 
					this.bIsVerified && 
					this.keySession != null){
					
					try {
						line = new String(
								CryptoTools.decryptData(
									this.keySession, 
									CryptoTools.getBytes(line)));
						line = line + " [ENCRYPTED]";
					} catch (Exception e) {
						// Inform chatter of unencrypted text!
						line = line + " [UNENCRYPTED]";
					}	
					
				}
				
				// Operation: public key transmitted
				if(line.startsWith("[public_key]")){
					this.keyPublicClientThread = line.substring(12);
					System.out.println("*** Got public key from server! ***");
				}
				// Operation: session key transmitted
				else if(line.startsWith("[session_key]")){ 
					this.keySession = new SecretKeySpec(
						CryptoTools.decipherKey(
							this.keyPair.getPrivate(), 
							CryptoTools.getBytes(line.substring(13))
						)
					,"AES");
					System.out.println("*** Got session key ***");
					
					try {
						this.streamOut.println(
							"[signed_key]" + 
							CryptoTools.getHex(
								CryptoTools.signedKey(
									this.keyPair.getPrivate(),
									this.keySession.getEncoded()
								)
							)	
						);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				// Operation: verifying transmitted
				else if(line.startsWith("[verify_key]")){
					this.bIsVerified = line.substring(12).equalsIgnoreCase("OK");
					System.out.println("*** Got verification from server! ***");	
				}
				// Deliver line at last
				else{
					System.out.println(line);
				}
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
	
	/**
	 * Create RSA keys
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private void createKeys() throws NoSuchAlgorithmException, IOException {
		//Create key generator (RSA asymmetric encryption)
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		//Use 1024 bit keys
		keyGen.initialize(1024);
		//Generate keyPair
		this.keyPair = keyGen.generateKeyPair();
	}
	
}
