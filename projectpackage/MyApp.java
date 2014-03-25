package projectpackage; 

import java.io.*;
import java.net.*;
import java.util.*;

class ListenSocket implements Runnable {		
		private int id;
		private final Socket socket;
		private int connId = -1;
		private BufferedReader in = null;
		private PrintWriter out = null;
		private PrintWriter connectedout = null;
		public boolean accepted = false;
		public ListenSocket(Socket psck,int pid) {
			this.socket = psck;
			this.id = pid;
		}
		public int getId() {
			return this.id;
		}
		public int getConnId() {
			return this.connId;
		}
		public void setConnId(int pconid) {
			this.connId = pconid;
		}
		public PrintWriter getOutput() {
			return this.out;
		}
		public void setOutput(PrintWriter pout) {
			this.out = pout;
		}
		public String toString() {
			return "ID="+id+"; socket:"+socket.toString()+" connected to ID="+connId;
		}
		public void run() {
			try {
				System.out.println("Connection accepted: "+socket+" in thread ID = "+id);
				in = new BufferedReader(new InputStreamReader(socket.getInputStream())); //assign ListenSocket input (keyboard)
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true); // assign ListenSocket output (screen)
				out.println("Your ID is "+id); // inform client with which ID it has connected;				
				
				if (MyApp.socketCount == 1) {
					out.println("You are the only client");
				} else if (MyApp.socketCount > 1) { // there are waiting clients
					out.println("Available sockets :");
					for(ListenSocket ls : MyApp.socketmap.values()) {
						if (ls.getId() != getId()) { // do not show own connection ID - not interesting
							out.println("ID="+ls.getId()); // show available connection IDs
						}
					}
					out.println("Insert ID of socket to communicate with");
				}
				
				String cid = in.readLine(); // wait for input of connection ID
					
				connectedout = MyApp.socketmap.get(Integer.parseInt(cid)).getOutput(); // set to which ListenSocket from Map output typed messages 
				setConnId(Integer.parseInt(cid)); // set to which ID the thread has connected
					
				if ((getConnId() != -1) && (false == this.accepted)) { // initiate connection
					connectedout.println("ListenSocket ID="+id+" wants to start communication with you. To accept, enter "+id);
					setConnId(Integer.parseInt(cid)); // set to which ID the thread has connected
					MyApp.socketmap.get(getConnId()).accepted = true; // set on connected ID that connection is accepted
				} else {
					connectedout.println("ListenSocket ID="+getConnId()+" accepted communication request"); // inform init thread that connection was accepted
				}
				
				while(true) {
					String str = in.readLine();
					if (str.equals("END")) {
						connectedout.println(id+" SAYS "+str);	
						break;
					}		
					connectedout.println(str);	// output to connected ID screen				
				}
			} catch (IOException e) {
			} catch (NullPointerException e) {
				System.out.println("Client ID="+id+" disconnected");
			} finally {
				try {
					System.out.println("Close socket in thread ID = "+id);
					in.close();
					out.close();
					socket.close();
					MyApp.socketmap.get(connId).setConnId(-1);
					MyApp.socketmap.get(connId).accepted = false;
					MyApp.socketmap.remove(id);
					MyApp.socketCount--;
					System.out.println("Remaining sockets: ");
					for (Map.Entry<Integer,ListenSocket> entry : MyApp.socketmap.entrySet()) {
						System.out.println("ID:" + entry.getKey() + " connected to ID:" + entry.getValue().getConnId());
					}
					Thread.currentThread().interrupt();
				} catch (IOException e) {
				} 
			}
		}
}

public class MyApp {

	private static final int PORT = 8888;
	public static ServerSocket srvsck;
	public static Map<Integer,ListenSocket> socketmap = new HashMap<Integer,ListenSocket>();
	public static int socketCount = 0;
	public static Integer nextID = 0;
	
	public static void main(String args[]) throws IOException {	
	
			srvsck = new ServerSocket(PORT); // listen on 127.0.0.1
			System.out.println("Server started");
			
			while (true) { // always listen for new connections 
				synchronized(nextID) { // ensure that no socket will modify ID at the same time concurrently
					Socket newsck = null; 
					newsck = srvsck.accept(); //accept connection		
				
					socketmap.put(nextID,new ListenSocket(newsck,nextID)); 
					Thread t1 = new Thread(socketmap.get(nextID)); // delegate new socket to thread
					t1.start(); //t1.start();
					nextID++;
					socketCount++;
				}
			}		
	}
}
