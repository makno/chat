/**
 * File			: Server.java
 * Package		: asechat.server
 * Classes		: Server
 * Description	: Run chat server and control client threads
 * 
 * Author		: Mathias Knoll
 * Year			: 2009
 * Subject		: Applied Cryptography (William Farrelly)
 * 
 * FH JOANNEUM - Kapfenberg - 2009
 */
package asechat.server;

// Imports
import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;

/**
 * Server:
 * This class builds up the server and contains a pool of client threads 
 * where to any accepted connection is put into.
 * @author Mathias Knoll
 */
public class Server {

	static int PORT = 8888;
	
	// Socket for clients
	static Socket socketClient = null;
	// Socket for server
	static ServerSocket socketServer = null;
	// This chat server can accept up to 10 clients' connections
	static ClientThread clientthreadPool[] = new ClientThread[10];

	public static void main(String args[]) {
		// Numbers of Threads
		int iThreadNumber = 0;
		
		// Default port
		int iPort = Server.PORT;
		
		// Given port
		if(args.length == 1){
			iPort = Integer.parseInt(args[0]);
		}
		
		// Try to open a server socket on port port_number (default 8888)
		// Note: Ports less than 1023 can only be defined by privileged users 
		try {
			Server.socketServer = new ServerSocket(iPort);
		}// try
		catch (IOException e) {
			System.out.println("Error: Open server socket on port " + iPort);
			System.out.println(e);
			System.exit(1);
		}
		
		System.out.println("Server up and running ...");
		
		// Create a socket object from the ServerSocket to listen and accept
		// connections. Open input and output streams for this socket will be 
		// created in client's thread since every client is served by the 
		// server in an individual thread
		while (true) {
			try {
				Server.socketClient = Server.socketServer.accept();
				iThreadNumber++;
				
				ClientThread clientthreadTmp = 
					new ClientThread(
						"Client-" + iThreadNumber,
						Server.socketClient, 
						Server.clientthreadPool);
				if(Server.add2Pool(clientthreadTmp)){
					clientthreadTmp.start();
				}else{
					System.out.println("No slots free for new client!");
				}
				
				System.out.println(
					"New client thread accepted. (Thread:" + 
					"Client-" + iThreadNumber + ")");
				System.out.println(
					"Clients in chat: " + Server.getActiveClients());
	
			}// try
			catch (IOException e) {
				System.out.println("Error: Accepting a client");
				System.out.println(e);
			}
		}
	}
	
	/**
	 *  Add client thread to thread pool
	 * @param clientthreadNew The new thread of a client
	 * @return True if a slot was free, otherwise false
	 */
	public static synchronized boolean add2Pool(ClientThread clientthreadNew){
		boolean bInPool = false;
		for(int i=0; i<Server.clientthreadPool.length;i++){
			if(Server.clientthreadPool[i]==null){
				Server.clientthreadPool[i] = clientthreadNew;
				bInPool = true;
				break;
			}
		}
		return bInPool;
	}
	
	/**
	 * Get number of active client threads in pool
	 * @return Number of threads in pool
	 */
	public static int getActiveClients(){
		int iCount = 0;
		for(int i=0; i < Server.clientthreadPool.length; i++){
			if(Server.clientthreadPool[i] != null)
				iCount++;
		}
		return iCount;
	}
}


