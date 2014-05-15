package project3002;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Handles the verification of a signature given the signature, a certificate for that signature and a file to compare against
 * @author Alex Guglielmino 20933584
 * @author Dominic Cockman 20927611
 *
 */
public class VerSig {

    public static void main(String[] args) {

        /* Verify a DSA signature */

        if (args.length != 3) {
            System.out.println("Usage: VerSig " +
                "certfile signaturefile " + "datafile");
        }
        else try {
     	
        	verifySign(args[0], args[1], args[2]);

        } catch (Exception e) {
            System.err.println("Caught exception " + e.toString());
        }
    }
    
    /**
     * Verifies a signature when given a certificate location, a signature location and the location of a file to verify
     * @param certLoc Certificate location
     * @param signLoc Signature location
     * @param file File location
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static boolean verifySign(String certLoc, String signLoc, String file) throws IOException, GeneralSecurityException{
    	
    	//adapted from http://docs.oracle.com/javase/tutorial/displayCode.html?code=http://docs.oracle.com/javase/tutorial/security/apisign/examples/VerSig.java
    	@SuppressWarnings("resource")
		FileInputStream fis = new FileInputStream(certLoc);
    	ByteArrayInputStream bis = null;

    	byte value[] = new byte[fis.available()];
    	  fis.read(value);
    	  bis = new ByteArrayInputStream(value);
    	  
    	  CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
    	  
    	  X509Certificate cert = (X509Certificate)certFactory.generateCertificate(bis);
    	
    	FileInputStream sigfis = new FileInputStream(signLoc);
    	byte[] sigToVerify = new byte[sigfis.available()]; 
    	sigfis.read(sigToVerify);
    	sigfis.close();
    	
    	Signature sig = Signature.getInstance("SHA1withDSA", "SUN");
    	
    	sig.initVerify(cert.getPublicKey());
    	
    	FileInputStream datafis = new FileInputStream(file);
    	BufferedInputStream bufin = new BufferedInputStream(datafis);

    	byte[] buffer = new byte[1024];
    	int len;
    	while (bufin.available() != 0) {
    	    len = bufin.read(buffer);
    	    sig.update(buffer, 0, len);
    	};

    	bufin.close();
    	
    	boolean verifies = sig.verify(sigToVerify);

    	System.out.println("signature verifies: " + verifies);
		return verifies;
    }
}
