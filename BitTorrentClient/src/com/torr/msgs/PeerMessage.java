package com.torr.msgs;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.io.Serializable;

public class PeerMessage implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public enum Type 
	{
		KEEP_ALIVE(-1),
		CHOKE(0),
		UNCHOKE(1),
		INTERESTED(2),
		NOT_INTERESTED(3),
		HAVE(4),
		BITFIELD(5),
		REQUEST(6),
		PIECE(7),
		CANCEL(8),
		HANDSHAKE(9);
		
		private byte id;
		Type(int id) 
		{
			this.id = (byte)id;
		}

		public byte getTypeByte() 
		{
			return this.id;
		}			
	}
	
	private final Type type;

	private PeerMessage(Type type) 
	{
		this.type = type;
	}

	public Type getType() 
	{
		return this.type;
	}	
	
	public String toString()
	{
		return this.getType().name();
	}
	
	/**
	 * Keep alive message
	 * 
	 *
	 */
	public static class KeepAliveMessage extends PeerMessage
	{
		public KeepAliveMessage()
		{
			super(Type.KEEP_ALIVE);
		}
	}
	
	/**
	 * Choke message
	 * 
	 *
	 */
	public static class ChokeMessage extends PeerMessage
	{
		public ChokeMessage()
		{
			super(Type.CHOKE);
		}
	}
	
	/**
	 * Unchoke message
	 * 
	 *
	 */	
	public static class UnchokeMessage extends PeerMessage
	{
		public UnchokeMessage()
		{
			super(Type.UNCHOKE);
		}
	}
	
	/**
	 * Interested message
	 * 
	 *
	 */		
	public static class InterestedMessage extends PeerMessage
	{
		public InterestedMessage()
		{
			super(Type.INTERESTED);
		}		
	}
	
	/**
	 * Not interested message
	 * 
	 *
	 */		
	public static class NotInterestedMessage extends PeerMessage
	{
		public NotInterestedMessage()
		{
			super(Type.NOT_INTERESTED);
		}		
	}	
	
	/**
	 * Have message
	 * 
	 *
	 */		
	public static class HaveMessage extends PeerMessage
	{
		private final int piece_index;
		
		public HaveMessage(int piece_index)
		{
			super(Type.HAVE);
			this.piece_index = piece_index; 
		}
		
		public int getPieceIndex()
		{
			return this.piece_index;
		}
		
		public String toString()
		{
			return super.toString() + " #" + getPieceIndex();
		}
	}
	
	/**
	 * Bitfield message
	 * 
	 *
	 */		
	public static class BitfieldMessage extends PeerMessage
	{
		private final BitSet bitfield;
		
		public BitfieldMessage(BitSet bitfield)
		{
			super(Type.BITFIELD);
			this.bitfield = bitfield;
		}
		
		public BitSet getBitfield()
		{
			return this.bitfield;
		}
		
		public String toString() 
		{
			return super.toString() + " " + this.getBitfield().cardinality();
		}		
	}
	
	/**
	 * Request message
	 * 
	 *
	 */		
	public static class RequestMessage extends PeerMessage
	{
		private final int piece;
		private final int offset;
		private final int length;
		
		public RequestMessage(final int piece, final int offset, final int length)
		{
			super(Type.REQUEST);
			this.piece = piece;
			this.offset = offset;
			this.length = length;			
		}
		public int getPiece() 
		{
			return this.piece;
		}

		public int getOffset() 
		{
			return this.offset;
		}

		public int getLength() 
		{
			return this.length;
		}	
		
		public String toString() 
		{
			return super.toString() + " #" + this.getPiece() +
				" (" + this.getLength() + "@" + this.getOffset() + ")";
		}		
	}
	
	
	/**
	 * Piece message
	 * 
	 *
	 */		
	public static class PieceMessage extends PeerMessage
	{
		private final int piece;
		private final int offset;
		private final byte[] block;
		
		public PieceMessage(final int piece, final int offset, ByteBuffer blockBuffer)
		{
			super(Type.PIECE);
			this.piece = piece;
			this.offset = offset;
			
			blockBuffer.rewind();
			this.block = new byte[blockBuffer.remaining()];
			blockBuffer.get(this.block);
		}
		
		public int getPiece() 
		{
			return this.piece;
		}

		public int getOffset() 
		{
			return this.offset;
		}

		public ByteBuffer getBlock() 
		{
			return ByteBuffer.wrap(this.block);
		}	
		
		public String toString() 
		{
			return super.toString() + " #" + this.getPiece() +
				" (" + this.getBlock().capacity() + "@" + this.getOffset() + ")";
		}		
	}
	
	/**
	 * Cacnel message
	 * 
	 *
	 */		
	public static class CancelMessage extends PeerMessage
	{
		private final int piece;
		private final int offset;
		private final int length;
		
		public CancelMessage(final int piece, final int offset, final int length)
		{
			super(Type.CANCEL);
			this.piece = piece;
			this.offset = offset;
			this.length = length;
		}
		
		public int getPiece() 
		{
			return this.piece;
		}

		public int getOffset() 
		{
			return this.offset;
		}

		public int getLength() 
		{
			return this.length;
		}		
	}
	
	public static class HandshakeMessage extends PeerMessage
	{
		private final String protocol_id;
		private final String info_hash;
		private final String peer_id;
		
		public HandshakeMessage(final String protocol_id, final String info_hash, final String peer_id)
		{
			super(Type.HANDSHAKE);
			this.protocol_id = protocol_id;
			this.info_hash = info_hash;
			this.peer_id = peer_id;			
		}
		
		public String getProtocolId() 
		{
			return this.protocol_id;
		}

		public String getInfoHash() 
		{
			return this.info_hash;
		}

		public String getPeerId() 
		{
			return this.peer_id;
		}		
		
		public String toString() 
		{
			return super.toString() + " - " + this.getProtocolId() +
				" - " + this.getInfoHash() + " - " + this.getPeerId();
		}		
	}
	
	
}
