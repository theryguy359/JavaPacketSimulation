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
							case("5tht"): { //add client
								String newAddress = buff[1];
								System.out.println("The new address is: " + newAddress);
								int newPort = Integer.parseInt(buff[2]);
								myServer.addClient(newAddress, newPort);
								break;
							}
							case("4term78"): { //terminate
								myServer.disconnect(cli);
								running = false;
								socket.close();
								break;
							}
							case("gh123"): {
								System.out.println("Connection to "+ address+" " + port +" was successful");
								break;
							}
							default: {//print message received message
								System.out.println("Message recieved from " + address);
								System.out.println("Sender's Port: " + port);
								System.out.println("Message: " + serverMessage.substring(1));
								break;
							}
						}
					}
					catch (IOException e){
						System.out.println(e);
					}
				}
	}
	public Socket getSocket() {
		return socket;
	}
	//add give method that takes a client
	public void write(String message) throws IOException {
		out.writeUTF(message);
		System.out.println("Message sent.");
	}
	public void remove() throws IOException {
		running = false;
		socket.close();
		
	}
}
