package com.torr.bencode;

import java.util.Vector;

public class TorrentFileDescriptor
{
	private TorrentFileHandler torrent_file_handler;
	private TorrentFile torrent_file;
                 
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
    public String SHA()
    {        
    	return torrent_file.info_hash_as_url;
    }
    public Integer NumberOfPieces()
    {
	    return torrent_file.piece_hash_values_as_hex.size();
    }
    public Vector<byte[]> PieceHashes()
    {
    	return torrent_file.piece_hash_values_as_binary;
    }
        
	public TorrentFileDescriptor(final String path)
	{
		Initialize(path);
 	}

	private void Initialize(final String path)
	{
		torrent_file_handler = new TorrentFileHandler();
		torrent_file = torrent_file_handler
				.openTorrentFile(path);		
	}
}
