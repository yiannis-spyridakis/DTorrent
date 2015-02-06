package com.torr.bencode;
public class TorrentFileHandlerTester
{
	private TorrentFileHandler torrent_file_handler;

	private TorrentFile torrent_file;
       private String[] nums;

                 
        public String TrackerUrl()
        {
        return torrent_file.tracker_url;
        }
        public Integer FileLength()
        {
        return torrent_file.file_length;
        }
        public Integer PieceLength()
        {
        return torrent_file.piece_length;
        }
        public String SHA()
        {
            
        return torrent_file.info_hash_as_url;
        }
        public Integer SizeNumberOfallPieces()
        {
        int num;
        num=torrent_file.piece_hash_values_as_hex.size();
        return num;
        }
        public String []HashOfeachpiece()
        {for (int i = 0; i < nums.length; i++)
			{System.out.println(nums[i]);}
         return nums;
         }
        
	public TorrentFileHandlerTester(String path)
	{
		super();
                testTorrentFileHandler(path);
                
             	}

	public void testTorrentFileHandler(String path)
	{
		torrent_file_handler = new TorrentFileHandler();
		torrent_file = torrent_file_handler
				.openTorrentFile(path);

		if (torrent_file != null)
		{
                        int []values = new int[torrent_file.piece_hash_values_as_hex.size()];
                            this.nums= new String[torrent_file.piece_hash_values_as_hex.size()];
			
			for (int i = 0; i < torrent_file.piece_hash_values_as_hex.size(); i++)
			{
                            
				System.out.println("SHA-1 Hash for Piece ["
						+ i
						+ "]: "
						+ (String) torrent_file.piece_hash_values_as_url
								.elementAt(i));
                                
                                         values[i]=i;      
                                       this.nums[i]=(String) torrent_file.piece_hash_values_as_url
								.elementAt(i);
			System.out.println(values[i] + "    " + nums[i]); 
                            
                                                }                
		}
		else
		{
			System.err.println("Error: There was a problem when unencoding the file \".");
			System.err.println("\t\tPerhaps it does not exist.");
		}
                
	}
      

    
}
