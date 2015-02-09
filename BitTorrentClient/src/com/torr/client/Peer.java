package com.torr.client;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Queue;
import java.util.concurrent.*;
import java.nio.*;
import java.text.ParseException;

import com.torr.policies.ProtocolPolicy;
import com.torr.msgs.PeerMessage;


/**
 * @class Peer
 * 
 * - Abstraction for a peer. 
 * - Starts a background thread on creation
 * - Its public methods are thread-safe 
 *
 */
public class Peer implements Runnable  {
	
//	private byte[] currentBitfield = null;
	private Socket peerSocket = null;
	private TorrentFile torrentFile = null;
	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;
	private Thread backgroundThread = null;
	private LinkedBlockingQueue<PeerMessage> outQueue
				= new LinkedBlockingQueue<PeerMessage>();
	private LinkedBlockingQueue<PeerMessage.RequestMessage> pieceRequests 
				= new LinkedBlockingQueue<PeerMessage.RequestMessage>();
	private String peerId = null;
	private Piece inPiece = null;
	private Piece outPiece = null;
	
	// Volatiles
	public volatile boolean clientInterested = false;
	public volatile boolean peerInterested = false;
	public volatile boolean clientChocking = true;
	public volatile boolean peerChoking = true;
	private volatile boolean shutdownRequested = false;
	
	
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
		
		// Create and start the background thread
		backgroundThread = new Thread(this);
		backgroundThread.start();
			
		SendHandshake();
	}
	
	public Peer(IPeerRegistrar peerRegistrar, Socket peerSocket) throws Exception
	{
		this(peerSocket);		
		
		// If torrentFile != null => the peer has been added to the torrentFiles peers collection
		InitializeStreams();
		PeerMessage.HandshakeMessage inMsg = ReadHandshake();
		
		boolean shut_down = false;
		if(inMsg != null)
		{
			this.peerId = inMsg.getPeerId();
			this.torrentFile = peerRegistrar.RegisterPeer(this, inMsg);
			if(this.torrentFile != null)
			{
				torrentFile.Log("Established connection with Peer [" + this.GetPeerId() + 
						"] for file [" + inMsg.getInfoHash() + "]");
			}
			else
			{
				shut_down = true;
			}
		}
		else
		{
			shut_down = true;
		}
		if(shut_down)
		{
			shutDown();
			return;
		}
		
		// Create and start the background thread
		backgroundThread = new Thread(this);
		backgroundThread.start();	
		
		SendHandshake();
	}
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
	public void run()
	{
		if(!InitializeStreams())
			return;
		
		while(!this.shutdownRequested)
		{		
			Boolean sentMessage = trySendNextMessage();
			Boolean readMessage = tryReadNextMessage();			
			
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
	
	public void SendHave(final int pieceIndex)
	{
		queueOutgoingMessage(new PeerMessage.HaveMessage(pieceIndex));
	}
	
	// TODO: Add argument and replace dummy BitSet
	public void SendBitfield()
	{
		BitSet dummy = new BitSet();
		queueOutgoingMessage(new PeerMessage.BitfieldMessage(dummy));
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
	
	private void SendHandshake() throws Exception
	{
		this.sendPeerMessage(
				new PeerMessage.HandshakeMessage(Consts.PROTOCOL_IDENTIFIER, 
					this.torrentFile.getInfoHash(),
					this.torrentFile.getPeerId()));
	}
	private PeerMessage.HandshakeMessage ReadHandshake()
	{
		return (PeerMessage.HandshakeMessage)readPeerMessage();			
	}
	
	// Returns true if a message is read
	private Boolean tryReadNextMessage()
	{		
		Boolean messageRead = false;
		
		try
		{	
			handleMessage(readPeerMessage());
			messageRead = true;
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
				sendPeerMessage(outgoingMsg);
				OnMessageSent(outgoingMsg);
				
				messageSent = true;
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
	private void sendPeerMessage(PeerMessage msg) throws IOException
	{
		this.outputStream.writeObject(msg);
		this.outputStream.flush();
	}
	private PeerMessage readPeerMessage()
	{
		try
		{
			return (PeerMessage)this.inputStream.readObject();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
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
	private void HandlePeerHave(PeerMessage.HaveMessage msg)
	{		
	}
	private void HandlePeerBitfield(PeerMessage.BitfieldMessage msg)
	{
	}
	private void HandlePeerRequest(PeerMessage.RequestMessage msg)
	{
		pieceRequests.add(msg);
	}
	private void HandlePieceMessage(PeerMessage.PieceMessage msg)
	{
	}
	private void HandleHandshakeMessage(PeerMessage.HandshakeMessage msg)
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
	
	
}
