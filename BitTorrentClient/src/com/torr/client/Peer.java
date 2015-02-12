package com.torr.client;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.*;

//import com.torr.utils.TasksQueue;
import com.torr.msgs.PeerMessage;
import com.torr.policies.ProtocolPolicy;
//import com.torr.policies.ProtocolPolicy;


/**
 * @class Peer
 * 
 * - Abstraction for a peer. 
 * - Starts a background thread on creation
 * - Its public methods are thread-safe 
 *
 */
public class Peer /*extends TasksQueue*/ implements Runnable, AutoCloseable  {
	
	private Socket peerSocket = null;
	private TorrentFile torrentFile = null;
	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;
	private SenderThread senderThread = null;
	private ReaderThread readerThread = null;
	private LinkedBlockingQueue<PeerMessage> outQueue
				= new LinkedBlockingQueue<PeerMessage>();
	private LinkedBlockingQueue<PeerMessage.RequestMessage> upPieceRequests 
				= new LinkedBlockingQueue<PeerMessage.RequestMessage>();
	private PeerStateChecker peerStateChecker = new PeerStateChecker();
	
	private BitSet peerBitField = null;
	private String peerId = null;
	private volatile Piece downPiece = null; // Currently downloaded piece
	
	public volatile boolean clientInterested = false;
	public volatile boolean peerInterested = false;
	public volatile boolean clientChocking = true;
	public volatile boolean peerChoking = true;
	
	private volatile boolean shutdownRequested = false;
	private volatile boolean incommingConnection = false;
	private volatile boolean peerInitialized = false; 
	private ITorrentFileHolder torrentFileHolder = null;
	private String hostName = null;
	private int portNumber = 0;
	
	public Peer(TorrentFile torrentFile, String hostName, int portNumber, String peerId) throws Exception
	{
		this.incommingConnection = false;
		
		this.torrentFile = torrentFile;
		this.hostName = hostName;
		this.portNumber = portNumber;
		this.peerId = peerId;
		
		FireBackgroundThreads();
	}
	
	public Peer(ITorrentFileHolder torrentFileHolder, Socket peerSocket) throws Exception
	{
		this.incommingConnection = true;
		this.torrentFileHolder = torrentFileHolder;
		this.peerSocket = peerSocket;
		
		FireBackgroundThreads();	
	}	
	
	@Override
	public void close()
	{
		try
		{
			Log("Closing connection with peer [" + this.GetPeerId() + "]");
			
			if(this.torrentFile != null)
			{
				this.torrentFile.UnregisterPeer(this);
			}
			if(this.downPiece != null)
			{
				this.downPiece.SetDownloadingPeer(null);
			}
			this.senderThread.close();
			this.readerThread.close();
			
			this.shutdownRequested = true;
			this.inputStream.close();
			this.outputStream.close();
			this.peerSocket.close();
		}
		catch(Exception ex)
		{			
		}			
	}
	
	private synchronized Piece GetDownPiece()
	{
		return this.downPiece;
	}
	public void SetDownPiece(Piece newPiece)
	{
		this.downPiece = newPiece;
	}
	
	@Override
	synchronized
	public void run()
	{
		if(!EnsureConnectionInitialized())
		{
			close();
			return;
		}
		
		ApplyArtificialDelay();
		CheckDownloadPieceState();
		
		this.peerStateChecker.CheckForIdleState();
	}
	
	private boolean EnsureConnectionInitialized()
	{
		if(!this.peerInitialized)
		{
			try
			{								
				if(this.incommingConnection) 
				{
					if(!RunIncommingConnectionPrelude())
					{
						return false;
					}
				}
				else if(!RunOutgoingConnectionPrelude())
				{
					return false;
				}
								
				SendBitfield();
				// Friendly peer: always interested and unchoking
				DeclareInterest(true);
				Choke(false);
								
				this.torrentFile.RegisterPeer(this);				
				this.peerInitialized = true;
			}
			catch(Exception ex)
			{
				Log("Failed to initialize peer", ex);
				return false;
			}				
		}
		
		return (this.torrentFile != null);
	}
	private boolean RunIncommingConnectionPrelude() throws Exception
	{
		if(this.torrentFileHolder == null)
		{
			return false;
		}
		
		this.outputStream = new ObjectOutputStream(peerSocket.getOutputStream());
		this.inputStream = new ObjectInputStream(peerSocket.getInputStream());
		
		PeerMessage.HandshakeMessage inMsg = ReadHandshakeDirect();
		System.out.println("Received handshake from remote peer");	
		
		Log("Accepted request from Peer [" + inMsg.getPeerId() + 
				"] for file [" + inMsg.getInfoHash() + "]");				
						
		this.torrentFile = this.torrentFileHolder
				.GetTorrentFileByInfoHash(inMsg.getInfoHash());
		this.torrentFileHolder = null;
		
		if(this.torrentFile == null)
		{
			return false;
		}
		
		this.peerId = inMsg.getPeerId();
		SendHandshake();
		// Friendly peer: always interested and unchoking
		DeclareInterest(true);
		Choke(false);		
		
		return true;
	}
	private boolean RunOutgoingConnectionPrelude() throws Exception
	{
		if(this.torrentFile == null)
		{
			return false;
		}
		
		Log("Requesting connection with Peer [" + this.hostName + 
				" : " + this.portNumber + "]");
		
		this.peerSocket = new Socket(this.hostName, this.portNumber);
		
		this.outputStream = new ObjectOutputStream(peerSocket.getOutputStream());
		SendHandshakeDirect();
		
		this.inputStream = new ObjectInputStream(peerSocket.getInputStream());		
		
		return true;
	}
	
	synchronized
	private void CheckDownloadPieceState()
	{
		if(IsInterested() && !IsChoked())
		{		
			// Go for a new piece if we're available
			if(this.downPiece == null)
			{
				this.downPiece = this.torrentFile.GetNextPieceForPeer(this);
			}
		}
	}
	
	private void ApplyArtificialDelay()
	{
		/*
		try
		{
			Thread.sleep(5);
		}
		catch(InterruptedException ex)
		{}
		*/				
	}
	
	synchronized
	public void NotifyForDownloadedPiece(Piece piece)
	{
		synchronized(this.torrentFile)
		{							
			torrentFile.SendHaveMessageToPeers(piece.getIndex());
		}
	}
	

	public boolean IsAlive()
	{
		return !this.shutdownRequested;
	}
	
	
	public void Choke(final boolean choke)
	{
		Log("Sending [" + (choke ? "Choke" : "Unchoke") +
				"] message to peer [" + this.GetPeerId() + 
				"] for file ["  + this.torrentFile.getInfoHash() + "]");		
		
		if(choke)
		{
			queueOutgoingMessage(new PeerMessage.ChokeMessage());
		}
		else
		{
			queueOutgoingMessage(new PeerMessage.UnchokeMessage());
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
		Log("Sending [" + (interested ? "Interested" : "Not Interested") +
				"] message to peer [" + this.GetPeerId() + 
				"] for file ["  + this.torrentFile.getInfoHash() + "]");
		
		if(interested)
		{
			queueOutgoingMessage(new PeerMessage.InterestedMessage());
		}
		else
		{
			queueOutgoingMessage(new PeerMessage.NotInterestedMessage());
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
	public String GetPeerId()
	{
		return this.peerId;
	}
	public BitSet GetBitField()
	{
		return this.peerBitField;
	}
	
	public void SendKeepAliveMessage()
	{
		if(this.peerStateChecker.CanSendKeepAlive())
		{
			queueOutgoingMessage(new PeerMessage.KeepAliveMessage());
			this.peerStateChecker.LogOutgoingKeepAlive();
		}
	}
	public void SendHaveMessage(final int pieceIndex)
	{
		queueOutgoingMessage(new PeerMessage.HaveMessage(pieceIndex));
	}
	
	public void SendBitfield()
	{
		queueOutgoingMessage(new PeerMessage.BitfieldMessage(this.torrentFile.getBitField()));
		
		Log("Sending bitfield to peer [" + this.GetPeerId() + 
				"] for file ["  + this.torrentFile.getInfoHash() + "]");
	}
	public void SendRequest(final int piece, final int offset, final int length)
	{
		queueOutgoingMessage(new PeerMessage.RequestMessage(piece, offset, length));
	}
	public void CancelRequest(final int piece, final int offset, final int length)
	{
		queueOutgoingMessage(
				new PeerMessage.CancelMessage(piece, offset, length));
	}
	private void SendData(ByteBuffer data, int piece, int offset)
	{
		queueOutgoingMessage(
				new PeerMessage.PieceMessage(piece, offset, data));
	}	
	
	private void SendHandshake() throws Exception
	{
		
		Log("Sending handshake to peer [" + this.peerId + 
				"] for file ["  + this.torrentFile.getInfoHash() + "]");			
		
		queueOutgoingMessage(
				new PeerMessage.HandshakeMessage(Consts.PROTOCOL_IDENTIFIER, 
					this.torrentFile.getInfoHash(),
					this.torrentFile.getPeerId()));
	
		
	}
	private void SendHandshakeDirect() throws Exception
	{
		Log("Sending handshake to peer [" + this.peerId + 
				"] for file ["  + this.torrentFile.getInfoHash() + "]");			
		
		sendPeerMessage(
				new PeerMessage.HandshakeMessage(Consts.PROTOCOL_IDENTIFIER, 
					this.torrentFile.getInfoHash(),
					this.torrentFile.getPeerId()));			
	}	
	
	private PeerMessage.HandshakeMessage ReadHandshakeDirect() throws Exception
	{
		return (PeerMessage.HandshakeMessage)readPeerMessage();			
	}
	
	// Returns true if a message is read
	private boolean tryReadNextMessage() throws Exception
	{		
		Boolean messageRead = false;
		
		handleMessage(readPeerMessage());
		messageRead = true;
		this.peerStateChecker.LogDownload();
		
		return messageRead;
	}
	private boolean trySendNextMessage() throws Exception
	{
		boolean messageSent = false;
		
		if(!outQueue.isEmpty())
		{
			PeerMessage outgoingMsg = outQueue.take();
			sendPeerMessage(outgoingMsg);
			OnMessageSent(outgoingMsg);
			
			messageSent = true;
		}
		
		return messageSent;
	}
	private boolean ServeBlockRequests() throws IOException
	{
		boolean served = false;
		
		if(!this.upPieceRequests.isEmpty())
		{
			PeerMessage.RequestMessage msg = this.upPieceRequests.remove();
			
			Piece requestPiece = this.torrentFile.getPiece(msg.getPiece());
			
			ByteBuffer data = requestPiece.read(msg.getOffset(), msg.getLength());
			this.SendData(data, msg.getPiece(), msg.getOffset());
			
			Log("Sent data of block #" + msg.getOffset() + " of piece #" + msg.getPiece());
		}		
		
		return served;
	}
	
	synchronized
	private boolean RequestNextBlock()
	{
		if(IsInterested() && !IsChoked())
		{		
			final Piece down_piece = this.downPiece;
			if(down_piece == null)
				return false;
			
			PeerMessage.RequestMessage request = down_piece.GetNextBlockRequest();
			if(request == null)
			{
				return false;
			}
			
			this.queueOutgoingMessage(request);
			return true;
		}
		return false;
	}
	
	
	private void queueOutgoingMessage(PeerMessage msg)
	{
		outQueue.add(msg);
	}
	private void sendPeerMessage(PeerMessage msg) throws IOException
	{
		this.outputStream.writeObject(msg);
		this.outputStream.flush();
	}
	private PeerMessage readPeerMessage() throws Exception
	{
		return (PeerMessage)this.inputStream.readObject();
	}
	
	private void handleMessage(PeerMessage msg) throws Exception
	{
		if(msg == null)
			return;
		
		
		Log("Received [" + msg.getType().name() + "] message");
		switch(msg.getType())
		{
		case KEEP_ALIVE:
			HandleKeepAliveMessage();
			break;
		case CHOKE:
			HandlePeerChoking(true);
			break;
		case UNCHOKE:
			HandlePeerChoking(false);
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
		case HANDSHAKE:
			HandleHandshakeMessage((PeerMessage.HandshakeMessage)msg);
			break;
		}
	}
	
	private void HandleKeepAliveMessage()
	{
		// Reply with a keepalive message if we didn't initiate the exchange
		if(!this.peerStateChecker.ExpectingKeepAlive())
		{
			SendKeepAliveMessage();
		}
		this.peerStateChecker.LogIncommingKeepAlive();
	}
	
	private void HandlePeerChoking(final boolean choking)
	{
		this.peerChoking = choking;
	}
	private void HandlePeerInterested(final boolean interested)
	{
		this.peerInterested = interested;		
	}
	private void HandlePeerHave(PeerMessage.HaveMessage msg) throws Exception
	{
		torrentFile.ProcessPeerPieceAvailability(this, msg.getPieceIndex());		
	}
	private void HandlePeerBitfield(PeerMessage.BitfieldMessage msg)
	{
		Log("Received bitfield from remote peer");
		
		this.torrentFile.ProcessPeerBitfieldChange(this.GetBitField(), msg.getBitfield());
		this.peerBitField = msg.getBitfield();
	}
	private void HandlePeerRequest(PeerMessage.RequestMessage msg)
	{
		if(!PeerInterested() || IsChoking())
		{
			Log("Terminating connection with peer [" + this.GetPeerId() + "]" + 
				" for file [" + this.torrentFile.getInfoHash() + 
				"] due to invalid request");
		}
		
		upPieceRequests.add(msg);
	}
	private void HandlePieceMessage(PeerMessage.PieceMessage msg) throws Exception
	{
		GetDownPiece().write(msg.getBlock(), msg.getOffset());
	}
	private void HandleHandshakeMessage(PeerMessage.HandshakeMessage msg)
	{	
		Log("Received handshake from peer [" + msg.getPeerId() + "]"
				+ " for file [" + msg.getInfoHash() + "]");
		
		this.peerId =  msg.getPeerId();
		
		
		if(!msg.getInfoHash().equals(this.torrentFile.getInfoHash()))
		{
			close();
		}
		
		Log("Established connection with Peer [" + msg.getPeerId() + "]" + 
			" for file [" + msg.getInfoHash() + "]");
		
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
			OnChokeSent(false);
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
			break;
		case HANDSHAKE:
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
	
	private class SenderThread implements Runnable, AutoCloseable
	{
		private Thread backgroundThread;
		public SenderThread()
		{
			this.backgroundThread = new Thread(this);
			this.backgroundThread.start();
		}
		
		@Override
		public void run()
		{
			try
			{
				while(!shutdownRequested)
				{	
					// Peer actions
					Peer.this.run();
					
					boolean served_request = ServeBlockRequests();
					boolean pipelined_request = RequestNextBlock();
					boolean sent_message = trySendNextMessage();
					if(!(served_request || sent_message || pipelined_request))
					{
						Thread.yield();
					}
				}
			}
			catch(Exception ex)
			{
				// Only act if we're not shutting down
				if(!Peer.this.shutdownRequested)
				{			
					ex.printStackTrace();
					Log("Error in peer upload [" + GetPeerId() + "]:", ex);
					Peer.this.close();					
				}
			}
		}
		
		@Override
		public void close()
		{
			this.backgroundThread.interrupt();
		}
		
	}
	private class ReaderThread implements Runnable, AutoCloseable
	{
		private Thread backgroundThread;
		public ReaderThread()
		{
			this.backgroundThread = new Thread(this);
			this.backgroundThread.start();
		}
		
		@Override
		public void run()
		{
			try
			{
				while(!shutdownRequested)
				{	
					// Peer actions
					Peer.this.run();									
					
					if(!tryReadNextMessage())
					{
						Thread.yield();
					}
				}
			}
			catch(Exception ex)
			{
				// Only act if we're not shutting down
				if(!Peer.this.shutdownRequested)
				{					
					Log("Error in peer download [" + GetPeerId() + "]:", ex);
					Peer.this.close();
				}
			}
		}
		
		@Override
		public void close()
		{
			this.backgroundThread.interrupt();
		}		
	}	
	
	private class PeerStateChecker
	{
		private volatile long last_download_time = 0;
		private volatile long sent_keep_alive_message_time = 0;
		private volatile boolean expecting_keep_alive = false;
		
		public boolean ExpectingKeepAlive()
		{
			return expecting_keep_alive;
		}
		public void LogOutgoingKeepAlive()
		{
			sent_keep_alive_message_time = System.currentTimeMillis();
		}
		public void LogIncommingKeepAlive()
		{
			this.expecting_keep_alive = false;
		}
		
		public long GetLastDownloadTime()
		{
			return last_download_time;
		}
		
		public void CheckForIdleState()
		{
			if((last_download_time == 0) || expecting_keep_alive)
				return;
			
			long current_time = System.currentTimeMillis();
			long time_diff = current_time - last_download_time;
			if(time_diff < ProtocolPolicy.PEER_IDLE_TIME_FLOOR_THRESHOLD)
			{
				// All good; do nothing
			}
			else if(time_diff < ProtocolPolicy.PEER_IDLE_TIME_CEILING_THRESHOLD)
			{
				expecting_keep_alive = true;
				SendKeepAliveMessage();
			}
			else
			{
				Peer.this.close();
			}
		}
		
		public boolean CanSendKeepAlive()
		{
			long last_keepalive_time_diff = 
					(System.currentTimeMillis() - sent_keep_alive_message_time);
			
			return (last_keepalive_time_diff >= 
						ProtocolPolicy.PEER_IDLE_TIME_FLOOR_THRESHOLD);
		}
		
		public void LogDownload()
		{
			this.last_download_time = System.currentTimeMillis();
		}
	}
	
	private void FireBackgroundThreads()
	{
		this.readerThread = new ReaderThread();
		this.senderThread = new SenderThread();
	}
	
	public void Log(String message)
	{
		try
		{
			if(this.torrentFile != null)
			{
				this.torrentFile.Log(message);
			}
		}
		catch(Exception ex)
		{
		}
	}
	public void Log(String message, Exception ex)
	{
		try
		{
			if(this.torrentFile != null)
			{
				this.torrentFile.Log(message, ex);
			}
		}
		catch(Exception ex2)
		{			
		}
	}	
}
