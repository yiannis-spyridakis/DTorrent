package com.torr.client;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.nio.*;

import com.torr.policies.ProtocolPolicy;
import com.torr.utils.TasksQueue;


/**
 * @class Peer
 * 
 * - Abstraction for a peer. 
 * - Starts a background thread on creation
 * - Its public methods are thread-safe 
 *
 */
public class Peer /*extends TasksQueue*/ implements Runnable  {
	
	private byte[] currentBitfield = null;
	private Socket peerSocket = null;
	private BufferedInputStream inputStream = null;
	private BufferedOutputStream outputStream = null;
	private Thread backgroundThread = null;
	private LinkedBlockingQueue<PeerRequest> peerRequests 
		= new LinkedBlockingQueue<PeerRequest>();
	
	// Volatiles
	public volatile Boolean clientInterested = false;
	public volatile Boolean peerInterested = false;
	public volatile Boolean clientChocking = true;
	public volatile Boolean peerChocking = true;
	private volatile Boolean shutdownRequested = false;
	
	public Peer(String hostName, int portNumber) throws IOException
	{
		this(new Socket(hostName, portNumber));
	}
	public Peer(Socket peerSocket) throws IOException
	{
		
		this.peerSocket = peerSocket;
		this.inputStream = new BufferedInputStream(peerSocket.getInputStream());
		this.outputStream = new BufferedOutputStream(peerSocket.getOutputStream());
		
		// Create and start the background thread
		backgroundThread = new Thread(this);
		backgroundThread.start();
	}
	
	@Override
	public void run()
	{
			
		
		while(!this.shutdownRequested)
		{
			// Run the next task if available
			//processOutstandingTasks();
			
			Boolean readMessage = tryReadNextMessage();
			Boolean sentMessage = trySendNextMessage();
			
			// If no data was read/written during the last loop yield execution
			if(!(readMessage || sentMessage))
			{
				Thread.yield();
			}
		}
	}
	
	public void shutDown()
	{
		this.shutdownRequested = true;		
	}
	
//	protected void processOutstandingTasks()
//	{		
//		try
//		{
//			super.processOutstandingTasks();
//		}
//		catch(Exception ex)
//		{
//			shutDown();
//		}
//	}
	
	// Returns true if a message is read
	private Boolean tryReadNextMessage()
	{
		byte[] messageBytes = new byte[ProtocolPolicy.BLOCK_SIZE];		
		Boolean messageRead = false;
			
		int bytesRead = 0;
		try
		{
			if((bytesRead = inputStream.read(messageBytes)) > 0)
			{
				messageRead = true;
				
				ByteBuffer messageBuffer = ByteBuffer.allocate(bytesRead);
				messageBuffer.put(messageBytes);					
				
				handleMessage(messageBuffer);
			}
		}
		catch(IOException ex)
		{
			shutDown();
		}	
		
		return messageRead;
	}
	private Boolean trySendNextMessage()
	{
		Boolean messageSent = false;
		
		if(!peerRequests.isEmpty())
		{
			try
			{
				PeerRequest peerRequest = peerRequests.take();
				Piece requestPiece = peerRequest.GetPiece();
				ByteBuffer blockBuffer = requestPiece.read();
				sendBlock(blockBuffer);
			}
			catch(Exception ex)
			{
				shutDown();
			}
		}
		
		return messageSent;
	}
	private void sendBlock(ByteBuffer blockBuffer)
	{
		// Use the buffer to construct a message of type piece
	}
	
	
	private void handleMessage(ByteBuffer messageBuffer)
	{
		messageBuffer.rewind();
	}
	
}
