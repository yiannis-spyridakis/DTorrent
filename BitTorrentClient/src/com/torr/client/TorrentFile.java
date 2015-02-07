package com.torr.client;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Vector;

import com.torr.bencode.TorrentFileDescriptor;
import com.torr.msgs.MessageToClient;

public class TorrentFile implements Runnable, AutoCloseable {
	
	
	TorrentMain torrentMain = null;
	Piece pieces[] = null;
	TorrentFileStorage torrentStorage = null;
	//List<MessageToClient> peers;
	TrackerClient trackerClient = null; //= new TrackerClient(Consts.TRACKER_PORT_NUMBER, this);
	TorrentFileDescriptor descriptor;
	
	public TorrentFile(
			TorrentMain torrentMain,
			TorrentFileDescriptor descriptor, 
			File destinationDir
		) throws IOException
	{
		this.torrentMain = torrentMain;
		this.descriptor = descriptor;
		
		File destinationFile = 
				destinationDir.toPath().resolve(descriptor.FileName()).toFile();
		InitializeTorrentFile(destinationFile);
				
		//this.pieces = descriptor.getPieces();
	}
	
	@Override
	public void run() {
		//creates Peers threads, 
		/*for (int i = 0; i < peers.size(); i++) {
			if(!peers.get(i).peer_id.equals(message.peer_id)) {
				Thread t = new Peer(peers.get(i).IP, peers.get(i).peer_id, peers.get(i).port);
				t.start();
			}
		}*/
	
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
	
	public void setPeers(List<MessageToClient> peers) {
		//this.peers = peers;
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
	
	
	private void InitializeTorrentFile(File destinationFile) throws IOException
	{
		this.torrentStorage = new TorrentFileStorage(destinationFile, descriptor.FileLength());
		InitializePieces();
		InitializeTorrentFileUI();
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
	
}
