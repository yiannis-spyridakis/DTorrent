import java.io.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.BitSet;

import com.torr.msgs.PeerMessage;


public class ClientPeer extends Thread{
	Socket requestSocket;
	ObjectOutputStream out;
	ObjectInputStream in;
	InetAddress IP;

	
	public ClientPeer(Socket connection) {
		try
		{
			out	= new ObjectOutputStream(connection.getOutputStream());
			in = new ObjectInputStream(connection.getInputStream());
			IP = connection.getInetAddress()/*.getHostAddress()*/ ;
			
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public	ClientPeer(InetAddress IP, String peer_id, int port) {
		try {
			requestSocket =	new	Socket(IP, port);
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			in = new ObjectInputStream(requestSocket.getInputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void run() {
		try {
			
			PeerMessage.KeepAliveMessage kamsg = new PeerMessage.KeepAliveMessage();
			out.writeObject(kamsg);
			PeerMessage.ChokeMessage chmsg = new PeerMessage.ChokeMessage();
			out.writeObject(chmsg);
			PeerMessage.UnchokeMessage ucmsg = new PeerMessage.UnchokeMessage();
			out.writeObject(ucmsg);
			PeerMessage.InterestedMessage imsg = new PeerMessage.InterestedMessage();
			out.writeObject(imsg);
			PeerMessage.NotInterestedMessage nimsg = new PeerMessage.NotInterestedMessage();
			out.writeObject(nimsg);
			PeerMessage.HaveMessage hmsg = new PeerMessage.HaveMessage(100);
			out.writeObject(hmsg);
			BitSet bitfield = new BitSet(40);
			bitfield.set(0); bitfield.set(10); bitfield.set(20);
			PeerMessage.BitfieldMessage bfmsg = new PeerMessage.BitfieldMessage(bitfield);
			out.writeObject(bfmsg);
			PeerMessage.RequestMessage rmsg = new PeerMessage.RequestMessage(100, 512, 1024);
			out.writeObject(rmsg);
			ByteBuffer block = ByteBuffer.allocateDirect(256);
			PeerMessage.PieceMessage pmsg = new PeerMessage.PieceMessage(110, 512, block);
			out.writeObject(pmsg);
			PeerMessage.CancelMessage cmsg = new PeerMessage.CancelMessage(110, 256,  512);
			out.writeObject(cmsg);
			PeerMessage.HandshakeMessage hsmsg = new PeerMessage.HandshakeMessage("BitTorrent Protocol", "info_hash", "peer_id");
			out.writeObject(hsmsg);
			
			out.flush();
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		finally
		{
			try
			{
				in.close();
				out.close();
			}
			catch
			(IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}
	
	
}
