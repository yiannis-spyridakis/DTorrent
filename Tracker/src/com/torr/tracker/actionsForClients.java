package com.torr.tracker;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.torr.trackermsgs.MessageToClient;
import com.torr.trackermsgs.MessageToTracker;

public class actionsForClients extends Thread {
	ObjectInputStream	in;
	ObjectOutputStream	out;
	InetAddress IP;
	
	public	actionsForClients(Socket connection) {
		try
		{
			out	= new ObjectOutputStream(connection.getOutputStream());
			in = new ObjectInputStream(connection.getInputStream());
			IP = connection.getInetAddress()/*.getHostAddress()*/ ;
			
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	public void run() {
		try
		{	
			MessageToTracker message =(MessageToTracker) in.readObject();
			System.out.print(
					"Accepted connection from peer " + message.peer_id + "\n" +
					"IP Address: " + IP.getHostAddress() + ", Port: " + message.port + "\n" + 
					"Requested torrent info hash: " + message.getInfo_hash() + "\n"				
				);
			
			MessageToClient messageC = new MessageToClient(message.getPeer_id(), IP, message.getPort());
			messageC.timer = System.currentTimeMillis();
			boolean found = false;
			for(Map.Entry<String, List<MessageToClient>> entry : Tracker.peers.entrySet()) {
				if(entry.getKey().equals(message.getInfo_hash())) {
					entry.getValue().add(messageC);
					found = true;
				}
			}
			
			if(found == false){
				List<MessageToClient> clients = new ArrayList<MessageToClient>();
				clients.add(messageC);
				Tracker.peers.put(message.getInfo_hash(), clients);
			}

			//Tracker.peers.put(message.getInfo_hash(), message);
			out.writeObject(Tracker.peers.get(message.getInfo_hash()));
			out.flush();
			
			/* // Display elements
			Set set = Tracker.peers.entrySet();
			Iterator i = set.iterator();
			while(i.hasNext()) {
				Map.Entry me = (Map.Entry)i.next();
				System.out.print(me.getKey() + ": ");
				System.out.println(me.getValue());
			}*/
			
		}
		catch(IOException e) {
			e.printStackTrace();
		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		finally
		{
			try
			{
				in.close();
				out.close();
			}
			catch
			(IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}
}