package com.torr.client;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Vector;
import java.util.HashMap;

import com.torr.bencode.TorrentFileDescriptor;
import com.torr.msgs.MessageToClient;

public class TorrentFile implements Runnable, AutoCloseable {
	
	
	private TorrentMain torrentMain = null;
	private Piece pieces[] = null;
	private TorrentFileStorage torrentStorage = null;
	private HashMap<String, Peer> peers;
	private TrackerClient trackerClient = null; //= new TrackerClient(Consts.TRACKER_PORT_NUMBER, this);
	private TorrentFileDescriptor descriptor = null;
	private File destinationFile = null;
	private Thread backgroundThread = null;
	
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
		
		this.backgroundThread = new Thread(this);
		this.backgroundThread.run();
	}
	
	@Override
	public void run() {		
		Log("Validating target file...");
		InitializeTorrentFile(this.destinationFile);
		
		trackerClient = new TrackerClient(this, this.torrentMain.GetTCPServerPortNumber());
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
	
	public String getTrackerUrl() {
		return descriptor.TrackerUrl();
	}
	
	public void updatePeersList(List<MessageToClient> messages)
	{
		try
		{
			for(MessageToClient msg : messages)
			{
				if(!this.peers.containsKey(msg.peer_id))
				{
					this.peers.put(
							msg.peer_id, 
							new Peer(this, msg.getIP().toString(), msg.getPort()));
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
	
	
	private void InitializeTorrentFile(File destinationFile)
	{
		try
		{
			this.torrentStorage = new TorrentFileStorage(destinationFile, descriptor.FileLength());
			InitializePieces();
			InitializeTorrentFileUI();
		}
		catch(Exception ex)
		{
			Log("Unable to initialize torrent file", ex);
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
