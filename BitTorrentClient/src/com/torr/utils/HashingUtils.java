package com.torr.utils;

import java.security.MessageDigest;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

public class HashingUtils {
	
	final private static String DIGEST_ID = "SHA-1";
	final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
	
	public static byte[] SHA1(byte[] input)
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance(DIGEST_ID);		
			digest.update(input);
			return digest.digest();
		}
		catch(NoSuchAlgorithmException ex)
		{
			return null;
		}
	}
	public static byte[] SHA1(ByteBuffer byteBuffer)
	{
		byteBuffer.rewind();
		byte[] dataBytes = new byte[byteBuffer.remaining()];
		return SHA1(dataBytes);
	}
	
	public static String stringSHA1(byte[] input)
	{
		return bytesToHex(SHA1(input));
	}
	
	
	private static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
}
