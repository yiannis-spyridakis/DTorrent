package com.torr.client;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
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
		
		
//		Log("Validating target file...");
//		InitializeTorrentFile(this.destinationFile);		
//		trackerClient = new TrackerClient(this, this.torrentMain.GetTCPServerPortNumber());
		
		this.backgroundThread = new Thread(this);
		this.backgroundThread.run();
	}
	
	@Override
	public void run() {
		
		Log("Validating target file...");
		InitializeTorrentFile(this.destinationFile);
		
		this.pieceSelectionPolicy =  new PieceSelectionPolicy(pieces);
		
		trackerClient = new TrackerClient(this, this.torrentMain.GetTCPServerPortNumber());
		
//		while(!shutdownRequested)
//		{
//			try
//			{
//				this.processOutstandingTasks();
//				Thread.yield();
//			}
//			catch(Exception ex)
//			{
//				Log("An error occured:", ex);
//			}
//			
//		}
		
	}
	@Override
	public void close()
	{
		if(torrentStorage != null)
		{
			try
			{
				torrentStorage.close();
			} 
			catch(Exception ex)
			{				
			}
		}
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
						this.peers.put(
								msg.peer_id, 
								new Peer(this, msg.getIP().getHostAddress(), msg.getPort()));
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
	
	private void setPieceState(int index, Piece.States state) {
		this.pieces[index].setState( state );
	}
	
	public BitSet getBitField()
	{
		BitSet bitfield = new BitSet(this.pieces.length);
		for(int i = 0; i < this.pieces.length; ++ i)
		{
			Piece piece = pieces[i];
			if(piece.getState() == Piece.States.DOWNLOADED)
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
	
	public int write(ByteBuffer buffer, long offset) throws IOException
	{
		return this.torrentStorage.write(buffer, offset);
	}
	
	public void NotifyForDownloadedPiece(final Piece piece)
	{
//		this.addTask(new FutureTask<Void>(new Callable<Void>()
//		{							
//			@Override
//			public Void call()// throws Exception
//			{
				try
				{
					// write data to disk
					write(piece.GetBuffer(), piece.getOffset());
					if(piece.validate())
					{
						Log("Successfully downloaded piece #" + piece.getIndex());
						// TODO: update UI!
					}
					
					
				}
				catch(Exception ex)
				{
					Log("Failed to write piece #" + piece.getOffset() + " to disk:", ex);
				}
				
//				return null;
//			}
//		}));		
	}
	
	public void ProcessPeerBitfield(final Peer peer, final BitSet bitfield)
	{
		final Piece[] local_pieces = this.pieces;
		
//		this.addTask(new FutureTask<Void>(new Callable<Void>()
//		{							
//			@Override
//			public Void call()// throws Exception
//			{
				for(int i = 0; i < local_pieces.length; ++i)
				{
					Piece piece = local_pieces[i];
					if(bitfield.get(i) && piece.getState() != Piece.States.DOWNLOADED)
					{
						piece.addPeer(peer);
					}
				}
				
//				return null;
//			}
//		}));
	}
	
	public void ProcessPeerPieceAvailability(final Peer peer, final int index) throws Exception
	{		
		if(index > pieces.length - 1)
			throw new Exception("Invalid piece index received from peer [" + peer.GetPeerId() + "]");
	
		final Piece piece = this.pieces[index];
		
//		this.addTask(new FutureTask<Void>(new Callable<Void>()
//		{							
//			@Override
//			public Void call()// throws Exception
//			{				
				if(piece.getState() != Piece.States.DOWNLOADED)
				{
					piece.addPeer(peer);
				}
				
//				return null;
//			}
//		}));				
	}
	
	//synchronized
	public Piece GetNextPieceForPeer(Peer peer)
	{
		return this.pieceSelectionPolicy.GetNextPieceForPeer(peer);
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
			this.setPieceState(i, this.pieces[i].isValid() 
									? Piece.States.DOWNLOADED
									: Piece.States.UNAVAILABLE);
			
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
		torrentMain.TorrentUI().SetDownloadedPieces(new Integer(GetValidPiecesCount()).toString());
	}
	
	public void Log(String message)
	{
		torrentMain.Log(message);
	}
	public void Log(String message, Exception ex)
	{
		torrentMain.Log(message, ex);
	}
	
}
