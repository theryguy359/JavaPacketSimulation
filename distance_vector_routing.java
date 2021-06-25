import java.io.*;
import java.net.*;
import java.util.*;

public class distance_vector_routing {

	public static void main(String[] args) throws IOException {
		String choice, address, myAddress, message, filename;
		InetAddress myip = InetAddress.getLocalHost();
		myAddress = myip.getHostAddress();
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter your port number:");
		int myPort = sc.nextInt();
		sc.nextLine();
		boolean exit = false;
		NodeServer myServer = new NodeServer(myAddress, myPort);
		myServer.graph = new int[9999][9999];
		myServer.routingTable = new int[9999][9999];
		myServer.connections = new int[9999][9999];
		myServer.initializeGraph();
		myServer.ids = new String[1000];
		Thread serv = new Thread(myServer);
		serv.start();
		System.out.println("Enter the name of the file you want to use:");
		filename = sc.nextLine();
		System.out.println("Enter the time interval you want to use:");
		myServer.interval = sc.nextInt() * 1000;
		Scanner readFile = new Scanner(new File(filename));
		int numOfServers = readFile.nextInt();
		myServer.vertices = numOfServers;
		myServer.initializeGraph();
		myServer.initializeGraphs();
		int numOfNeighbors = readFile.nextInt();

		for(int i = 0; i < numOfServers; i++) {
			int id = readFile.nextInt();
			id--;
			address = readFile.next();
			int port = readFile.nextInt();
			myServer.ids[id] = (address + " " + port);
		}
		for(int i = 0; i < numOfServers; i++) {
			String[] line = myServer.ids[i].split(" ");
			address = line[0];
			int port = Integer.parseInt(line[1]);
			if(address.equals(myAddress) && port == myPort) { //host
				myServer.hostID = i + 1;
			}
			else {
				myServer.addClient(address, port);
			}
		}

		for(int i = 0; i < numOfNeighbors; i++) {
			int id1 = readFile.nextInt();
			id1--;
			int id2 = readFile.nextInt();
			id2--;
			int cost = readFile.nextInt();
			myServer.graph[id1][id2] = cost;
			myServer.graph[id2][id1] = cost;
		}
		myServer.setIDs();
		myServer.updateTables();
		myServer.packetInterval();
		while(!exit) {
			choice = sc.next();
			switch(choice) {
				case "help": {
					System.out.println("server -t <topology-file-name> -i <routing-update-interval> The  topology  file  contains  the  initial  topology  configuration  for  the server, e.g., timberlake_init.txt.routing-update-interval:It specifies the time interval between routing updates in seconds.port and server-id: They are written in the topology file. The server should find its port and server-id in the topology file without changing the entry format or adding any new entries \n"
						     +"update <server-ID1> <server-ID2> <Link Cost>server-ID1, server-ID2:The link for which the cost is being updated.Link Cost:It specifies the new link cost between the source and the destination server.Note that  this  command  will  be  issued  to both server-ID1and server-ID2and  involve  them  to update the cost and no other server \n" +
						     "stepSend routing update to neighbors right away. Note that except this, routing updates only happen periodically \n"+
						     "packetsDisplay the  number  of  distance  vector(packets)this  server  has  received  since  the  last invocation of this information.\n"+
						     "displayDisplay  the current routing  table.  And  the  table  should  be  displayed  in a sorted order  from small  ID  to  big \n"+
						     "disable<server-ID>Disable the link to a given server. Doing this “closes”the connection to a given server with server-ID\n"+
						     "crash“Close”all connections. This is to simulate server crashes.Close all connections on all links\n");
					break;
				}
				case "display": {
					myServer.updateTables();
					myServer.printRoutingTable();
					System.out.println("display SUCCESS");
					break;
				}
				case "disable": {
					//disable client
					int connectID = sc.nextInt();
					if(myServer.graph[connectID - 1][myServer.hostID - 1] != myServer.INFINITY) {
						myServer.termClient(connectID);
						System.out.println("disable SUCCESS");
					}
					else {
						System.out.println("disable did not work because the server is not a neighbor");
					}
					break;
				}
				case "crash": {
					//simulate server crash
					myServer.close();
					exit = true;
					System.out.println("Stopped");
					break;
				}
				case "packets": {
					System.out.println("Packets sent since last request: " + myServer.getPacketsSent());
					System.out.println("packets SUCCESS");
					break;
				}
				case "step": {
					myServer.sendUpdate(myServer.makePacket());
					System.out.println("step SUCCESS");
					break;
				}
				case "update": {
					int id1 = sc.nextInt();
					id1--;
					int id2 = sc.nextInt();
					id2--;
					int cost;
					String buffer = sc.next();
					if(buffer.equals("inf")) {
						cost = myServer.INFINITY;
					} 
					else {
						cost = Integer.parseInt(buffer);
					}
					myServer.graph[id1][id2] = cost;
					myServer.graph[id2][id1] = cost;
					myServer.updateTables();
					System.out.println("update SUCCESS");
					break;
				}
				default: {
					System.out.println("You have entered an invalid command. Enter 'help' to get a list of commands");
				}
			}
		
		}
		sc.close();
		

	}

}
