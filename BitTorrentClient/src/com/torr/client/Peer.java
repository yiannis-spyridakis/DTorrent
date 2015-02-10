package com.torr.client;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.*;

//import com.torr.utils.TasksQueue;
import com.torr.msgs.PeerMessage;
//import com.torr.policies.ProtocolPolicy;


/**
 * @class Peer
 * 
 * - Abstraction for a peer. 
 * - Starts a background thread on creation
 * - Its public methods are thread-safe 
 *
 */
public class Peer /*extends TasksQueue*/ implements Runnable  {
	
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
//	private LinkedBlockingQueue<PeerMessage.RequestMessage> downPieceRequests 
//	= new LinkedBlockingQueue<PeerMessage.RequestMessage>();
	
	private BitSet peerBitField = null;
	private String peerId = null;
	
	// Volatiles
	public volatile boolean clientInterested = false;
	public volatile boolean peerInterested = false;
	public volatile boolean clientChocking = true;
	public volatile boolean peerChoking = true;
	private volatile boolean shutdownRequested = false;
	
	private Piece upPiece = null;
	private Piece downPiece = null;
	
	private Peer(Socket peerSocket)
	{
		this.peerSocket = peerSocket;
	}
	public Peer(TorrentFile torrentFile, String hostName, int portNumber) throws Exception
	{
		this(torrentFile, new Socket(hostName, portNumber));
	}
	public Peer(TorrentFile torrentFile, Socket peerSocket) throws Exception
	{
		this(peerSocket);		
		this.torrentFile = torrentFile;
		
		this.outputStream = new ObjectOutputStream(peerSocket.getOutputStream());	
		
		Log("Sending handshake to peer");	
		SendHandshake();	
		Log("Sending bitfield to peer");
		SendBitfield();
		
		FireBackgroundThreads();
	}
	
	public Peer(IPeerRegistrar peerRegistrar, Socket peerSocket) throws Exception
	{
		this(peerSocket);		
		
		// If torrentFile != null => the peer has been added to the torrentFiles peers collection
		InitializeStreams();
		PeerMessage.HandshakeMessage inMsg = ReadHandshake();
		System.out.println("Received handshake from remote peer");
		
		boolean shut_down = false;
		if(inMsg != null)
		{
			this.peerId = inMsg.getPeerId();
			this.torrentFile = peerRegistrar.RegisterPeer(this, inMsg);
			if(this.torrentFile != null)
			{
				Log("Established connection with Peer [" + this.GetPeerId() + 
						"] for file [" + inMsg.getInfoHash() + "]");
			}
			else
			{
				System.out.println("torrentFile == null");
				shut_down = true;
			}
		}
		else
		{
			System.out.println("inMsg == null");
			shut_down = true;
		}
		if(shut_down)
		{
			System.out.println("Shutting down peer");
			shutDown();
			return;
		}
		
		Log("Sending handshake to peer [" + inMsg.getPeerId() + 
				"] for file ["  + inMsg.getInfoHash() + "]");
		SendHandshake();
		Log("Sending bitfield to peer [" + inMsg.getPeerId() + 
				"] for file ["  + inMsg.getInfoHash() + "]");		
		SendBitfield();

		FireBackgroundThreads();		
	}
	
	synchronized
	private boolean InitializeStreams()
	{
		if(this.peerSocket == null)
			return false;
		try
		{
			if(this.outputStream == null)
				this.outputStream = new ObjectOutputStream(peerSocket.getOutputStream());
			if(this.inputStream == null)
				this.inputStream = new ObjectInputStream(peerSocket.getInputStream());
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
			return false;
		}		
		
		return (this.outputStream != null && this.inputStream != null);		
	}
	
	@Override
	synchronized
	public void run()
	{
		if(torrentFile == null)
		{
			Log("Invalid peer state. Aborting...");
			shutDown();
		}
				
		// Go for a new piece if we're available
		if(downPiece == null)
		{
			//Log("Selecting new piece for download");			
			downPiece = this.torrentFile.GetNextPieceForPeer(this);
			if(downPiece == null)
			{
				//Log("No new piece found");
			}
			
		}
		
	}
	
	
	public void shutDown()
	{
		if(this.torrentFile != null)
			Log("Closing connection with peer [" + this.GetPeerId() + "]");
			
		this.shutdownRequested = true;		
	}
	public boolean IsAlive()
	{
		return !this.shutdownRequested;
	}
	
	
	public void Choke(final boolean choke)
	{
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
	
	public void SendHave(final int pieceIndex)
	{
		queueOutgoingMessage(new PeerMessage.HaveMessage(pieceIndex));
	}
	
	public void SendBitfield()
	{
		queueOutgoingMessage(new PeerMessage.BitfieldMessage(this.torrentFile.getBitField()));
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
		this.sendPeerMessage(
				new PeerMessage.HandshakeMessage(Consts.PROTOCOL_IDENTIFIER, 
					this.torrentFile.getInfoHash(),
					this.torrentFile.getPeerId()));
	}
	private PeerMessage.HandshakeMessage ReadHandshake() throws Exception
	{
		return (PeerMessage.HandshakeMessage)readPeerMessage();			
	}
	
	// Returns true if a message is read
	private boolean tryReadNextMessage() throws Exception
	{		
		Boolean messageRead = false;
		
		handleMessage(readPeerMessage());
		messageRead = true;
		
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
	private boolean RequestNextBlock()
	{
		if(this.downPiece == null)
			return false;
				
		PeerMessage.RequestMessage request = this.downPiece.GetNextBlockRequest();
		if(request == null)
		{
			return false;
		}
		
		this.queueOutgoingMessage(request);
		return true;
		
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
			// Nothing to do, we're keeping the connection open anyways.
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
	private void HandlePeerHave(PeerMessage.HaveMessage msg) throws Exception
	{
		torrentFile.ProcessPeerPieceAvailability(this, msg.getPieceIndex());		
	}
	private void HandlePeerBitfield(PeerMessage.BitfieldMessage msg)
	{
		Log("Received bitfield from remote peer");
		this.peerBitField = msg.getBitfield();
	}
	private void HandlePeerRequest(PeerMessage.RequestMessage msg)
	{
		upPieceRequests.add(msg);
	}
	private void HandlePieceMessage(PeerMessage.PieceMessage msg) throws Exception
	{
		this.downPiece.write(msg.getBlock(), msg.getOffset());
	}
	private void HandleHandshakeMessage(PeerMessage.HandshakeMessage msg)
	{	
		Log("Received handshake from peer [" + msg.getPeerId() + "]"
				+ " for file [" + msg.getInfoHash() + "]");
		
		if(!msg.getInfoHash().equals(this.torrentFile.getInfoHash()))
		{
			shutDown();
		}
		this.peerId =  msg.getPeerId();
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
	
	private class SenderThread implements Runnable
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
				if(!InitializeStreams())
					return;
				
				while(!shutdownRequested)
				{	
					// Peer actions
					Peer.this.run();
					
					boolean served_request = ServeBlockRequests();
					boolean sent_message = trySendNextMessage();
					if(!(served_request || sent_message))
					{
						Thread.yield();
					}
				}
			}
			catch(Exception ex)
			{
				Log("Error in peer upload [" + GetPeerId() + "]:", ex);
				shutDown();
			}
		}
	}
	private class ReaderThread implements Runnable
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
				if(!InitializeStreams())
					return;
				
				while(!shutdownRequested)
				{	
					// Peer actions
					Peer.this.run();
					
					boolean pipelined_request = RequestNextBlock();
					boolean sent_message = tryReadNextMessage();
					
					if(!(pipelined_request || sent_message))
					{
						Thread.yield();
					}
				}
			}
			catch(Exception ex)
			{
				Log("Error in peer download [" + GetPeerId() + "]:", ex);
				shutDown();
			}
		}
	}	
	
	private void FireBackgroundThreads()
	{
		this.readerThread = new ReaderThread();
		this.senderThread = new SenderThread();
	}
	
	public void Log(String message)
	{
		this.torrentFile.Log(message);
	}
	public void Log(String message, Exception ex)
	{
		this.torrentFile.Log(message, ex);
	}	
}
