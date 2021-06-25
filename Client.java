import java.io.*;
import java.net.*;

public class Client implements Runnable{
	Socket socket;
	public DataInputStream in = null; 
	public DataOutputStream out	 = null;
	NodeServer myServer;
	boolean running = true;
	public String address;
	public int port;
	public int clientNum;
	Client cli = this;
	int id;
	public Client(Socket socket, DataInputStream in, DataOutputStream out, NodeServer server, int num, String address, int port, int id) {
		this.in = in;
		this.out = out;
		this.address = address;
		this.port = port;
		this.socket = socket;
		this.myServer = server;
		this.clientNum = num;
		this.id = id;
	}
	public void run() {
				while(running) {
					try {
						String serverMessage = in.readUTF();
						String[] buff = serverMessage.split(" ");
						switch (buff[0]) { //terminate, datagram recieved
							case("4term78"): { //terminate
								myServer.disconnect(cli);
								running = false;
								socket.close();
								break;
							}
							case("gh123"): {
								break;
							}
							default: {//datagram received
								System.out.println("RECEIVED A MESSAGE FROM SERVER " + id);
								myServer.readPacket(serverMessage);
								break;
							}
						}
					}
					catch (IOException e){
					}
				}
	}
	public Socket getSocket() {
		return socket;
	}
	public void write(String packet) throws IOException {
		out.writeUTF(packet);
	}
	public void remove() throws IOException {
		running = false;
		socket.close();
		
	}
}
