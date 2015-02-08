/**
 * Copyright (C) 2011-2012 Turn, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.turn.ttorrent;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.BitSet;

/**
 * BitTorrent peer protocol messages representations.
 *
 * <p>
 * This class and its <em>*Messages</em> subclasses provide POJO
 * representations of the peer protocol messages, along with easy parsing from
 * an input ByteBuffer to quickly get a usable representation of an incoming
 * message.
 * </p>
 *
 * @author mpetazzoni
 * @see <a href="http://wiki.theory.org/BitTorrentSpecification#Peer_wire_protocol_.28TCP.29">BitTorrent peer wire protocol</a>
 */
public abstract class PeerMessage {

	/** The size, in bytes, of the length field in a message (one 32-bit
	 * integer). */
	public static final int MESSAGE_LENGTH_FIELD_SIZE = 4;

	/**
	 * Message type.
	 *
	 * <p>
	 * Note that the keep-alive messages don't actually have an type ID defined
	 * in the protocol as they are of length 0.
	 * </p>
	 */
	public enum Type {
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
		Type(int id) {
			this.id = (byte)id;
		}

		public boolean equals(byte c) {
			return this.id == c;
		}

		public byte getTypeByte() {
			return this.id;
		}

		public static Type get(byte c) {
			for (Type t : Type.values()) {
				if (t.equals(c)) {
					return t;
				}
			}
			return null;
		}
	};

	private final Type type;
	private final ByteBuffer data;

	private PeerMessage(Type type, ByteBuffer data) {
		this.type = type;
		this.data = data;
		this.data.rewind();
	}

	public Type getType() {
		return this.type;
	}

	/**
	 * Returns a {@link ByteBuffer} backed by the same data as this message.
	 *
	 * <p>
	 * This method returns a duplicate of the buffer stored in this {@link
	 * PeerMessage} object to allow for multiple consumers to read from the
	 * same message without conflicting access to the buffer's position, mark
	 * and limit.
	 * </p>
	 */
	public ByteBuffer getData() {
		return this.data.duplicate();
	}

//	/**
//	 * Validate that this message makes sense for the torrent it's related to.
//	 *
//	 * <p>
//	 * This method is meant to be overloaded by distinct message types, where
//	 * it makes sense. Otherwise, it defaults to true.
//	 * </p>
//	 *
//	 * @param torrent The torrent this message is about.
//	 */
//	public PeerMessage validate(TorrentFile torrent)
//		throws MessageValidationException {
//		return this;
//	}

	public String toString() {
		return this.getType().name();
	}

	/**
	 * Parse the given buffer into a peer protocol message.
	 *
	 * <p>
	 * Parses the provided byte array and builds the corresponding PeerMessage
	 * subclass object.
	 * </p>
	 *
	 * @param buffer The byte buffer containing the message data.
	 * @return A PeerMessage subclass instance.
	 * @throws ParseException When the message is invalid, can't be parsed or
	 * does not match the protocol requirements.
	 */
	public static PeerMessage parse(ByteBuffer buffer)
		throws ParseException {
		int length = buffer.getInt();
		if (length == 0) {
			return KeepAliveMessage.parse(buffer);
		} else if (length != buffer.remaining()) {
			throw new ParseException("Message size did not match announced " +
					"size!", 0);
		}

		Type type = Type.get(buffer.get());
		if (type == null) {
			throw new ParseException("Unknown message ID!",
					buffer.position()-1);
		}

		switch (type) {
			case CHOKE:
				return ChokeMessage.parse(buffer.slice());
			case UNCHOKE:
				return UnchokeMessage.parse(buffer.slice());
			case INTERESTED:
				return InterestedMessage.parse(buffer.slice());
			case NOT_INTERESTED:
				return NotInterestedMessage.parse(buffer.slice());
			case HAVE:
				return HaveMessage.parse(buffer.slice());
			case BITFIELD:
				return BitfieldMessage.parse(buffer.slice());
			case REQUEST:
				return RequestMessage.parse(buffer.slice());
			case PIECE:
				return PieceMessage.parse(buffer.slice());
			case CANCEL:
				return CancelMessage.parse(buffer.slice());
			case HANDSHAKE:
				return HandshakeMessage.parse(buffer.slice());
			default:
				throw new IllegalStateException("Message type should have " +
						"been properly defined by now.");
		}
	}

	public static class MessageValidationException extends ParseException {

		static final long serialVersionUID = -1;

		public MessageValidationException(PeerMessage m) {
			super("Message " + m + " is not valid!", 0);
		}

	}


	/**
	 * Keep alive message.
	 *
	 * <len=0000>
	 */
	public static class KeepAliveMessage extends PeerMessage {

		private static final int BASE_SIZE = 0;

		private KeepAliveMessage(ByteBuffer buffer) {
			super(Type.KEEP_ALIVE, buffer);
		}

		public static KeepAliveMessage parse(ByteBuffer buffer) 
				throws MessageValidationException {
			return new KeepAliveMessage(buffer);
		}

		public static KeepAliveMessage craft() {
			ByteBuffer buffer = ByteBuffer.allocateDirect(
				MESSAGE_LENGTH_FIELD_SIZE + KeepAliveMessage.BASE_SIZE);
			buffer.putInt(KeepAliveMessage.BASE_SIZE);
			return new KeepAliveMessage(buffer);
		}
	}

	/**
	 * Choke message.
	 *
	 * <len=0001><id=0>
	 */
	public static class ChokeMessage extends PeerMessage {

		private static final int BASE_SIZE = 1;

		private ChokeMessage(ByteBuffer buffer) {
			super(Type.CHOKE, buffer);
		}

		public static ChokeMessage parse(ByteBuffer buffer) 
				throws MessageValidationException {
			return new ChokeMessage(buffer);
		}

		public static ChokeMessage craft() {
			ByteBuffer buffer = ByteBuffer.allocateDirect(
				MESSAGE_LENGTH_FIELD_SIZE + ChokeMessage.BASE_SIZE);
			buffer.putInt(ChokeMessage.BASE_SIZE);
			buffer.put(PeerMessage.Type.CHOKE.getTypeByte());
			return new ChokeMessage(buffer);
		}
	}

	/**
	 * Unchoke message.
	 *
	 * <len=0001><id=1>
	 */
	public static class UnchokeMessage extends PeerMessage {

		private static final int BASE_SIZE = 1;

		private UnchokeMessage(ByteBuffer buffer) {
			super(Type.UNCHOKE, buffer);
		}

		public static UnchokeMessage parse(ByteBuffer buffer) 
				throws MessageValidationException {
			return new UnchokeMessage(buffer);
		}

		public static UnchokeMessage craft() {
			ByteBuffer buffer = ByteBuffer.allocateDirect(
				MESSAGE_LENGTH_FIELD_SIZE + UnchokeMessage.BASE_SIZE);
			buffer.putInt(UnchokeMessage.BASE_SIZE);
			buffer.put(PeerMessage.Type.UNCHOKE.getTypeByte());
			return new UnchokeMessage(buffer);
		}
	}

	/**
	 * Interested message.
	 *
	 * <len=0001><id=2>
	 */
	public static class InterestedMessage extends PeerMessage {

		private static final int BASE_SIZE = 1;

		private InterestedMessage(ByteBuffer buffer) {
			super(Type.INTERESTED, buffer);
		}

		public static InterestedMessage parse(ByteBuffer buffer) 
				throws MessageValidationException {
			return new InterestedMessage(buffer);
		}

		public static InterestedMessage craft() {
			ByteBuffer buffer = ByteBuffer.allocateDirect(
				MESSAGE_LENGTH_FIELD_SIZE + InterestedMessage.BASE_SIZE);
			buffer.putInt(InterestedMessage.BASE_SIZE);
			buffer.put(PeerMessage.Type.INTERESTED.getTypeByte());
			return new InterestedMessage(buffer);
		}
	}

	/**
	 * Not interested message.
	 *
	 * <len=0001><id=3>
	 */
	public static class NotInterestedMessage extends PeerMessage {

		private static final int BASE_SIZE = 1;

		private NotInterestedMessage(ByteBuffer buffer) {
			super(Type.NOT_INTERESTED, buffer);
		}

		public static NotInterestedMessage parse(ByteBuffer buffer) 
				throws MessageValidationException {
			return new NotInterestedMessage(buffer);
		}

		public static NotInterestedMessage craft() {
			ByteBuffer buffer = ByteBuffer.allocateDirect(
				MESSAGE_LENGTH_FIELD_SIZE + NotInterestedMessage.BASE_SIZE);
			buffer.putInt(NotInterestedMessage.BASE_SIZE);
			buffer.put(PeerMessage.Type.NOT_INTERESTED.getTypeByte());
			return new NotInterestedMessage(buffer);
		}
	}

	/**
	 * Have message.
	 *
	 * <len=0005><id=4><piece index=xxxx>
	 */
	public static class HaveMessage extends PeerMessage {

		private static final int BASE_SIZE = 5;

		private int piece;

		private HaveMessage(ByteBuffer buffer, int piece) {
			super(Type.HAVE, buffer);
			this.piece = piece;
		}

		public int getPieceIndex() {
			return this.piece;
		}

		public static HaveMessage parse(ByteBuffer buffer) 
				throws MessageValidationException {
			return new HaveMessage(buffer, buffer.getInt());
		}

		public static HaveMessage craft(int piece) {
			ByteBuffer buffer = ByteBuffer.allocateDirect(
				MESSAGE_LENGTH_FIELD_SIZE + HaveMessage.BASE_SIZE);
			buffer.putInt(HaveMessage.BASE_SIZE);
			buffer.put(PeerMessage.Type.HAVE.getTypeByte());
			buffer.putInt(piece);
			return new HaveMessage(buffer, piece);
		}

		public String toString() {
			return super.toString() + " #" + this.getPieceIndex();
		}
	}

	/**
	 * Bitfield message.
	 *
	 * <len=0001+X><id=5><bitfield>
	 */
	public static class BitfieldMessage extends PeerMessage {

		private static final int BASE_SIZE = 1;

		private BitSet bitfield;

		private BitfieldMessage(ByteBuffer buffer, BitSet bitfield) {
			super(Type.BITFIELD, buffer);
			this.bitfield = bitfield;
		}

		public BitSet getBitfield() {
			return this.bitfield;
		}

		public static BitfieldMessage parse(ByteBuffer buffer) 
				throws MessageValidationException {
			BitSet bitfield = new BitSet(buffer.remaining()*8);
			for (int i=0; i < buffer.remaining()*8; i++) {
				if ((buffer.get(i/8) & (1 << (7 -(i % 8)))) > 0) {
					bitfield.set(i);
				}
			}

			return new BitfieldMessage(buffer, bitfield);
		}

		public static BitfieldMessage craft(BitSet availablePieces) {
			byte[] bitfield = new byte[
				(int) Math.ceil((double)availablePieces.length()/8)];
			for (int i=availablePieces.nextSetBit(0); i >= 0;
					i=availablePieces.nextSetBit(i+1)) {
				bitfield[i/8] |= 1 << (7 -(i % 8));
			}

			ByteBuffer buffer = ByteBuffer.allocateDirect(
				MESSAGE_LENGTH_FIELD_SIZE + BitfieldMessage.BASE_SIZE + bitfield.length);
			buffer.putInt(BitfieldMessage.BASE_SIZE + bitfield.length);
			buffer.put(PeerMessage.Type.BITFIELD.getTypeByte());
			buffer.put(ByteBuffer.wrap(bitfield));
			return new BitfieldMessage(buffer, availablePieces);
		}

		public String toString() {
			return super.toString() + " " + this.getBitfield().cardinality();
		}
	}

	/**
	 * Request message.
	 *
	 * <len=00013><id=6><piece index><block offset><block length>
	 */
	public static class RequestMessage extends PeerMessage {

		private static final int BASE_SIZE = 13;

		/** Default block size is 2^14 bytes, or 16kB. */
		public static final int DEFAULT_REQUEST_SIZE = 16384;

		/** Max block request size is 2^17 bytes, or 131kB. */
		public static final int MAX_REQUEST_SIZE = 131072;

		private int piece;
		private int offset;
		private int length;

		private RequestMessage(ByteBuffer buffer, int piece,
				int offset, int length) {
			super(Type.REQUEST, buffer);
			this.piece = piece;
			this.offset = offset;
			this.length = length;
		}

		public int getPiece() {
			return this.piece;
		}

		public int getOffset() {
			return this.offset;
		}

		public int getLength() {
			return this.length;
		}

		public static RequestMessage parse(ByteBuffer buffer) 
				throws MessageValidationException {
			int piece = buffer.getInt();
			int offset = buffer.getInt();
			int length = buffer.getInt();
			return new RequestMessage(buffer, piece,
					offset, length);
		}

		public static RequestMessage craft(int piece, int offset, int length) {
			ByteBuffer buffer = ByteBuffer.allocateDirect(
				MESSAGE_LENGTH_FIELD_SIZE + RequestMessage.BASE_SIZE);
			buffer.putInt(RequestMessage.BASE_SIZE);
			buffer.put(PeerMessage.Type.REQUEST.getTypeByte());
			buffer.putInt(piece);
			buffer.putInt(offset);
			buffer.putInt(length);
			return new RequestMessage(buffer, piece, offset, length);
		}

		public String toString() {
			return super.toString() + " #" + this.getPiece() +
				" (" + this.getLength() + "@" + this.getOffset() + ")";
		}
	}

	/**
	 * Piece message.
	 *
	 * <len=0009+X><id=7><piece index><block offset><block data>
	 */
	public static class PieceMessage extends PeerMessage {

		private static final int BASE_SIZE = 9;

		private int piece;
		private int offset;
		private ByteBuffer block;

		private PieceMessage(ByteBuffer buffer, int piece,
				int offset, ByteBuffer block) {
			super(Type.PIECE, buffer);
			this.piece = piece;
			this.offset = offset;
			this.block = block;
		}

		public int getPiece() {
			return this.piece;
		}

		public int getOffset() {
			return this.offset;
		}

		public ByteBuffer getBlock() {
			return this.block;
		}

		public static PieceMessage parse(ByteBuffer buffer) 
				throws MessageValidationException {
			int piece = buffer.getInt();
			int offset = buffer.getInt();
			ByteBuffer block = buffer.slice();
			return new PieceMessage(buffer, piece, offset, block);
		}

		public static PieceMessage craft(int piece, int offset,
				ByteBuffer block) {
			ByteBuffer buffer = ByteBuffer.allocateDirect(
				MESSAGE_LENGTH_FIELD_SIZE + PieceMessage.BASE_SIZE + block.capacity());
			buffer.putInt(PieceMessage.BASE_SIZE + block.capacity());
			buffer.put(PeerMessage.Type.PIECE.getTypeByte());
			buffer.putInt(piece);
			buffer.putInt(offset);
			buffer.put(block);
			return new PieceMessage(buffer, piece, offset, block);
		}

		public String toString() {
			return super.toString() + " #" + this.getPiece() +
				" (" + this.getBlock().capacity() + "@" + this.getOffset() + ")";
		}
	}

	/**
	 * Cancel message.
	 *
	 * <len=00013><id=8><piece index><block offset><block length>
	 */
	public static class CancelMessage extends PeerMessage {

		private static final int BASE_SIZE = 13;

		private int piece;
		private int offset;
		private int length;

		private CancelMessage(ByteBuffer buffer, int piece,
				int offset, int length) {
			super(Type.CANCEL, buffer);
			this.piece = piece;
			this.offset = offset;
			this.length = length;
		}

		public int getPiece() {
			return this.piece;
		}

		public int getOffset() {
			return this.offset;
		}

		public int getLength() {
			return this.length;
		}

		public static CancelMessage parse(ByteBuffer buffer) 
				throws MessageValidationException {
			int piece = buffer.getInt();
			int offset = buffer.getInt();
			int length = buffer.getInt();
			return new CancelMessage(buffer, piece,
					offset, length);
		}

		public static CancelMessage craft(int piece, int offset, int length) {
			ByteBuffer buffer = ByteBuffer.allocateDirect(
				MESSAGE_LENGTH_FIELD_SIZE + CancelMessage.BASE_SIZE);
			buffer.putInt(CancelMessage.BASE_SIZE);
			buffer.put(PeerMessage.Type.CANCEL.getTypeByte());
			buffer.putInt(piece);
			buffer.putInt(offset);
			buffer.putInt(length);
			return new CancelMessage(buffer, piece, offset, length);
		}

		public String toString() {
			return super.toString() + " #" + this.getPiece() +
				" (" + this.getLength() + "@" + this.getOffset() + ")";
		}
	}
	
	
	/**
	 * Handshake message.
	 *
	 * <pstrlen=v><pstr=v><reserved=8><info_hash=20><peer_id=20> 
	 */
	public static class HandshakeMessage extends PeerMessage {

		private static final int BASE_SIZE = 49;
		
		private String protocol_id;
		private String info_hash;
		private String peer_id;

		private HandshakeMessage(ByteBuffer buffer, String protocol_id,
				String info_hash, String peer_id) {
			super(Type.HANDSHAKE, buffer);
			this.protocol_id = protocol_id;
			this.info_hash = info_hash;
			this.peer_id = peer_id;
		}

		public String getProtocolId() {
			return this.protocol_id;
		}

		public String getInfoHash() {
			return this.info_hash;
		}

		public String getPeerId() {
			return this.peer_id;
		}

		public static HandshakeMessage parse(ByteBuffer buffer) 
				throws MessageValidationException {
			
			byte protocol_id_len = buffer.get();
			byte[] protocol_id_bytes = new byte[protocol_id_len];
			buffer.get(protocol_id_bytes);			
			String protocol_id = new String(protocol_id_bytes);
			
			buffer.position(buffer.position() + 8);
			
			byte[] array_buffer = new byte[20];
			
			buffer.get(array_buffer);
			String info_hash = new String(array_buffer);
			
			buffer.get(array_buffer);
			String peer_id = new String(array_buffer);
			
			return new HandshakeMessage(buffer, protocol_id, info_hash, peer_id);
		}

		public static HandshakeMessage craft(String protocol_id, String info_hash, String peer_id) 
			throws Exception {
			ByteBuffer buffer = ByteBuffer.allocateDirect(
					protocol_id.length() + CancelMessage.BASE_SIZE);
			
			ValidateParemeters(protocol_id, info_hash, peer_id);
						
			buffer.put((byte)protocol_id.length());	// pstrlen
			buffer.put(protocol_id.getBytes()); // protocol_id
			buffer.put(new byte[8]); // reserved
			buffer.put(info_hash.getBytes());  // info_hash
			buffer.put(peer_id.getBytes()); // peer_id
			
			return new HandshakeMessage(buffer, protocol_id, info_hash, peer_id);
		}

		private static void ValidateParemeters(String protocol_id, String info_hash, String peer_id)
																throws Exception
		{
			if(info_hash.length() != 20 ||
			   peer_id.length() != 20)
			{
				throw new Exception("Invalid parameters");
			}
		}
		
		public String toString() {
			return super.toString() + " - " + this.getProtocolId() +
				" - " + this.getInfoHash() + " - " + this.getPeerId();
		}
	}	
	
	
}
