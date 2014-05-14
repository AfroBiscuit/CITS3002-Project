package project3002;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

class VerSig {

    public static void main(String[] args) {

        /* Verify a DSA signature */

        if (args.length != 3) {
            System.out.println("Usage: VerSig " +
                "certfile signaturefile " + "datafile");
        }
        else try {
     	
        	@SuppressWarnings("resource")
			FileInputStream fis = new FileInputStream(args[0]);
        	ByteArrayInputStream bis = null;

        	byte value[] = new byte[fis.available()];
        	  fis.read(value);
        	  bis = new ByteArrayInputStream(value);
        	  
        	  CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        	  
        	  X509Certificate cert = (X509Certificate)certFactory.generateCertificate(bis);
        	
        	FileInputStream sigfis = new FileInputStream(args[1]);
        	byte[] sigToVerify = new byte[sigfis.available()]; 
        	sigfis.read(sigToVerify);
        	sigfis.close();
        	
        	Signature sig = Signature.getInstance("SHA1withDSA", "SUN");
        	
        	sig.initVerify(cert.getPublicKey());
        	
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
