/**
 * File			: ClientThread.java
 * Package		: chat.server
 * Classes		: ClientThread
 * Description	: A client's thread which resides in the servers's thread pool
 * 
 * Author		: Mathias Knoll
 * Year			: 2009 de-secured in 2014
 * 
 * FH JOANNEUM - Kapfenberg - 2009
 */
package chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;


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
				"Chat by Mathias Knoll (C2009)");
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
				
				
					
				// If '/quit' is typed in, we'll leave the loop
				if (line.startsWith("/quit"))
					break;
				if (line.startsWith("/help")){
					this.giveHelp();
				}else if (line.startsWith("/users")){
					this.giveUsers();
				}else{
					// Send input to all other clients in pool
					for (int i = 0; i < this.clientthreadPool.length; i++){
						if (this.clientthreadPool[i] != null){
							this.clientthreadPool[i].printChat("<" + this.sUserName + "> " + line);
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
	 * Print either encrypted or unencrypted stuff!
	 * @param sLine
	 */
	public void printChat(String sLine){
		this.printstreamOut.println(sLine);
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
