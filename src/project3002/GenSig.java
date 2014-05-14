package project3002;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
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
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

class GenSig {

    public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, CertificateException, IOException {

        /* Generate a DSA signature */

        if (args.length != 3) {
            System.out.println("Usage: GenSig nameOfFileToSign nameOfCert nameOfPriKey");
        }
        else{
        	generate(args[0], args[1], args[2]);
        }
    }
    
    /**
     * Generates a signature given a file to sign, the location of a certificate to sign it, and the private key to sign it with
     * @param file file to be signed
     * @param certLoc certificate that is vouching for said file
     * @param privateKey private key to sign it
     */
    public static void generate(String file, String certLoc, String privateKey){

        /* Generate a DSA signature */

    	try {

        	@SuppressWarnings("resource")
			FileInputStream fis = new FileInputStream(certLoc);
        	ByteArrayInputStream bis = null;

        	byte value[] = new byte[fis.available()];
        	  fis.read(value);
        	  bis = new ByteArrayInputStream(value);
        	  
        	  CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        	  
        	  X509Certificate cert = (X509Certificate)certFactory.generateCertificate(bis);
    		
    		String name = cert.getSubjectDN().getName();
    		
        	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
        	SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        	keyGen.initialize(1024, random);
        	      	
        	KeyPair loaded = LoadKeyPair("DSA", name);
        	
        	Signature dsa = Signature.getInstance("SHA1withDSA", "SUN");
        	dsa.initSign(loaded.getPrivate());
        	
        	FileInputStream fios = new FileInputStream(file);
        	BufferedInputStream bufin = new BufferedInputStream(fios);
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
    
    //Loads in a keypair from file to be able to recreate the original combo
    public static KeyPair LoadKeyPair(String algorithm, String owner)
			throws IOException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		// Read Public Key.
		File filePublicKey = new File(owner + "_pub.key");
		FileInputStream fis = new FileInputStream(owner + "_pub.key");
		byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
		fis.read(encodedPublicKey);
		fis.close();
 
		// Read Private Key.
		File filePrivateKey = new File(owner + "_pri.key");
		fis = new FileInputStream(owner + "_pri.key");
		byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
		fis.read(encodedPrivateKey);
		fis.close();
 
		// Generate KeyPair.
		KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
				encodedPublicKey);
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
 
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
				encodedPrivateKey);
		PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
 
		return new KeyPair(publicKey, privateKey);
	}
}
