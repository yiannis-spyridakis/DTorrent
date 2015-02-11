package com.torr.bencode;

import java.nio.file.Paths;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;

public class TorrentFileDescriptor
{
	//private TorrentFileHandler torrent_file_handler;
	private TorrentFile torrent_file;
	private String file_name;
             
	public TorrentFileDescriptor(final String path)
	{
		Initialize(path);
 	}
	
	public String FileName()
	{
		return file_name;
	}
    public String TrackerUrl()
    {
    	return torrent_file.tracker_url;
    }
    public Integer FileLength()
    {
    	return torrent_file.file_length;
    }
    public Integer PieceLength()
    {
    	return torrent_file.piece_length;
    }
    public String InfoHash()
    {        
    	return torrent_file.info_hash_as_hex;
    }
    
    public Integer NumberOfPieces()
    {
	    return torrent_file.piece_hash_values_as_binary.size();
    }
    public Vector<byte[]> PieceHashes()
    {
    	return torrent_file.piece_hash_values_as_binary;
    }
    public boolean IsValid()
    {
    	return this.torrent_file != null;
    }

	private void Initialize(final String path)
	{
		String full_name = Paths.get(path).getFileName().toString();
		file_name = FilenameUtils.removeExtension(full_name);
		
		TorrentFileHandler torrent_file_handler = new TorrentFileHandler();
		torrent_file = torrent_file_handler
				.openTorrentFile(path);	

	}
}
