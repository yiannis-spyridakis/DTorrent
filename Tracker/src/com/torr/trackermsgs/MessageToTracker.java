package com.torr.trackermsgs;
import java.io.Serializable;

public class MessageToTracker implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public String info_hash;
	public String peer_id;
	public int port;
	
	
	public MessageToTracker(String info_hash, String peer_id, int port) {
		super();
		this.info_hash = info_hash;
		this.peer_id = peer_id;
		this.port = port;
		
	}
	

	public String toString() {
		return info_hash + " - " + peer_id + " - " + port;
	}


	public String getInfo_hash() {
		return info_hash;
	}


	public void setInfo_hash(String info_hash) {
		this.info_hash = info_hash;
	}


	public String getPeer_id() {
		return peer_id;
	}


	public void setPeer_id(String peer_id) {
		this.peer_id = peer_id;
	}


	public int getPort() {
		return port;
	}


	public void setPort(int port) {
		this.port = port;
	}
	
}