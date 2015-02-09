package com.torr.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import com.torr.trackermsgs.*;

public class TrackerClient 
	extends Thread 
	implements AutoCloseable 
{
	
	private int portId;
	private TorrentFile torrentFile;
	private volatile boolean shutdownRequested = false;
	
	public MessageToTracker message;
	
	TrackerClient(TorrentFile torrentFile, int portId) 
	{
		this.portId = portId;
		this.torrentFile = torrentFile;
		message = new MessageToTracker(torrentFile.getInfoHash(), torrentFile.getPeerId(), this.portId);
		this.start();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void	run() 
	{
		while(!this.shutdownRequested) 
		{
			try(Socket requestSocket = new Socket(torrentFile.getTrackerIP(), torrentFile.getTrackerPort());
				ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream()))
			{
				out.writeObject(message);
				out.flush();
			
				List<MessageToClient> peers =(List<MessageToClient>) in.readObject();
				
				torrentFile.updatePeersList(peers);								
			}
			catch (UnknownHostException unknownHost) 
			{
				torrentFile.Log("Unable to connect to Tracker: Unknown host");
			} 
			catch (Exception ex) 
			{
				torrentFile.Log("Unable to connect to Tracker", ex);
				ex.printStackTrace();
			}
			
			try
			{
				sleep(1900000);
			}
			catch(InterruptedException ex)
			{
				
			}
		}
	}
	
	@Override
	public void close()
	{
		this.shutdownRequested = true;
	}
	
}