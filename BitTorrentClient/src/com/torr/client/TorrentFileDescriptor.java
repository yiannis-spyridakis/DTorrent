package com.torr.client;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.torr.policies.*;
import com.torr.utils.HashingUtils;

public class TorrentFileDescriptor {
	// REMINDER: Temporary hardcoding
	private final int PIECE_LENGTH = ProtocolPolicy.GetBlockSize() * 16;
	
	
	private final String infoHash;
	private int numberOfPieces = 0;
	private Piece[] pieces = null;
	private int length = 0;
	
	public TorrentFileDescriptor(String torrentDescriptorFilePath) throws Exception
	{
		this.infoHash = readInfoHash(torrentDescriptorFilePath);
		readPieces(torrentDescriptorFilePath);
	}
	public String getInfoHash()
	{
		return this.infoHash;
	}
	public int getLength()
	{
		return this.length;
	}	
	public int getNumberOfPieces()
	{
		return this.numberOfPieces;
	}
	public Piece[] getPieces()
	{
		return this.pieces;
	}
	
	private String readInfoHash(String torrentDescriptorFilePath) throws Exception
	{
		File torrFile = new File(torrentDescriptorFilePath, "r");
		if(!torrFile.exists())
			throw new Exception("Invalid torrent file path");		
		
		return "";
	}
	private void readPieces(String torrentDescriptorFilePath) throws Exception
	{
		File targetFile = new File(targetFilePath(torrentDescriptorFilePath));
		if(!targetFile.exists())
			throw new Exception("Invalid destination file path");
		
		this.length = (int)targetFile.length();
		numberOfPieces = (int)(this.length / PIECE_LENGTH + 1);
		pieces = new Piece[numberOfPieces];
			
		
		try(RandomAccessFile raf = new RandomAccessFile(targetFile, "r"))
		{
			FileChannel inChannel = raf.getChannel();
			
			ByteBuffer byteBuffer = ByteBuffer.allocate(PIECE_LENGTH);
			
			int pieceIndex = 0;
			int pieceOffset = 0;
			
			int numBytesRead = 0;
			while((numBytesRead = inChannel.read(byteBuffer)) > 0)
			{
				pieces[pieceIndex] = new Piece(
						pieceIndex, 
						pieceIndex, 
						numBytesRead, 
						HashingUtils.SHA1(byteBuffer));
				
				pieceOffset += numBytesRead;
				pieceIndex++;
			}
		}
		
	}
	
	private String targetFilePath(String torrentDescriptorFilePath)
	{
		return torrentDescriptorFilePath.substring(
			0, 
			torrentDescriptorFilePath.lastIndexOf(".")
		);
		
	}
}
