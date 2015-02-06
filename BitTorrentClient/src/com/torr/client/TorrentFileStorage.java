package com.torr.client;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;


public class TorrentFileStorage implements AutoCloseable {
	
	private File targetFile;
	
	// Can be used for multiple files implementation
	private final long offset = 0;
	private final long size;
	
	private RandomAccessFile raf;
	private FileChannel channel;
	
	public TorrentFileStorage(File file, long size) throws IOException
	{
		this.targetFile = file;
		this.size = size;
		
		// TODO: check for existing file
		this.targetFile.delete();
		
		this.raf = new RandomAccessFile(this.targetFile, "rw");
		this.raf.setLength(this.size);
		
		this.channel = raf.getChannel();
		
	}
	
	public long getOffset()
	{
		return this.offset;
	}
	public long getSize()
	{
		return this.size;
	}
	
	public int read(ByteBuffer buffer, long offset) throws IOException
	{
		// TODO: check for overrun
		return this.channel.read(buffer, offset);
		
	}
	
	public int write(ByteBuffer buffer, long offset) throws IOException
	{
		return this.channel.write(buffer, offset);
	}
	
	@Override
	public void close() throws IOException
	{
		// Also closes the channel
		this.raf.close();
	}
}
