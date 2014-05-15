package project3002;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

/**
 * Creates certificates from given information
 * @author Alex Guglielmino 20933584
 * @author Dominic Cockman 20927611
 */
public class CertTest {

	X509Certificate x509Test;
	/**
	 * Generates a certificate with information given to it
	 * @param issuee Who the cert is for
	 * @param issuer Who issued it
	 * @param days How long its valid for
	 * @param algorithm the algorithm used to encode it
	 * @return a valid x509certificate
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	static X509Certificate generateCertificate(String issuee, String issuer, int days, String algorithm)
			throws GeneralSecurityException, IOException {
		
		//need to impose a 16 character minimum
		if(issuee.length()<16||issuer.length()<16){
			System.out.println("Issuee/ issuer names not long enough. There is a 16 character minimum.");
			return null;
		}
		else{
		
		//generate a private/public key pair for this cert
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
    	SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
    	keyGen.initialize(1024, random);
    	
    	KeyPair pair = keyGen.generateKeyPair();
    	
		PrivateKey privkey = pair.getPrivate(); //private key
		PublicKey pubkey = pair.getPublic(); //public key
		X509CertInfo info = new X509CertInfo(); //new info structure for the cert
		Date from = new Date(); //valid from date
		Date to = new Date(from.getTime() + days * 86400000l); //valid to date
		CertificateValidity interval = new CertificateValidity(from, to); //interval of validity
		BigInteger sn = new BigInteger(64, new SecureRandom()); 
		X500Name owner = new X500Name(issuee); //recipient
		X500Name issuedby = new X500Name(issuer); //issuer

		info.set(X509CertInfo.VALIDITY, interval); //setting that info into the cert
		info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
		info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
		info.set(X509CertInfo.ISSUER, new CertificateIssuerName(issuedby));
		info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
		info.set(X509CertInfo.VERSION, new CertificateVersion(
				CertificateVersion.V3));
		AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
		info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

		// Sign the cert to identify the algorithm that's used.
		X509CertImpl cert = new X509CertImpl(info);
		cert.sign(privkey, algorithm);

		// Update the algorithm, and resign.
		algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
		info.set(CertificateAlgorithmId.NAME + "."
				+ CertificateAlgorithmId.ALGORITHM, algo);
		cert = new X509CertImpl(info);
		cert.sign(privkey, algorithm);

		//System.out.println(cert);

		 // Get subject
        Principal principal = cert.getSubjectDN();
        String subjectDn = principal.getName();

        // Get issuer
        principal = cert.getIssuerDN();
        byte[] key = pubkey.getEncoded();
    	FileOutputStream keyfos = new FileOutputStream("./ClientPublicKeys/" + subjectDn + "_pub.key");
    	keyfos.write(key);
    	keyfos.close();
    	
    	PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
				privkey.getEncoded());
    	FileOutputStream prikeyfos = new FileOutputStream("./ClientPrivateKeys/" + subjectDn + "_pri.key");
    	prikeyfos.write(pkcs8EncodedKeySpec.getEncoded());
    	prikeyfos.close();
		
		/* save the signature in a file */
		File certDest = new File("./ClientCerts/" + subjectDn + ".cer");
		FileOutputStream certout = new FileOutputStream(certDest);
		certout.write(cert.getEncoded());
		certout.close();
		return cert;
		}
	}  
	
	/**
	 * returns the diameter of the ring of trust for a given certificate
	 * @param certLoc location of that certificate
	 * @return the diameter of the ring
	 */
	public static int getROTDiameter(String certLoc){
		int diameter = 0;
		//instantiate the first certificate, and then loop from there
		try{
		@SuppressWarnings("resource")
		FileInputStream fis = new FileInputStream(certLoc);
    	ByteArrayInputStream bis = null;
    	byte value[] = new byte[fis.available()];
    	fis.read(value);
    	bis = new ByteArrayInputStream(value);
    	CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
    	X509Certificate cert = (X509Certificate)certFactory.generateCertificate(bis);
    	
    	//System.out.println("First Succeeded");
    	
    	//Instantiate the first certificate's issuer's cert, ready for a loop to check
    	String certIssuerPath = "./ServerCertificates/" + cert.getIssuerDN().getName() + ".cer";
    	//System.out.println(certIssuerPath);

    	
		@SuppressWarnings("resource")
		FileInputStream fisCheck = new FileInputStream(certIssuerPath);
    	ByteArrayInputStream bisCheck = null;
    	byte valueCheck[] = new byte[fisCheck.available()];
    	fisCheck.read(valueCheck);
    	bisCheck = new ByteArrayInputStream(valueCheck);
    	X509Certificate certCheck = (X509Certificate)certFactory.generateCertificate(bisCheck);
    	diameter++;
    	
    	//System.out.println("Second Succeeded");
    	while(!(cert.equals(certCheck))){
    		certIssuerPath = "./ServerCertificates/" + certCheck.getIssuerDN().getName();
    		
    		@SuppressWarnings("resource")
    		FileInputStream fisCheck2 = new FileInputStream(certIssuerPath + ".cer");
        	ByteArrayInputStream bisCheck2 = null;
        	byte valueCheck2[] = new byte[fisCheck2.available()];
        	fisCheck2.read(valueCheck2);
        	bisCheck2 = new ByteArrayInputStream(valueCheck2);
        	certCheck = (X509Certificate)certFactory.generateCertificate(bisCheck2);
        	diameter++;
    	}
		}
		catch(FileNotFoundException e){
			System.out.println("Ring does not exist: certificate missing");
		}
		catch(Exception e){
			System.out.println("Something's gone wrong - unable to retrieve diameter");
			e.printStackTrace();
		}
    	
		return diameter;
	}
	
	public static void main(String[] args) throws GeneralSecurityException, IOException{

		//generating some certs for use later on
		generateCertificate("CN = AlexGuglielmino14", "CN = DominicCockman14", 1, "DSA");
		generateCertificate("CN = DominicCockman14", "CN = AlexGuglielmino14", 1, "DSA");
		generateCertificate("CN = TomBombadillo14", "CN = AlexGuglielmino14", 1, "DSA");
		generateCertificate("CN = LeonardoDiCaprio14", "CN = TomBombadillo14", 1, "DSA");
		generateCertificate("CN = WakaFlackaFlame14", "CN = DominicCockman14", 1, "DSA");
		generateCertificate("CN = DominicCockman14", "CN = DominicCockman14", 1, "DSA");
		generateCertificate("CN = MicrosoftMegaCorp14", "CN = AlexGuglielmino14", 1, "DSA");
	}
}
