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
	
	public PieceSelectionPolicy(TorrentFile torrentFile)//, HashMap<String, Peer> peers)
	{
		this.torrentFile = torrentFile;
	}
	public int GetDownloadedPiecesCount()
	{
		int ret = 0;
		
		for(Piece piece : this.torrentFile.GetPieces())
		{
			if(piece.isValid())
			{
				++ret;
			}
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
			// Implement rarest first policy:
			// Randomly select among the pieces with the minimum peer count
			
			
			// Get the count of peers for each available index
			int[][] peerCounts = new int[peerAvailableIndices.length][2];
			
			for(int i = 0; i < peerAvailableIndices.length; ++i)
			{
				int examined_index = peerAvailableIndices[i];
				peerCounts[i][0] = examined_index;
				
				Piece piece = this.torrentFile.GetPieces()[examined_index];
				
				// Omit pieces that are being downloaded
				if(piece.IsRegisteredWithPeer())
				{
					peerCounts[i][1] = 0;
				}
				else
				{
					peerCounts[i][1] = piece.GetSeedingPeersCount();
				}
			}
			int[] max_indices = GetMaxIndices(peerCounts);
			piece_index = max_indices[new Random().nextInt(max_indices.length)];
		}
		Piece return_piece = this.torrentFile.GetPieces()[piece_index];
		if((return_piece.GetSeedingPeersCount() == 0) && return_piece.IsRegisteredWithPeer())
		{
			return null;
		}
		else
		{
			return_piece.SetDownloadingPeer(peer);
			return return_piece;
		}
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
	
	private int[] GetMaxIndices(int[][] indices)
	{
		int[] ret = null;
		
		// Sort array by available peer count (second column)
		java.util.Arrays.sort(indices, new java.util.Comparator<int[]>()
		{
			public int compare(int[] lhs, int[] rhs)
			{
				return Integer.compare(rhs[1], lhs[1]); // Descending
			}
		});
		
		// Return a 1-d array that contains the indices with the maximum
		// peer count
		
		int length = indices.length;
		if(length == 1)
		{
			ret = new int[1];
			ret[0] = indices[0][0];
			return ret;			
		}
				
		
		int ceiling = 0;
		while((ceiling < length-1) && indices[ceiling][1] == indices[ceiling+1][1])
		{
			++ceiling;
		}
				
		ret = new int[ceiling+1];
		
		int ret_idx = 0;
		for(int i = 0; i <= ceiling; ++i, ++ret_idx)
		{
			ret[ret_idx] = indices[i][0];
		}
		return ret;		
	}		
}