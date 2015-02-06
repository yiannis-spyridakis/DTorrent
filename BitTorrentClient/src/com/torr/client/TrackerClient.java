package com.torr.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import com.torr.bencode.TorrentFileDescriptor;
import com.torr.msgs.MessageToClient;
import com.torr.msgs.MessageToTracker;

public class TrackerClient extends Thread {
	
	private int portId;
	
	public MessageToTracker message;
	
	TrackerClient(int portId) 
	{
		this.portId = portId;
		message = new MessageToTracker("sha1", "peer1", this.portId);
	}
	
	public void	run() {
		Socket requestSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in =	null;
		
		
		
		try
		{
			while(true) {
				TorrentFileDescriptor torrentFileDescriptor = 
						new TorrentFileDescriptor("project3.torrent");
				requestSocket =	new	Socket(torrentFileDescriptor.TrackerUrl(), 7001);
				System.out.println(requestSocket.getInetAddress().getHostAddress());
				out = new ObjectOutputStream(requestSocket.getOutputStream());
				in = new ObjectInputStream(requestSocket.getInputStream());
				out.writeObject(message);
				out.flush();
			
				List<MessageToClient> peers =(List<MessageToClient>) in.readObject();
				System.out.println(peers);
				
				/*for (int i = 0; i < peers.size(); i++) {
					if(!peers.get(i).peer_id.equals(message.peer_id)) {
					Thread t = new ClientPeer(peers.get(i).IP, peers.get(i).peer_id, peers.get(i).port);
					t.start();
					}
				}*/
				
				sleep(1900000);
			}
		}
		catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException) {
			ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		finally
		{
			try {
				in.close();
				out.close();
				requestSocket.close();
			}
			catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}
	
}