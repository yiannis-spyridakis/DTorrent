package com.torr.bencode;

   public class Connection {

        
public static void main(String[] args)
	{
		TorrentFileHandlerTester tfht = new TorrentFileHandlerTester("project3.torrent");
                System.out.println("Tracker URL: " + tfht.TrackerUrl());
                System.out.println("File Size (Bytes): " + tfht.FileLength());
                System.out.println("Piece Size (Bytes): "+ tfht.PieceLength());
                System.out.println("SHA-1 Info Hash: "+ tfht.SHA());
                System.out.println("Number Of Pieces: "+ tfht.SizeNumberOfallPieces());
                System.out.println("Hash Of every Piece: "+ tfht.HashOfeachpiece());
	}  		               
    
   }

	
