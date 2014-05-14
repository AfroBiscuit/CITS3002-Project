package project3002;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
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

public class CertTest {

	X509Certificate x509Test;

	
	/*X509Certificate generateCertificate(String dn, KeyPair pair, int days, String algorithm){
		
		PrivateKey privKey = pair.getPrivate();
		X509CertInfo info = new X509CertInfo();
		Date from = new Date();
		Date to = new Date(from.getTime() + days*86400000l);
		
		
	}*/
	
	
	// not yours, adapt this code though
	static X509Certificate generateCertificate(String issuee, String issuer, KeyPair pair, int days, String algorithm)
			throws GeneralSecurityException, IOException {
		PrivateKey privkey = pair.getPrivate();
		PublicKey pubkey = pair.getPublic();
		X509CertInfo info = new X509CertInfo();
		Date from = new Date();
		Date to = new Date(from.getTime() + days * 86400000l);
		CertificateValidity interval = new CertificateValidity(from, to);
		BigInteger sn = new BigInteger(64, new SecureRandom());
		X500Name owner = new X500Name(issuee);
		X500Name issuedby = new X500Name(issuer);

		info.set(X509CertInfo.VALIDITY, interval);
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

		// Update the algorith, and resign.
		algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
		info.set(CertificateAlgorithmId.NAME + "."
				+ CertificateAlgorithmId.ALGORITHM, algo);
		cert = new X509CertImpl(info);
		cert.sign(privkey, algorithm);

		System.out.println(cert);

    	byte[] key = pubkey.getEncoded();
    	FileOutputStream keyfos = new FileOutputStream(owner.getCommonName() + "_pub.pk");
    	keyfos.write(key);
    	keyfos.close();
    	
    	PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
				privkey.getEncoded());
    	FileOutputStream prikeyfos = new FileOutputStream(owner.getCommonName() + "_pri.pk");
    	prikeyfos.write(pkcs8EncodedKeySpec.getEncoded());
    	prikeyfos.close();
		
		/* save the signature in a file */
		File certDest = new File(owner.getCommonName() + ".cer");
		FileOutputStream certout = new FileOutputStream(certDest);
		certout.write(cert.getEncoded());
		certout.close();
		return cert;
	}  
	
	public void testValid(Date currentDate) throws CertificateExpiredException, CertificateNotYetValidException{
		x509Test.checkValidity(currentDate);
	}
	
	public Principal getIssues(){
		return x509Test.getIssuerDN();
	}
	
	public BigInteger getSerialNumber(){
		return x509Test.getSerialNumber();
	}
	
	
	
	public static void main(String[] args) throws GeneralSecurityException, IOException{
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
    	SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
    	keyGen.initialize(1024, random);
    	
    	KeyPair pair = keyGen.generateKeyPair();
		generateCertificate("CN = Alex", "CN = Dom", pair, 1, "DSA");
	}
}
