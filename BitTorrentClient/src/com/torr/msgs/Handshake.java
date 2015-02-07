package com.torr.msgs;

import java.io.Serializable;

public class Handshake implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public String info_hash;
	public String peer_id;
	
	public Handshake(String info_hash, String peer_id){
		this.info_hash = info_hash;
		this.peer_id = peer_id;
	}
}
