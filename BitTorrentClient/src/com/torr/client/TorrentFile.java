package com.torr.client;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.BitSet;
import java.util.List;
import java.util.Vector;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.torr.bencode.TorrentFileDescriptor;
import com.torr.policies.PieceSelectionPolicy;
import com.torr.trackermsgs.MessageToClient;
import com.torr.utils.SystemUtils;
import com.torr.utils.TasksQueue;

public class TorrentFile extends TasksQueue implements Runnable, AutoCloseable {
	
	
	private TorrentMain torrentMain = null;
	private Piece pieces[] = null;
	private TorrentFileStorage torrentStorage = null;
	private HashMap<String, Peer> peers = new HashMap<String, Peer>();
	private TrackerClient trackerClient = null;
	private TorrentFileDescriptor descriptor = null;
	private File destinationFile = null;
	private Thread backgroundThread = null;
	private PieceSelectionPolicy pieceSelectionPolicy;
	private volatile boolean shutdownRequested = false;
	
	public TorrentFile(
			TorrentMain torrentMain,
			TorrentFileDescriptor descriptor, 
			File destinationDir
		) throws IOException
	{
		this.torrentMain = torrentMain;
		this.descriptor = descriptor;
		this.destinationFile = 
				destinationDir.toPath().resolve(descriptor.FileName()).toFile();		
		
		// Initialize background thread
		this.backgroundThread = new Thread(this);
		this.backgroundThread.start();
	}
	
	@Override
	public void run() {
		
		Log("Validating target file...");
		InitializeTorrentFile(this.destinationFile);
		
		this.pieceSelectionPolicy =  new PieceSelectionPolicy(this);
		
		trackerClient = new TrackerClient(this, this.torrentMain.GetTCPServerPortNumber());
		
		while(!shutdownRequested)
		{
			try
			{
				this.processOutstandingTasks();
				Thread.yield();
			}
			catch(Exception ex)
			{
				Log("An error occured:", ex);
			}
			
		}
		
	}
	@Override
	public void close()
	{
		try
		{
			this.shutdownRequested = true;
			
			if(this.torrentStorage != null)
			{	
				this.torrentStorage.close();	
			}
			
			if(this.peers != null)
			{
				for(Peer peer : this.peers.values())
				{
					peer.close();
				}
				this.peers.clear();
				this.peers = null;
			}
			
			if(this.trackerClient != null)
			{
				this.trackerClient.close();
			}
		} 
		catch(Exception ex)
		{				
		}		
	}
	
	synchronized
	public void RegisterPeer(Peer peer)
	{
		this.peers.put(peer.GetPeerId(), peer);
		UpdateUIForConnectedPeers();
	}
	
	synchronized
	public void UnregisterPeer(Peer peer)
	{
		this.peers.remove(peer.GetPeerId());
		UpdateUIForConnectedPeers();
	}
	
	
	public Piece[] GetPieces()
	{
		return this.pieces;
	}
	
	public String getTrackerIP() {
		return descriptor.TrackerUrl().split(":")[0];
	}
	public int getTrackerPort()
	{
		try
		{
			String port = descriptor.TrackerUrl().split(":")[1];
			return Integer.parseInt(port);
		}
		catch(Exception ex)
		{
			return 0;
		}
	}
	

	public String getInfoHash()
	{
		return descriptor.InfoHash();
	}
	
	
	public String getPeerId()
	{
		return torrentMain.GetPeerId();
	}
	
	public void updatePeersList(List<MessageToClient> messages)
	{
		try
		{
			String localIP = Inet4Address.getLocalHost().getHostAddress() 
					+ ":" + this.torrentMain.GetTCPServerPortNumber();
			
			for(MessageToClient msg : messages)
			{
				try
				{
					String peerIP = msg.getIP().getHostAddress() + ":" + msg.getPort();
					if(!this.torrentMain.GetPeerId().equals(msg.peer_id) && 
					   !peerIP.equals(localIP) && 
					   !this.peers.containsKey(msg.peer_id))
					{
						new Peer(this, msg.getIP().getHostAddress(), 
								 msg.getPort(), msg.peer_id);
					}
				}
				catch(Exception ex)
				{
					Log("Unable to connect to peer [" + msg.peer_id + "]:", ex);
				}
			}
		}
		catch(Exception ex)
		{
			Log("Unable to read peers list:", ex);
		}
	}
	
	public Piece getPiece(int index)
	{
		if (this.pieces == null) {
			throw new IllegalStateException("Torrent not initialized yet.");
		}

		if (index >= this.pieces.length) {
			throw new IllegalArgumentException("Invalid piece index!");
		}
			
		return this.pieces[index];
	}
	
	public BitSet getBitField()
	{
		BitSet bitfield = new BitSet(this.pieces.length);
		for(int i = 0; i < this.pieces.length; ++ i)
		{
			Piece piece = pieces[i];
			if(piece.isValid())
			{
				bitfield.set(i);
			}
		}
		return bitfield;
	}
	
	
	public int getPieceCount()
	{
		if(pieces == null)
			return 0;
		return pieces.length;
	}
	
	public int read(ByteBuffer buffer, long offset) throws IOException
	{
		return this.torrentStorage.read(buffer, offset);
		
	}
	
	private int write(ByteBuffer buffer, long offset) throws IOException
	{
		return this.torrentStorage.write(buffer, offset);
	}
	
	synchronized
	public void NotifyForDownloadedPiece(final Piece piece)
	{		
		this.addTask(new FutureTask<Void>(new Callable<Void>()
		{							
			@Override
			synchronized public Void call()// throws Exception
			{
				try
				{
					// Notify peer for download completion
					Peer downloadingPeer = piece.GetDownloadingPeer();					
					
					// write data to disk
					write(piece.GetBuffer(), piece.getOffset());
					if(piece.validate())
					{
						Log("Successfully downloaded piece #" + piece.getIndex());
						
						UpdateUIForDownloadedPieces();
						
						downloadingPeer.NotifyForDownloadedPiece(piece);
					}										
				}
				catch(Exception ex)
				{
					Log("Failed to write piece #" + piece.getOffset() + " to disk:", ex);
				}
				
				return null;
			}
		}));		
	}
	
	synchronized
	public void ProcessPeerBitfield(final Peer peer, final BitSet bitfield)
	{
		final Piece[] local_pieces = this.pieces;
		
		this.addTask(new FutureTask<Void>(new Callable<Void>()
		{							
			@Override
			public Void call()// throws Exception
			{
				for(int i = 0; i < local_pieces.length; ++i)
				{
					Piece piece = local_pieces[i];
					if(bitfield.get(i) && (!piece.isValid()))
					{
						piece.IncreaseAvailablePeers();
					}
				}
				
				return null;
			}
		}));
	}
	
	synchronized
	public void ProcessPeerPieceAvailability(final Peer peer, final int index) throws Exception
	{		
		if(index > pieces.length - 1)
			throw new Exception("Invalid piece index received from peer [" + peer.GetPeerId() + "]");
	
		final Piece piece = this.pieces[index];
		
		this.addTask(new FutureTask<Void>(new Callable<Void>()
		{							
			@Override
			public Void call()// throws Exception
			{				
				if(!piece.isValid())
				{
					piece.IncreaseAvailablePeers();
				}
				
				return null;
			}
		}));				
	}
	
	synchronized
	public Piece GetNextPieceForPeer(Peer peer)
	{
		return this.pieceSelectionPolicy.GetNextPieceForPeer(peer);
	}
	
	synchronized
	public void SendHaveMessageToPeers(final int pieceIndex)
	{
		for(Peer peer : this.peers.values())
		{
			peer.SendHaveMessage(pieceIndex);
		}
	}
	
	
	private void InitializeTorrentFile(File destinationFile)
	{
		try
		{
			this.torrentStorage = new TorrentFileStorage(destinationFile, descriptor.FileLength());
			InitializePieces();
			InitializeTorrentFileUI();
			
			Log("Successfully initialized torrent");
		}
		catch(Exception ex)
		{
			Log("Unable to initialize torrent", ex);
		}
	}
	
	private void InitializePieces()
	{
		final int fileLength = descriptor.FileLength();
		final int pieceLength = descriptor.PieceLength();
		Vector<byte[]> hashes = descriptor.PieceHashes();
		int currentPieceOffset = 0;
		
		this.pieces = new Piece[descriptor.NumberOfPieces()];
		for(int i = 0; i < descriptor.NumberOfPieces(); ++i)
		{
			int thisPieceLength = Math.min(pieceLength, (fileLength - currentPieceOffset));			
			this.pieces[i] = new Piece(this, i, currentPieceOffset, thisPieceLength, hashes.get(i));
			this.pieces[i].validate();
			
			currentPieceOffset += thisPieceLength;
		}
		
	}
	private int GetValidPiecesCount()
	{
		int validPiecesNumber = 0;
		for(Piece piece : this.pieces)
		{
			if(piece.isValid())
				++validPiecesNumber;
		}
		
		return validPiecesNumber;
	}
	
	public void InitializeTorrentFileUI()
	{
		torrentMain.TorrentUI().SetFileName(this.descriptor.FileName());
		torrentMain.TorrentUI().SetInfoHash(this.descriptor.InfoHash());
		torrentMain.TorrentUI().SetNumberOfPieces(this.descriptor.NumberOfPieces().toString());
		
		String parentFolder = this.destinationFile.getParent();
		if(parentFolder.length() > 100)
		{
			parentFolder = parentFolder.substring(0, 100) + "...";
		}
		torrentMain.TorrentUI().SetSaveLocation(this.destinationFile.getParent());
		UpdateUIForDownloadedPieces();
	}
	
	synchronized
	public void UpdateUIForDownloadedPieces()
	{
		torrentMain.TorrentUI().SetDownloadedPieces(new Integer(GetValidPiecesCount()).toString());
	}
	private void UpdateUIForConnectedPeers()
	{
		torrentMain.TorrentUI().SetPeersNumber(new Integer(this.peers.size()).toString());
	}
	
	
	public void Log(String message)
	{
		if(this.torrentMain != null)
		{
			this.torrentMain.Log(message);
		}
	}
	public void Log(String message, Exception ex)
	{
		if(this.torrentMain != null)
		{
			this.torrentMain.Log(message, ex);
		}
	}
	
}
