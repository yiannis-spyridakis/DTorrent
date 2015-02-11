package com.torr.policies;

/**
 * Policies related to the BitTorrent protocol
 *
 */
public class ProtocolPolicy {
	public static final int BLOCK_SIZE = 16384; //2 ^ 14 bytes = 16K
	public static final int KEEP_ALIVE_INTERVAL = 2 * 60 * 1000; // (msecs = 2 mins)
	public static final int RANDOM_PIECE_SELECTION_THRESHOLD = 4; // threshold over which rarest first is implemented
	public static final int MAX_INCOMING_CONNECTIONS = 40;
	public static final int MAX_OUTGOING_CONNECTIONS = 40;
	public static final int MAX_OUTSTANDING_REQUESTS = 20;
	public static final int PEER_REFRESH_INTERVAL = 5 * 60 * 1000; // 5 mins  
}
