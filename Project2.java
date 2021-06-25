import java.io.*;
import java.net.*;
import java.util.*;

public class Project2 {

	public static void main(String[] args) throws IOException {
		String choice, address, myAddress, message, filename, buff = " ";
		InetAddress myip = InetAddress.getLocalHost();
		myAddress = myip.getHostAddress();
		Scanner sc = new Scanner(System.in);
		int myPort = sc.nextInt();
		boolean exit = false;
		NodeServer myServer = new NodeServer(myAddress, myPort);
		Thread serv = new Thread(myServer);
		serv.start();
		while(buff != "server -t") {
			buff = sc.next();
		}
		filename = sc.next();
		myServer.interval = sc.nextInt() * 1000;
		Scanner readFile = new Scanner(new File(filename));
		int numOfServers = readFile.nextInt();
		int numOfNeighbors = readFile.nextInt();


		for(int i = 0; i < numOfServers; i++) {
			int id = readFile.nextInt();
			//store id in with address in an array?
			address = readFile.next();
			int port = readFile.nextInt();

			if(address == myAddress) { //skip first bc it's the host?
				continue;
			}
			else {
				//Client cli = new Client(, , , myServer, i, myip, port); //(socket, in, out, server, num, address, port)
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
		
		

	}

}
