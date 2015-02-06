package com.torr.policies;

/**
 * Policies related to the BitTorrent protocol
 *
 */
public class ProtocolPolicy {
	public static final int BLOCK_SIZE = 2 ^ 14; // 16K
	
	public static int GetBlockSize()
	{
		return BLOCK_SIZE;
	}
	
}
