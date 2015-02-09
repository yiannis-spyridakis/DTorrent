package com.torr.msgs;

//import java.io.Serializable;
import java.nio.ByteBuffer;
import com.torr.utils.HashingUtils;

//import com.turn.ttorrent.PeerMessage;
//import com.turn.ttorrent.PeerMessage.HandshakeMessage;
//import com.turn.ttorrent.PeerMessage.MessageValidationException;
//import com.turn.ttorrent.PeerMessage.Type;
//
//public class Handshake implements Serializable {
//	
//	private static final long serialVersionUID = 1L;
//	public String info_hash;
//	public String peer_id;
//	
//	public Handshake(String info_hash, String peer_id){
//		this.info_hash = info_hash;
//		this.peer_id = peer_id;
//	}
//}


/**
 * Handshake message.
 *
 * <pstrlen=v><pstr=v><reserved=8><info_hash=20><peer_id=20> 
 */
public class HandshakeMessage {

	private static final int BASE_SIZE = 49;
	
	private String protocol_id;
	private String info_hash;
	private String peer_id;
	private ByteBuffer data;
	

	private HandshakeMessage(ByteBuffer buffer, String protocol_id,
			String info_hash, String peer_id) {
		
		this.data = buffer;
		buffer.rewind();
		
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

	public ByteBuffer getData() {
		return this.data.duplicate();
	}
	
	public static HandshakeMessage parse(ByteBuffer buffer) 
	{
		
		byte protocol_id_len = buffer.get();
		byte[] protocol_id_bytes = new byte[protocol_id_len];
		buffer.get(protocol_id_bytes);			
		String protocol_id = new String(protocol_id_bytes);
		
		buffer.position(buffer.position() + 8);
		
		
		byte[] array_buffer = new byte[20];
		buffer.get(array_buffer);
		String info_hash = new String(array_buffer);
		
		buffer.get(array_buffer);
		String peer_id = HashingUtils.bytesToHex(array_buffer);
		
		return new HandshakeMessage(buffer, protocol_id, info_hash, peer_id);
	}

	public static HandshakeMessage craft(String protocol_id, String info_hash, String peer_id) 
		throws Exception {
		ByteBuffer buffer = ByteBuffer.allocateDirect(
				protocol_id.length() + HandshakeMessage.BASE_SIZE);
		
		ValidateParemeters(protocol_id, info_hash, peer_id);
					
		buffer.put((byte)protocol_id.length());	// pstrlen
		buffer.put(protocol_id.getBytes()); // protocol_id
		buffer.put(new byte[8]); // reserved
		buffer.put(HashingUtils.hexStringToByteArray(info_hash));  // info_hash
		buffer.put(peer_id.getBytes()); // peer_id
		
		return new HandshakeMessage(buffer, protocol_id, info_hash, peer_id);
	}

	private static void ValidateParemeters(String protocol_id, String info_hash, String peer_id)
															throws Exception
	{
		if(info_hash.length() != 40 ||
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