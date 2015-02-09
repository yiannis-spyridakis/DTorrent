package com.torr.policies;

/**
 * Policies related to the BitTorrent protocol
 *
 */
public class ProtocolPolicy {
	public static final int BLOCK_SIZE = 2 ^ 14; // 16K
	public static final int KEEP_ALIVE_INTERVAL = 2 * 60 * 1000; // (msecs = 2 mins)
	
//	public static int GetBlockSize()
//	{
//		return BLOCK_SIZE;
//	}
	
}
