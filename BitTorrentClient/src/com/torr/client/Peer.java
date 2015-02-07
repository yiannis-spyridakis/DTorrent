package com.torr.client;

import java.io.*;
import java.net.*;
import java.util.BitSet;
import java.util.concurrent.*;
import java.nio.*;
import java.text.ParseException;

import com.turn.ttorrent.PeerMessage;
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
	
//	private byte[] currentBitfield = null;
	private Socket peerSocket = null;
	private TorrentFile torrentFile = null;
	private BufferedInputStream inputStream = null;
	private BufferedOutputStream outputStream = null;
	private Thread backgroundThread = null;
	private LinkedBlockingQueue<PeerMessage> outQueue
		= new LinkedBlockingQueue<PeerMessage>();
	
	// Volatiles
	public volatile Boolean clientInterested = false;
	public volatile Boolean peerInterested = false;
	public volatile Boolean clientChocking = true;
	public volatile Boolean peerChoking = true;
	private volatile Boolean shutdownRequested = false;
	
	public Peer(TorrentFile torrentFile, String hostName, int portNumber) throws IOException
	{
		this(torrentFile, new Socket(hostName, portNumber));
	}
	public Peer(TorrentFile torrentFile, Socket peerSocket) throws IOException
	{
		
		this.peerSocket = peerSocket;
		this.torrentFile = torrentFile;
		this.inputStream = new BufferedInputStream(this.peerSocket.getInputStream());
		this.outputStream = new BufferedOutputStream(this.peerSocket.getOutputStream());
		
		// Create and start the background thread
		backgroundThread = new Thread(this);
		backgroundThread.start();
	}

	
	@Override
	public void run()
	{
		while(!this.shutdownRequested)
		{		
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
	
	public void Choke(final boolean choke)
	{
		if(choke)
		{
			queueOutgoingMessage(PeerMessage.ChokeMessage.craft());
		}
		else
		{
			queueOutgoingMessage(PeerMessage.UnchokeMessage.craft());
		}
	}
	public boolean IsChoked()
	{
		return this.peerChoking;
	}
	public boolean IsChoking()
	{
		return this.clientChocking;
	}
	
	public void DeclareInterest(final boolean interested)
	{
		if(interested)
		{
			queueOutgoingMessage(PeerMessage.InterestedMessage.craft());
		}
		else
		{
			queueOutgoingMessage(PeerMessage.NotInterestedMessage.craft());
		}		
	}
	public boolean IsInterested()
	{
		return this.clientInterested;		
	}
	public boolean PeerInterested()
	{
		return this.peerInterested;
	}
	
	public void SendHave(final int pieceIndex)
	{
		queueOutgoingMessage(PeerMessage.HaveMessage.craft(pieceIndex));
	}
	
	// TODO: Add argument and replace dummy BitSet
	public void SendBitfield()
	{
		BitSet dummy = new BitSet();
		queueOutgoingMessage(PeerMessage.BitfieldMessage.craft(dummy));
	}
	public void SendRequest(final int piece, final int offset, final int length)
	{
		queueOutgoingMessage(PeerMessage.RequestMessage.craft(piece, offset, length));
	}
	public void CancelRequest(final int piece, final int offset, final int length)
	{
		PeerMessage.CancelMessage.craft(piece, offset, length);
	}
	
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
		catch(Exception ex)
		{
			shutDown();
		}	
		
		return messageRead;
	}
	private Boolean trySendNextMessage()
	{
		Boolean messageSent = false;
		
		if(!outQueue.isEmpty())
		{
			try
			{
				PeerMessage outgoingMsg = outQueue.take();
				sendBlock(outgoingMsg.getData());
				OnMessageSent(outgoingMsg);
			}
			catch(Exception ex)
			{
				shutDown();
			}
		}
		
		return messageSent;
	}
	private void queueOutgoingMessage(PeerMessage msg)
	{
		outQueue.add(msg);
	}
	private void sendBlock(ByteBuffer blockBuffer) throws IOException
	{
		blockBuffer.rewind();
		byte[] dataBytes = new byte[blockBuffer.remaining()];
		blockBuffer.get(dataBytes);
		
		this.outputStream.write(dataBytes);
	}
	
	
	private void handleMessage(ByteBuffer messageBuffer) throws ParseException
	{
		messageBuffer.rewind();
		
		PeerMessage msg = PeerMessage.parse(messageBuffer);
		handleMessage(msg);		
	}
	
	private void handleMessage(PeerMessage msg)
	{
		switch(msg.getType())
		{
		case KEEP_ALIVE:
			// Nothing to do, we're keeping the connection open anyways.
			break;
		case CHOKE:
			HandlePeerChoking(true);
			break;
		case UNCHOKE:
			HandlePeerChoking(true);;
			break;
		case INTERESTED:
			HandlePeerInterested(true);
			break;
		case NOT_INTERESTED:
			HandlePeerInterested(false);
			break;
		case HAVE:
			HandlePeerHave((PeerMessage.HaveMessage)msg);
			break;
		case BITFIELD:
			HandlePeerBitfield((PeerMessage.BitfieldMessage)msg);
			break;
		case REQUEST:
			HandlePeerRequest((PeerMessage.RequestMessage)msg);
			break;
		case PIECE:
			HandlePieceMessage((PeerMessage.PieceMessage)msg);
			break;
		case CANCEL:
			// No need to support
			break;		
		}
	}
	
	private void HandlePeerChoking(final boolean choking)
	{
		this.peerChoking = choking;
		// TODO: Notify TorrentFile
	}
	private void HandlePeerInterested(final boolean interested)
	{
		this.peerInterested = interested;
		// TODO: Notify TorrentFile
	}
	private void HandlePeerHave(PeerMessage.HaveMessage msg)
	{
		
	}
	private void HandlePeerBitfield(PeerMessage.BitfieldMessage msg)
	{
		
	}
	private void HandlePeerRequest(PeerMessage.RequestMessage msg)
	{
		
	}
	private void HandlePieceMessage(PeerMessage.PieceMessage msg)
	{
	}
	
	
	private void OnMessageSent(PeerMessage msg)
	{
		switch(msg.getType())
		{
		case KEEP_ALIVE:
			// Nothing to do, we're keeping the connection open anyways.
			break;
		case CHOKE:
			OnChokeSent(true);
			break;
		case UNCHOKE:
			OnChokeSent(true);;
			break;
		case INTERESTED:
			OnInterestedSent(true);
			break;
		case NOT_INTERESTED:
			OnInterestedSent(false);
			break;
		case HAVE:
			break;
		case BITFIELD:
			break;
		case REQUEST:
			OnRequestSent((PeerMessage.RequestMessage)msg);
			break;
		case PIECE:
			OnPieceSent((PeerMessage.PieceMessage)msg);
			break;
		case CANCEL:
			// No need to support
			break;		
		}		
	}	
	
	private void OnChokeSent(final boolean choking)
	{
		this.clientChocking = choking;
	}
	private void OnInterestedSent(final boolean interested)
	{
		this.clientInterested = interested;
	}
	private void OnRequestSent(PeerMessage.RequestMessage msg)
	{
	}
	private void OnPieceSent(PeerMessage.PieceMessage msg)
	{		
	}
	
	
}
