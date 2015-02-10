package com.torr.policies;

/**
 * Policies related to the BitTorrent protocol
 *
 */
public class ProtocolPolicy {
	public static final int BLOCK_SIZE = 2 ^ 14; // 16K
	public static final int KEEP_ALIVE_INTERVAL = 2 * 60 * 1000; // (msecs = 2 mins)
	public static final int RANDOM_PIECE_SELECTION_THRESHOLD = 4; // threshold over which rarest first is implemented
}
