package com.torr.msgs;
import java.io.Serializable;
import java.net.InetAddress;


public class MessageToClient implements Serializable {
	//hello
	private static final long serialVersionUID = 1L;
	public String peer_id;
	public InetAddress IP;
	public int port;
	public long timer = 0;
	
	public MessageToClient(String peer_id, InetAddress IP, int port) {
		super();
		this.peer_id = peer_id;
		this.IP = IP;
		this.port = port;
	}

	public String getPeer_id() {
		return peer_id;
	}

	public void setPeer_id(String peer_id) {
		this.peer_id = peer_id;
	}

	public InetAddress getIP() {
		return IP;
	}

	public void setIP(InetAddress iP) {
		IP = iP;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	
	public String toString() {
		return peer_id + "-" + IP + "-" + port;
	}
}
