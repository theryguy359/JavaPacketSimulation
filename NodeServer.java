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
	public final int INFINITY = /*Integer.MAX_VALUE*/9999;
	Vector<Client> clients = new Vector<>();
	int clientCount = 0;
	public int vertices;
	public int interval = 0;
	public int packets = 0;
	public String[] ids;
	public int[][] graph;
	public int[][] routingTable;
	public int[][] connections;
	public int hostID;
	Thread myServer;
	String address;

	private int port;

	public NodeServer(String address, int port) throws IOException {
		this.port = port;
		this.address = address;
		server = new ServerSocket(port);
		myServer = new Thread(this);
		myServer.start();
	}
	public void packetInterval() {
		Thread updates = new Thread(new Runnable() {
			@Override
			public void run() {
				while(running) {
					updateTables();
					try {
						Thread.sleep(interval);
						updateTables();
						sendUpdate(makePacket());
					} catch (IOException | InterruptedException e1) {
					}
				}
			}
		});
		updates.start();
		System.out.println("Server is running");
	}
//Create Routing Table and Update it////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void initializeGraph() {
		for(int x = 0; x < vertices; x++) {
			for(int y = 0; y < vertices; y++) {
				if(x == y) {
					graph[x][y] = 0;
				}
				else {
					graph[x][y] = INFINITY;
				}
			}
		}
	}
	public void initializeGraphs() {
		for(int x = 0; x < vertices; x++) {
			for(int y = 0; y < vertices; y++) {
				if(x == y) {
					routingTable[x][y] = 0;
					connections[x][y] = x;
				}
				else {
					routingTable[x][y] = INFINITY;
					connections[x][y] = INFINITY;
				}
			}
			
		}
	}
	public void updateTables() {
		for(int x = 0; x < vertices; x++) {
			for(int y = 0; y < vertices; y++) {
				if(x == y) {
					routingTable[x][y] = 0;
					connections[x][y] = x;
				}
				else {
					routingTable[x][y] = INFINITY;
					connections[x][y] = INFINITY;
				}
			}
			
		}
		int node = 0;
		for(int i = 0; i < vertices * 4; i++) {
			for(int x = 0; x < vertices; x++) {
				if(this.graph[node][x] != INFINITY) {
					int cost = this.graph[node][x];
					for(int y = 0; y < vertices; y++) {
						int prevCost = this.routingTable[x][y];
						if(connections[x][y] == node) {
							prevCost = INFINITY;
						}
						if(cost + prevCost < this.routingTable[node][y]) {
							this.routingTable[node][y] = cost + prevCost;
							this.connections[node][y] = x;
						}
					}
				}
			}
			node++;
			if(node == vertices) {
				node = 0;
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
		packets++;
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
		updateTables();
		sc.close();
	}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void close() throws IOException {
		for(int i =0; i < vertices; i++) {
			graph[hostID - 1][i] = INFINITY;
			graph[i][hostID - 1] = INFINITY;
		}
		this.updateTables();
		while(!clients.isEmpty()) {
			this.termClient(1);
		}
		running = false;;
		server.close();
	}
	public int getPacketsSent() {
		int current = packets;
		packets = 0;
		return current;
	}
	
	public void addClient(String newAdd, int newPort) throws UnknownHostException, IOException {
		int id = 0;
		boolean found = false;
		boolean isListed = false;
		for(int i = 0; i < clientCount; i++) {
			if (clients.get(i).address.equals(newAdd) && clients.get(i).port == newPort) {
				isListed = true;
			}
		}
		if (isListed) {
		} 
		else {
			for(int i = 0; i < vertices && !found; i++) {
				String info[] = ids[i].split(" ");
				if(info[0].equals(newAdd) && Integer.parseInt(info[1]) == newPort) {
					id = i + 1;
					found = true;
				}
			}
			Socket socket = new Socket(newAdd, newPort);
			DataInputStream newIn = new DataInputStream(socket.getInputStream());
			DataOutputStream newOut = new DataOutputStream(socket.getOutputStream());
			newOut.writeUTF(address+":" + port);
			Client client = new Client(socket, newIn, newOut, this, clientCount, newAdd, newPort, id);
			Thread cli = new Thread(client);
			clients.add(client);
			cli.start();
			clientCount++;
		}
	}
	
	public void run() {
		try {
			while(running) {
				boolean found = true;
				int id = 0;
				Socket socket = server.accept();
				DataInputStream newIn = new DataInputStream(socket.getInputStream());
				DataOutputStream newOut = new DataOutputStream(socket.getOutputStream());
				String newAdd = newIn.readUTF();
				String[] sock = newAdd.split(":");
				for(int i = 0; i < ids.length && !found; i++) {
					String info[] = ids[i].split(" ");
					if(info[0].equals(sock[0]) && Integer.parseInt(sock[1]) == port) {
						id = i + 1;
						found = true;
					}
				}
				Client client = new Client(socket, newIn, newOut, this, clientCount, sock[0], Integer.parseInt(sock[1]), id+1);
				Thread cli = new Thread(client);
				clients.add(client);
				cli.start();
				newOut.writeUTF("gh123");//this tells the new client that the connection was successful, completing the TCP handshake
				clientCount++;
			}

		}
		catch (IOException e) {
		}
	}
	public void printRoutingTable() {
        for (int i = 0; i < vertices; i++) {
        	for (int j = 0; j < vertices; j++) {
        		if(i != j) {
        			System.out.println((i + 1) + " " + (j + 1) + " " + routingTable[i][j]);
        		}
        	}
        }
	}
	public int getPort() {
		return port;
	}
	public void sendUpdate(String packet) throws IOException {
		for(int i = 0; i < clients.size(); i++) {
			clients.get(i).write(packet);
		}
	}
	public void termClient(int id) throws IOException {
		graph[hostID - 1][id - 1] = INFINITY;
		graph[id - 1][hostID - 1] = INFINITY;
		this.sendUpdate(this.makePacket());
		int index = 0;
		for(int i = 0; i < clients.size(); i++) {
			if((clients.get(i).id) == id) {
				index = i;
			}
		}
		if (!clients.isEmpty()) {
			clients.get(index).out.writeUTF("4term78");
		}
		clients.get(index).remove();
		clients.remove(index);
		clientCount--;
		this.updateTables();
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
	public void setIDs() {
		for(int i = 0; i < clients.size(); i++) {
			for(int j = 0; j < vertices; j++) {
				String info[] = ids[j].split(" ");
				if(clients.get(i).port == Integer.parseInt(info[1]) && clients.get(i).address.equals(info[0])) {
					clients.get(i).id = j + 1;
				}
			}
		}
	}
}
