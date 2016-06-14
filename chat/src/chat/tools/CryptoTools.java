/**
 * File			: CryproTools.java
 * Package		: asechat.tools
 * Classes		: CryptoTools
 * Description	: Provides cryptographic tools
 * 
 * Author		: Mathias Knoll
 * Year			: 2009
 * Subject		: Applied Cryptography (William Farrelly)
 * 
 * FH JOANNEUM - Kapfenberg - 2009
 */
package asechat.tools;

// Imports
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

/**
 * CryptoTools:
 * This set of tools provides methods concerning cryptographic functionality
 * @author Mathias Knoll
 */
public class CryptoTools {

	/**
	 * Get MessageDigest 5 Key
	 * @param arbData Payload for key calculation
	 * @return Key as a byte array
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] getMD5Key(byte[] arbData) 
		throws NoSuchAlgorithmException{
		MessageDigest msgDigest = MessageDigest.getInstance("MD5");
		msgDigest.update(arbData);
		return msgDigest.digest();
	}
	
	/**
	 * Get a Hex String of a byte array
	 * @param b
	 * @return
	 * @throws Exception
	 */
	public static String getHex(byte[] arByte) throws Exception {
		String result = "";
		for (int i=0; i < arByte.length; i++) {
			result +=
		      Integer.toString( ( arByte[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		return result;
	}
	
	/**
	 * Get a byte array from a hex string
	 * @param sHex
	 * @return
	 */
	public static byte[] getBytes(String sHex){
		int len = sHex.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(sHex.charAt(i), 16) << 4)
	                             + Character.digit(sHex.charAt(i+1), 16));
	    }
	    return data;
	}
	
	/**
	 * Cipher a key with a PublicKey
	 * @param keyPublic
	 * @param arbKey
	 * @return
	 */
	public static byte[] cipherKey (PublicKey keyPublic, byte[] arbKey){
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, keyPublic);
			return cipher.doFinal(arbKey);
		} catch (Exception e) {
			System.out.println("[CryptoTool] Exception: " + e.getMessage());
			return new byte[]{};
		}
	}
	
	/**
	 * Decipher a key with PrivateKey
	 * @param keyPrivate
	 * @param arbKey
	 * @return
	 */
	public static byte[] decipherKey (PrivateKey keyPrivate, byte[] arbKey){
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, keyPrivate);
			return cipher.doFinal(arbKey);		
		} catch (Exception e) {
			System.out.println("[CryptoTool] Exception: " + e.getMessage());
			return new byte[]{};
		}
	}
	
	/**
	 * Encrypt data with secret key (AES)
	 * @param keySecret
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static byte[] encryptData(
			SecretKey keySecret, byte[] data) throws Exception{
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE , keySecret);
		return cipher.doFinal(data);
	}
	
	/**
	 * Decrypt data with secret key (AES)
	 * @param keySecret
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static byte[] decryptData( 
			SecretKey keySecret, byte[] data) throws Exception{
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE , keySecret);
		return cipher.doFinal(data);
	}
	
	
	/** 
	 * Create signed key
	 * @return
	 */
	public static byte[] signedKey(PrivateKey keyPrivate, byte[] arbKey){
		try {
			// Signature with MD5 and RSA
			Signature sig = Signature.getInstance("MD5withRSA");
			// Initialize signature with private key
			sig.initSign(keyPrivate);
			// Transfer key data into signature
			sig.update(CryptoTools.getMD5Key(arbKey));
			// Sign the key data and get bytes
			byte[] signedEncryptedSessionKey = sig.sign();
			// return byte array
			return signedEncryptedSessionKey;
		} catch (Exception e) {
			System.out.println("[CryptoTool] Exception: " + e.getMessage());
			return new byte[]{};
		}
	}
	
	/** 
	 * Verify signed key
	 * @return
	 */
	public static boolean verifySignedKey(
			PublicKey keyPublic, byte[] arbKey, byte[] arbData){
		try {
			// Signature with MD5 and RSA
			Signature sig = Signature.getInstance("MD5withRSA");
			//Initialize signature using public key
			sig.initVerify(keyPublic);
			// Encrypted session key)
			sig.update(CryptoTools.getMD5Key(arbKey));
			// Perform verification with received signature bytes
			return sig.verify(arbData);
		} catch (Exception e) {
			System.out.println("[CryptoTool] Exception: " + e.getMessage());
			return false;
		}
	}
}
