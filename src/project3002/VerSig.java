package project3002;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.*;

class VerSig {

    public static void main(String[] args) {

        /* Verify a DSA signature */

        if (args.length != 3) {
            System.out.println("Usage: VerSig " +
                "certfile signaturefile " + "datafile");
        }
        else try {

        	/*FileInputStream keyfis = new FileInputStream(args[0]);
        	byte[] encKey = new byte[keyfis.available()];  
        	keyfis.read(encKey);

        	keyfis.close();*/
        	
        	@SuppressWarnings("resource")
			FileInputStream fis = new FileInputStream(args[1]);
        	ByteArrayInputStream bis = null;

        	byte value[] = new byte[fis.available()];
        	  fis.read(value);
        	  bis = new ByteArrayInputStream(value);
        	  
        	  CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        	
        	//X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encKey);
        	
        	KeyFactory keyFactory = KeyFactory.getInstance("DSA", "SUN");
        	
        	//PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);
        	
        	PublicKey pubKeyCert = certFactory.generateCertificate(bis).getPublicKey();
        	
        	FileInputStream sigfis = new FileInputStream(args[1]);
        	byte[] sigToVerify = new byte[sigfis.available()]; 
        	sigfis.read(sigToVerify);
        	sigfis.close();
        	
        	Signature sig = Signature.getInstance("SHA1withDSA", "SUN");
        	
        	sig.initVerify(pubKeyCert);
        	
        	FileInputStream datafis = new FileInputStream(args[2]);
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

        } catch (Exception e) {
            System.err.println("Caught exception " + e.toString());
        }
    }

}
