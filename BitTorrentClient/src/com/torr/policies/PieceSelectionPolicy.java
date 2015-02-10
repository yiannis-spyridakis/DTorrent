package com.torr.policies;

import java.util.BitSet;
//import java.util.HashMap;
import java.util.Random;

import com.torr.client.Piece;
import com.torr.client.Peer;

/**
 * 
 * Implements the piece selection strategy centrally on a TorrentFile level
 *
 */
public class PieceSelectionPolicy 
{
	private Piece pieces[];
	//private HashMap<String, Peer> peers;
	
	public PieceSelectionPolicy(Piece pieces[])//, HashMap<String, Peer> peers)
	{
		this.pieces = pieces;
		//this.peers = peers;
	}
	public int GetDownloadedPiecesCount()
	{
		int ret = 0;
		
		for(Piece piece : this.pieces)
		{
			if(piece.getState() == Piece.States.DOWNLOADED)
				++ret;
		}
		
		return ret;
	}
	public Piece GetNextPieceForPeer(Peer peer)
	{
		BitSet peerBitfield = peer.GetBitField();
		if(peerBitfield == null)
			return null;
				
		int[] peerAvailableIndices = GetBitfieldSetIndices(peerBitfield);
		if(peerAvailableIndices == null || peerAvailableIndices.length == 0)
			return null;
		
		int piece_index = 0;
		
		int downloadedPiecesCount = GetDownloadedPiecesCount();
		if(downloadedPiecesCount <= ProtocolPolicy.RANDOM_PIECE_SELECTION_THRESHOLD)
		{
			// Randomly select among the available pieces
			piece_index = peerAvailableIndices[new Random().nextInt(peerAvailableIndices.length)];			
		}
		else
		{
			// Implement rarest first policy
			int max_peer_count = 0;
			for(int i = 0; i < peerAvailableIndices.length; ++i)
			{
				int examined_index = peerAvailableIndices[i];
				Piece piece = pieces[examined_index];
				
				int piece_seeding_peers_count = piece.GetSeedingPeersCount();
				if(piece_seeding_peers_count > max_peer_count)
				{
					piece_index = examined_index;
				}
			}
		}
		return pieces[piece_index];
	}
	
	private int[] GetBitfieldSetIndices(BitSet peerBitfield)
	{
		int cardinality = peerBitfield.cardinality();
		if(cardinality == 0)
			return null;
		
		int[] ret = new int[cardinality];
		
		int ret_idx = 0;
		for(int i = 0; i < peerBitfield.length(); ++i)
		{
			if(peerBitfield.get(i))
			{
				ret[ret_idx++] = i;
			}			
		}
		
		return ret;
	}
		
}