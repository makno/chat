/**
 * File			: ClientThread.java
 * Package		: securechat.server
 * Classes		: ClientThread
 * Description	: A client's thread which resides in the servers's thread pool
 * 
 * Author		: Mathias Knoll
 * Year			: 2009
 * Subject		: Applied Cryptography (William Farrelly)
 * 
 * FH JOANNEUM - Kapfenberg - 2009
 */
package securechat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.CharBuffer;

// Have the security library imported ;-)
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import securechat.tools.CryptoTools;

/**
 * ClientThread:
 * The client thread opens input and output streams for a particular client,
 * asks for client's name, informs all other clients currently connected of the
 * the new client, and as long as it receives data, echos that data back to all 
 * other clients. When the client leaves the thread it informs all clients 
 * about it and terminates.
 * @author Mathias Knoll
 */
class ClientThread extends Thread {

	// Read inputs
	private BufferedReader readerInput = null;
	// Output stream
	private PrintStream printstreamOut = null;
	// The client's socket
	private Socket socketClient = null;
	// The pool of client threads
	private ClientThread clientthreadPool[];
	// Thread's name
	private String sThreadName = null;
	// User's name
	private String sUserName = null;	
	// Flag which tells if cryptography is initialized properly
	private boolean bIsCryptoInitialized =  false;
	// If signature is verified correctly
	private boolean bIsCryptoVerfied = false;	
	// Flag if communication is secure!
	private boolean bSecure = false;	
	// Secret key
	private SecretKey keySession;
	// Key Pair
	private KeyPair keyPair;
	// Public key of client
	private PublicKey keyPublicClient; 
	
	/**
	 * Constructor of class ClientThread
	 * Handles a single client's thread 
	 * @param sThreadName The thread's  name
	 * @param clientSocket The client's socket
	 * @param clientthreadPool The pool of client threads
	 */
	public ClientThread(
			String sThreadName,
			Socket clientSocket, 
			ClientThread[] clientthreadpool) {
		this.sThreadName = sThreadName;
		this.socketClient = clientSocket;
		this.clientthreadPool = clientthreadpool;
		
		// Initializing keys and other stuff
		this.bIsCryptoInitialized = this.initializeCryptography();
	}
	
	/** 
	 * Initializing keys and other stuff
	 */
	private boolean initializeCryptography(){
		try {
			
			// Key generator for AES
			KeyGenerator generatorKeys = KeyGenerator.getInstance("AES");
			generatorKeys.init(128);
			this.keySession = generatorKeys.generateKey();
			
			// Key generator for RSA
			KeyPairGenerator generatorKeyPair = KeyPairGenerator.getInstance("RSA");
	   	 	generatorKeyPair.initialize(1024);
	   		this.keyPair = generatorKeyPair.generateKeyPair();
			
	   		return true;
	   		
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Error: Crypto algorithm.");
			System.out.println(e.getMessage());
			return false;
		}
	}
	
	/**
	 * Get public key
	 * @return byte array 
	 */
	public byte[] getPublicKey(){
		return this.keyPair.getPublic().getEncoded();
	}
	
	/**
	 * Running the thread
	 */
	public synchronized void run() {
		
		// Line of text which is typed in
		String line = null;
		
		try {
			// Establish input reader
			this.readerInput = 
				new BufferedReader(
					new InputStreamReader(
						this.socketClient.getInputStream()));
			// Establish output stream
			this.printstreamOut = 
				new PrintStream(
					this.socketClient.getOutputStream());
			
			// Get name of user
			this.printstreamOut.println(
				"aseChat1.0/Applied Cryptography/Mathias Knoll/ASE08/2009");
			this.printstreamOut.println(
				"Enter your name.");
			this.sUserName = this.readerInput.readLine();		
			this.sThreadName = this.sThreadName + "-" + this.sUserName;
			
			// Welcome user with his name
			this.printstreamOut.println(
				"Hello " + this.sUserName + "! (Type \"/help\" for usage!)");
			
			System.out.println("New user " + sUserName + ".");
			
			// Deliver message of new user to all other users
			for (int i = 0; i < this.clientthreadPool.length; i++){
				// If thread is there and not this one...
				if (
					this.clientthreadPool[i] != null && 
					this.clientthreadPool[i] != this){
					
					this.clientthreadPool[i].printstreamOut.println(
						"*** New user: " + this.sUserName + " ***");
				}
			}
			
			// Start threads endless loop (until user quits)
			while (true) {
				// Get input
				line = readerInput.readLine();
				
				// If secured transfer is established:
				if(
					this.bSecure && 
					this.bIsCryptoVerfied && 
					this.keySession != null){
					
					try {
						line = new String(
								CryptoTools.decryptData(
									this.keySession, 
									CryptoTools.getBytes(line)));
					} catch (Exception e) {
						line = "[" + this.sThreadName + "] Decrypting failed!";
					}	
					
				}
					
				// If '/quit' is typed in, we'll leave the loop
				if (line.startsWith("/quit"))
					break;
				if (line.startsWith("/help")){
					this.giveHelp();
				}else if (line.startsWith("/users")){
					this.giveUsers();
				}else if (line.startsWith("/secure")){
					this.secureChat();
				}else if (line.startsWith("[public_key]")){
					this.storeKeyPublicClient(line.substring(12));
				}else if (line.startsWith("[signed_key]")){
					this.storeSignedSessionKey(line.substring(12));
				}else{
					// Send input to all other clients in pool
					for (int i = 0; i < this.clientthreadPool.length; i++){
						if (this.clientthreadPool[i] != null){
							
							this.clientthreadPool[i].printChat(
									"<" + this.sUserName + "> " + line,
									this.bIsCryptoVerfied);
							
							//this.clientthreadPool[i].printstreamOut.println(
							//	"<" + this.sUserName + "> " + line);
						}
					}
				}
			}
			
			// Inform other clients that this on leaves
			for (int i = 0; i < this.clientthreadPool.length; i++){
				if (
					this.clientthreadPool[i] != null && 
					this.clientthreadPool[i] != this) {
					
					this.clientthreadPool[i].printstreamOut.println(
						"*** User " + this.sUserName + " has left ***");
				}
			}
			
			// Leaving message to this client
			this.printstreamOut.println("*** Bye " + this.sUserName + " ***");

			this.cleanup();
			
		} catch (IOException e) {
			System.out.println(
				"["+this.sThreadName+"] Failure in client thread, " +
				"cleaning up ...");
			this.cleanup();
		}

	}

	/**
	 * Activate secure transfer of data!
	 */
	private void secureChat(){
		this.bSecure = true;
		System.out.println(
			"User " + this.sUserName + "("+ this.sThreadName + 
			") demands security!");
		try {
			System.out.println("["+this.sThreadName+"] send public key!");
			this.printstreamOut.println(
				"[public_key]" +
				CryptoTools.getHex(this.getPublicKey()));
		} catch (Exception e) {
			System.out.println(
				"["+this.sThreadName+"] Exception sending public key: +" +
				e.getMessage());
		}
	}
	
	public void printChat(String sLine, boolean bIsCryptoVerfied){
	
		String sLineNew = "";
		
		// If secured transfer is established:
		if(
			this.bSecure && 
			this.bIsCryptoVerfied && 
			this.keySession != null){
			
			try {
				sLineNew = 
					CryptoTools.getHex(
						CryptoTools.encryptData(
							this.keySession, 
							sLine.getBytes()));
			} catch (Exception e) {
				sLineNew = "Encrypting failed!";
			}	
		}
		// Only send if encryption is defined
		if( bIsCryptoVerfied)
			this.printstreamOut.println(sLineNew);
		else
			this.printstreamOut.println(sLine);
	}
	
	/**
	 * Store away the clients public key!
	 * @param sKey
	 */
	private void storeKeyPublicClient(String sKey){
		System.out.println("["+this.sThreadName+"] Got public key: " + sKey);
		if (! sKey.startsWith("FAIL")){
			X509EncodedKeySpec publicKeySpec=new X509EncodedKeySpec(
				CryptoTools.getBytes(sKey));
			KeyFactory keyFactory;
			try {
				keyFactory = KeyFactory.getInstance("RSA");
			this.keyPublicClient = keyFactory.generatePublic(publicKeySpec);
			try {
				System.out.println(
						"["+this.sThreadName+"] send encrypted session key!");
				this.printstreamOut.println(
					"[session_key]" +
					CryptoTools.getHex(
						CryptoTools.cipherKey(
							this.keyPublicClient, 
							this.keySession.getEncoded())));
			} catch (Exception e) {
				System.out.println(
					"["+this.sThreadName+"] Exception sending public key: +" +
					e.getMessage());
			}
			} catch (NoSuchAlgorithmException e) {
				this.keyPublicClient = null;
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {	
				this.keyPublicClient = null;
				e.printStackTrace();
			}	
		}else{
			this.keyPublicClient = null;
		}
	}
	
	/**
	 * Store away the clients public key!
	 * @param sKey
	 */
	private void storeSignedSessionKey( String sKey){
		this.bIsCryptoVerfied = 
			CryptoTools.verifySignedKey(
				this.keyPublicClient, 
				this.keySession.getEncoded(), 
				CryptoTools.getBytes(sKey) );
		if(this.bIsCryptoVerfied){
			System.out.println(
					"[" + this.sThreadName + "] successfully verifed key!");
			this.printstreamOut.println("[verify_key]OK");
		}else{
			System.out.println(
					"[" + this.sThreadName + "] did NOT verify key!");
			this.printstreamOut.println("[verify_key]FAIL");
		}
	}
	
	/**
	 * Give basic help on functionality!
	 */
	private void giveHelp(){
		this.printstreamOut.println("Usage:");
		this.printstreamOut.println("To leave enter \"/quit\".");
		this.printstreamOut.println("For help type \"/help\".");
		this.printstreamOut.println("For user infos type \"/users\".");
	}
	
	/** 
	 * List of users in chat - just for fun
	 */
	private void giveUsers(){
		this.printstreamOut.println(
			"Clients in chat: " + Server.getActiveClients());
		for (int i = 0; i < this.clientthreadPool.length; i++){
			if (
				this.clientthreadPool[i] != null) {
				this.printstreamOut.println(this.clientthreadPool[i].sUserName);
			}
		}
	}
	
	/** 
	 * General cleaning up of client thread pool and all open streams!
	 */
	private void cleanup(){
		// Set this thread to null in client thread pool
		for (int i = 0; i < this.clientthreadPool.length; i++){
			if (this.clientthreadPool[i] == this) {
				this.clientthreadPool[i] = null;
			}
		}
		
		// Close all streams and connections
		try {
			if(this.readerInput!=null)
				this.readerInput.close();
		} catch (IOException e) {
			System.out.println("Error: Closing reader input!");
		}
		
		if(this.printstreamOut!=null)
			this.printstreamOut.close();
		
		try {
			if(this.socketClient!=null)
				this.socketClient.close();
		} catch (IOException e) {
			System.out.println("Error: Closing client socket!");
		}
		System.out.println("Removing user " +  sUserName + ".");
		System.out.println("Clients in chat: " + Server.getActiveClients());
	}
}
