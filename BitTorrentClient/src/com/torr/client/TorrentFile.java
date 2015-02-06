package com.torr.client;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class TorrentFile {
	
	TorrentFiles torrentFiles = null;
	Piece pieces[] = null;
	TorrentFileStorage torrentStorage = null;
	
	public TorrentFile(
			TorrentFileDescriptor descriptor, 
			File destinationDir, 
			boolean isSeeder
		)
	{
		this.pieces = descriptor.getPieces();
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
	
	
}
