package com.torr.policies;

import java.util.BitSet;
//import java.util.HashMap;
import java.util.Random;

import com.torr.client.Piece;
import com.torr.client.Peer;
import com.torr.client.TorrentFile;

/**
 * 
 * Implements the piece selection strategy centrally on a TorrentFile level
 *
 */
public class PieceSelectionPolicy 
{
	private TorrentFile torrentFile = null;
	//private HashMap<String, Peer> peers;
	
	public PieceSelectionPolicy(TorrentFile torrentFile)//, HashMap<String, Peer> peers)
	{
		this.torrentFile = torrentFile;
		//this.peers = peers;
	}
	public int GetDownloadedPiecesCount()
	{
		int ret = 0;
		
		for(Piece piece : this.torrentFile.GetPieces())
		{
			if(piece.getState() == Piece.States.DOWNLOADED)
				++ret;
		}
		
		return ret;
	}
	
	public Piece GetNextPieceForPeer(Peer peer)
	{
		BitSet localBitfield = this.torrentFile.getBitField();		
		BitSet peerBitfield = peer.GetBitField();
		if(localBitfield == null || peerBitfield == null)
			return null;
		
		peerBitfield.andNot(localBitfield);
						
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
			boolean found_piece = false;
			int max_peer_count = 0;
			for(int i = 0; i < peerAvailableIndices.length; ++i)
			{
				int examined_index = peerAvailableIndices[i];
				Piece piece = this.torrentFile.GetPieces()[examined_index];
				
				int piece_seeding_peers_count = piece.GetSeedingPeersCount();
				if(piece_seeding_peers_count > max_peer_count)
				{
					piece_index = examined_index;
					found_piece = true;
				}
			}
			if(!found_piece)
			{
				return null;
			}
		}
		return this.torrentFile.GetPieces()[piece_index];
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