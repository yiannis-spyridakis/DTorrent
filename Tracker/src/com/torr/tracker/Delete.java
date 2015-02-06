package com.torr.tracker;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.torr.trackermsgs.MessageToClient;

public class Delete extends Thread {
	long currentTime;
	
	public	Delete() {}
	
	
	public void run() {
		
			while(true) {
				for(Map.Entry<String, List<MessageToClient>> entry : Tracker.peers.entrySet()) {
					for (Iterator<MessageToClient> iter = entry.getValue().iterator(); iter.hasNext(); ) {
						MessageToClient element = iter.next();
						System.out.println(System.currentTimeMillis() - element.timer);
						if(System.currentTimeMillis() - element.timer > 900000 ) {
								iter.remove();
						}
			   
					}
				}
				try {
					sleep(1800000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		
			
		}
		
	}

