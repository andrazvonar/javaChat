import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {

	protected int serverPort = 1234;
	protected List<Socket> clients = new ArrayList<Socket>(); // list of clients
	protected List<String> usernames = new ArrayList<String>();

	public static void main(String[] args) throws Exception {
		new ChatServer();
	}

	public ChatServer() {
		ServerSocket serverSocket = null;

		// create socket
		try {
			serverSocket = new ServerSocket(this.serverPort); // create the ServerSocket
		} catch (Exception e) {
			System.err.println("[system] could not create socket on port " + this.serverPort);
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// start listening for new connections
		System.out.println("[system] listening ...");
		try {
			while (true) {
				Socket newClientSocket = serverSocket.accept(); // wait for a new client connection
				synchronized(this) {
					clients.add(newClientSocket); // add client to the list of clients
				}
				ChatServerConnector conn = new ChatServerConnector(this, newClientSocket); // create a new thread for communication with the new client
				conn.start(); // run the new thread
			}
		} catch (Exception e) {
			System.err.println("[error] Accept failed.");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// close socket
		System.out.println("[system] closing server socket ...");
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	// send a message to all clients connected to the server
	public void sendToAllClients(String message) throws Exception {
		Iterator<Socket> i = clients.iterator();
		while (i.hasNext()) { // iterate through the client list
			Socket socket = i.next(); // get the socket for communicating with this client
			try {
				DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages to the client
				out.writeUTF(message); // send message to the client
			} catch (Exception e) {
				System.err.println("[system] could not send message to a client");
				e.printStackTrace(System.err);
			}
		}
	}

	public void addUsername(String username) {
		synchronized(this) {
			usernames.add(username);
		}
	}

	public void removeUsername(String username) {
		synchronized(this) {
			usernames.remove(username);
		}
	}

	public void printUsers() {
		synchronized(this) {
			Iterator<String> i = usernames.iterator();
			System.out.print("Users["+ usernames.size() +"]: ");
			while (i.hasNext()) {
				System.out.print(i.next() + " ");
			}
		}
	}

	public void removeClient(Socket socket) {
		synchronized(this) {
			clients.remove(socket);
		}
	}
}

class ChatServerConnector extends Thread {
	private ChatServer server;
	private Socket socket;
	private String username;

	public ChatServerConnector(ChatServer server, Socket socket) {
		this.server = server;
		this.socket = socket;
	}

	public void run() {
		System.out.println("[system] connected with " + this.socket.getInetAddress().getHostName() + ":" + this.socket.getPort());

		DataInputStream in;
		try {
			in = new DataInputStream(this.socket.getInputStream()); // create input stream for listening for incoming messages
		} catch (IOException e) {
			System.err.println("[system] could not open input stream!");
			e.printStackTrace(System.err);
			this.server.removeClient(socket);
			return;
		}

		while (true) { // infinite loop in which this thread waits for incoming messages and processes them
			String msg_received;
			try {
				msg_received = in.readUTF(); // read the message from the client
			} catch (Exception e) {
				System.err.println("[system] there was a problem while reading message client on port " + this.socket.getPort());
				e.printStackTrace(System.err);
				this.server.removeClient(this.socket);
				this.server.removeUsername(this.username);
				return;
			}

			if (msg_received.length() == 0) // invalid message
				continue;


			String msg_send = "";
			System.out.println(Character.getNumericValue(msg_received.charAt(0)));
			switch (Character.getNumericValue(msg_received.charAt(0))) {
				case 0:
					String msg = msg_received.substring(2);
					System.out.println("[RKchat] [" + this.socket.getPort() + "] : " + msg); // print the incoming message in the console
					msg_send = "someone said: " + msg.toUpperCase(); // TODO
					break;

				case 1:
					String user = msg_received.substring(2);
					this.server.addUsername(user);
					System.out.println("User " + user + " added");
					this.server.printUsers();
					break;

				default:
					System.out.println("Error!");
			}


			try {
				this.server.sendToAllClients(msg_send); // send message to all clients
			} catch (Exception e) {
				System.err.println("[system] there was a problem while sending the message to all clients");
				e.printStackTrace(System.err);
				continue;
			}
		}
	}
}
