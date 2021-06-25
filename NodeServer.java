import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Vector;

public class NodeServer implements Runnable{
	private ServerSocket server = null; 
	private DataInputStream in	 = null; 
	private DataOutputStream out = null;
	private Thread client = null;
	public boolean running = true;
	public final int INFINITY = Integer.MAX_VALUE;
	Vector<Client> clients = new Vector<>();
	int clientCount = 0;
	int vertices;
	public int interval = 0;
	public int packets = 0;
	int[][] graph;
	int[][] routingTable;
	int[][] connections;
	int hostID;
	Thread myServer;
	String address;

	private int port;

	public NodeServer(String address, int port) throws IOException {
		this.port = port;
		this.address = address;
		graph = new int[vertices][vertices];//initial graph before distance routing algorithm
		routingTable = new int[vertices][vertices];//final graph after distance routing algorithm
		connections = new int[vertices][vertices];//graph of where each node was last connected to (if neighbor than default is itself and it doesn't store costs)
		initializeGraphs();
		server = new ServerSocket(port);
		myServer = new Thread(this);
		myServer.start();
		Thread updates = new Thread(new Runnable() {
			@Override
			public void run() {
				while(running) {
					updateTab();
					try {
						Thread.sleep(interval);
					} catch (InterruptedException e) {
					}
				}
			}
		});
		updates.start();
	}
//Create Routing Table and Update it////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void initializeGraphs() {
		for(int x = 0; x < vertices; x++) {
			for(int y = 0; y < vertices; y++) {
				if(x == y) {
					graph[x][y] = 0;
					routingTable[x][y] = 0;
					connections[x][y] = x;
				}
				else {
					graph[x][y] = INFINITY;
					routingTable[x][y] = INFINITY;
					connections[x][y] = vertices * vertices;
				}
			}
			
		}
		updateTab();
	}
	public void updateTab() {
		for(int i = 0; i < vertices; i++) {
			for(int x = 0; x < vertices; x++) {
				if(graph[i][x] != INFINITY) {
					int cost = graph[i][x];
					for(int y = 0; y < vertices; y++) {
						int midCost = routingTable[x][y];
						if(connections[x][y] == i) {
							midCost = INFINITY;
						}
						if(cost + midCost < routingTable[i][x]) {
							routingTable[i][y] = cost + midCost;
							connections[i][y] = x;
						}
					}
				}
			}
		}
	}
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String makePacket() {
		String newPacket = vertices + "\n";
		for(int i = 1; i <= vertices; i++) {
			newPacket = newPacket + hostID + " " + i + " " + graph[hostID - 1][i - 1] + "\n";
		}
		return newPacket;
	}
	public void  readPacket(String packet) {
		Scanner sc = new Scanner(packet);
		int fields = sc.nextInt();
		for(int i = 0; i < fields; i++) {
			int id1 = sc.nextInt();
			id1--;
			int id2 = sc.nextInt();
			id2--;
			int cost = sc.nextInt();
			graph[id1][id2] = cost;
			graph[id2][id1] = cost;
		}
		sc.close();
	}
	public void close() throws IOException {
		while(!clients.isEmpty()) {
			this.termClient(1);
		}
		running = false;;
		server.close();
	}
	
	public void addClient(String newAdd, int newPort) throws UnknownHostException, IOException {
		int id = 0;
		boolean isListed = false;
		for(int i = 0; i < clientCount; i++) {
			if (clients.get(i).address == newAdd && clients.get(i).port == newPort) {
				isListed = true;
			}
		}
		if (isListed) {
			System.out.println("This client has already been added.");
		} 
		else {
			Socket socket = new Socket(newAdd, newPort);
			DataInputStream newIn = new DataInputStream(socket.getInputStream());
			DataOutputStream newOut = new DataOutputStream(socket.getOutputStream());
			newOut.writeUTF(address+":" + port);
			Client client = new Client(socket, newIn, newOut, this, clientCount, newAdd, newPort, id);
		//this will send all of the clients to the other server
			Thread cli = new Thread(client);
			clients.add(client);
			cli.start();
			clientCount++;
		}
	}
	
	public void run() {
		try {
			while(running) {
				int id = 0;
				Socket socket = server.accept();
				DataInputStream newIn = new DataInputStream(socket.getInputStream());
				DataOutputStream newOut = new DataOutputStream(socket.getOutputStream());
				String newAdd = newIn.readUTF();
				String[] sock = newAdd.split(":");
				//System.out.println("IP: "+sock[0] + " Port: " + Integer.parseInt(sock[1]) + " has joined.");
				Client client = new Client(socket, newIn, newOut, this, clientCount, sock[0], Integer.parseInt(sock[1]), id);
				Thread cli = new Thread(client);
				clients.add(client);
				cli.start();
				newOut.writeUTF("gh123");//this tells the new client that the connection was successful, completing the TCP handshake
				clientCount++;
			}

		}
		catch (IOException e) {
			System.out.println(e);
		}
	}
	/*
	public void makeTopologyMap() throws FileNotFoundException {
		String ip_1, ip_2;
		int port_1, port_2, cost;
		File map = new File("Something.txt");
		Scanner sc = new Scanner(map);
		while(sc.hasNextLine()) {
			ip_1 = sc.next();
			port_1 = sc.nextInt();
			ip_2 = sc.next();
			port_2 = sc.nextInt();
			cost = sc.nextInt();
		}
	}
	*/
	public void clientListToString() {
		System.out.println("IP Address\t        Port");
		for(int i = 0; i < clients.size(); i++) {
			System.out.println((i+1) +". " +  clients.get(i).address + "\t" + clients.get(i).port);
		}
		
	}
	public int getPort() {
		return port;
	}
	public void termClient(int id) throws IOException {
		if (!clients.isEmpty()) {
			clients.get(id-1).out.writeUTF("4term78");
		}
		clients.get(id-1).remove();
		clients.remove(id - 1);
		clientCount--;
		System.out.println("User " + id + " has disconnected.");
	}
	public void disconnect(Client cli) {
		int id = clients.indexOf(cli);
		try {
			clients.get(id).remove();
		} catch (IOException e) {
			e.printStackTrace();
		}
		clients.remove(id);
		clientCount--;
		System.out.println("User " + (id + 1) + " has disconnected.");
	}
}
