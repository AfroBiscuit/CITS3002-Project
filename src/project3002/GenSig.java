package project3002;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

class GenSig {

    public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, CertificateException, IOException {

        /* Generate a DSA signature */

        if (args.length != 3) {
            System.out.println("Usage: GenSig nameOfFileToSign nameOfCert nameOfPriKey");
        }
        else{
       	
        	@SuppressWarnings("resource")
			FileInputStream fis = new FileInputStream(args[1]);
        	ByteArrayInputStream bis = null;

        	byte value[] = new byte[fis.available()];
        	  fis.read(value);
        	  bis = new ByteArrayInputStream(value);
        	  
        	  CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        	  
        	generate(args[0], (X509Certificate)certFactory.generateCertificate(bis), args[2]);
        }
    }
    
    public static void generate(String file, X509Certificate cert, String privateKey){

        /* Generate a DSA signature */

    	try {

    		String name = cert.getSubjectX500Principal().toString();
    		
        	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
        	SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        	keyGen.initialize(1024, random);
        	
        	File filePrivateKey = new File(file);
    		FileInputStream fios = new FileInputStream(file);
    		byte[] encodedPrivateKey = new byte[fios.available()];
    		fios.read(encodedPrivateKey);
    		fios.close();
        	
    		KeyFactory keyFactory = KeyFactory.getInstance("DSA");
    		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
    				encodedPrivateKey);
    		PrivateKey privKey = keyFactory.generatePrivate(privateKeySpec);
    		
        	PublicKey pub = cert.getPublicKey();
        	
        	Signature dsa = Signature.getInstance("SHA1withDSA", "SUN");
        	dsa.initSign(privKey);
        	
        	FileInputStream fis = new FileInputStream(file);
        	BufferedInputStream bufin = new BufferedInputStream(fis);
        	byte[] buffer = new byte[1024];
        	int len;
        	while((len = bufin.read(buffer)) >=0){
        		dsa.update(buffer, 0, len);
        	};
        	bufin.close();
        	
        	byte[] realSig = dsa.sign();
        	
        	/* save the signature in a file */
        	FileOutputStream sigfos = new FileOutputStream(file + "_" + name + ".sig");
        	sigfos.write(realSig);
        	sigfos.close();

        } catch (Exception e) {
            System.err.println("Caught exception " + e.toString());
        }
    }
}
