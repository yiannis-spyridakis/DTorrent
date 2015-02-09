import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;

import com.torr.msgs.PeerMessage;

public class ClientTracker extends Thread {
	
	private int portId;
	
	//public MessageToTracker message;
	
	ClientTracker(int portId) 
	{
		this.portId = portId;
		//message = new MessageToTracker("sha1", "peer1", this.portId);
	}
	
	public void	run() {
		Socket requestSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in =	null;
		
		
		
		try
		{		
			requestSocket =	new	Socket(InetAddress.getLocalHost(), this.portId);
			System.out.println("Client listening at: " + requestSocket.getInetAddress().getHostAddress());
			
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			in = new ObjectInputStream(requestSocket.getInputStream());
		
			while(true) {				
				PeerMessage msg = (PeerMessage)in.readObject();
				switch(msg.getType())
				{
				case KEEP_ALIVE:
					PeerMessage.KeepAliveMessage alivemsg = (PeerMessage.KeepAliveMessage)msg;
					System.out.println("Client received keep alive message");
					break;
				case CHOKE:
					PeerMessage.ChokeMessage chokemsg = (PeerMessage.ChokeMessage)msg;
					System.out.println("Client received choke message");
					break;
				case UNCHOKE:
					PeerMessage.UnchokeMessage unchokemsg = (PeerMessage.UnchokeMessage)msg;
					System.out.println("Client received unchoke message");
					break;
				case INTERESTED:
					PeerMessage.InterestedMessage intmsg = (PeerMessage.InterestedMessage)msg;
					System.out.println("Client received interested message");
					break;
				case NOT_INTERESTED:
					PeerMessage.NotInterestedMessage nintmsg = (PeerMessage.NotInterestedMessage)msg;
					System.out.println("Client received 'not interested' message");
					break;
				case HAVE:
					PeerMessage.HaveMessage havemsg = (PeerMessage.HaveMessage)msg;
					System.out.println("Client received have message: #" + havemsg.getPieceIndex());					
					break;
				case BITFIELD:
					PeerMessage.BitfieldMessage bfmsg = (PeerMessage.BitfieldMessage)msg;
					System.out.println("Client received bitfield message:" + bfmsg);					
					break;					
				case REQUEST:
					PeerMessage.RequestMessage rmsg = (PeerMessage.RequestMessage)msg;
					System.out.println("Client received request message:" + rmsg);					
					break;					
				case PIECE:
					PeerMessage.PieceMessage pmsg = (PeerMessage.PieceMessage)msg;
					System.out.println("Client received piece message:" + pmsg);					
					break;
				case CANCEL:
					PeerMessage.CancelMessage cmsg = (PeerMessage.CancelMessage)msg;
					System.out.println("Client received cancel message:" + cmsg);					
					break;					
				case HANDSHAKE:
					PeerMessage.HandshakeMessage hmsg = (PeerMessage.HandshakeMessage)msg;
					System.out.println("Client received handshake message:" + hmsg);					
					break;					
					
				default:
					break;
				}				
				
				sleep(100);
			}
		}
		catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(EOFException ex)
		{
			System.out.println("Connection ended");
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
		finally
		{
			try {
				in.close();
				out.close();
				requestSocket.close();
			}
			catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}
	
}