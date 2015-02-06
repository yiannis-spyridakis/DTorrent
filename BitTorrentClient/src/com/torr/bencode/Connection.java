package com.torr.bencode;

import java.util.Vector;

public class Connection {
  
	public static void main(String[] args)
	{
		TorrentFileDescriptor tfht = new TorrentFileDescriptor("project3.torrent");
	            System.out.println("Tracker URL: " + tfht.TrackerUrl());
	            System.out.println("File Size (Bytes): " + tfht.FileLength());
	            System.out.println("Piece Size (Bytes): "+ tfht.PieceLength());
	            System.out.println("SHA-1 Info Hash: "+ tfht.SHA());
	            System.out.println("Number Of Pieces: "+ tfht.NumberOfPieces());
	            Vector<byte[]> hashes = tfht.PieceHashes();
	            for(byte[] hash : hashes)
	            {
	            	System.out.println(hash);
	            }
	            
	            //System.out.println("Hash Of every Piece: "+ tfht.HashOfeachpiece());
	}  		               
    
}

	
