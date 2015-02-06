package com.torr.client;

public class PeerRequest {
	private Piece piece;
	private int beginOffset;
	private int length;
	
	public PeerRequest(Piece piece, int beginOffset, int length)
	{
		this.piece = piece;
		this.beginOffset = beginOffset;
		this.length = length;
	}
	public Piece GetPiece()
	{
		return piece;
	}
	public int GetBegin()
	{
		return beginOffset;
	}
	public int GetLength()
	{
		return length;
	}
}
