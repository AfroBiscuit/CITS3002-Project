package project3002;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class CertTest {

	X509Certificate x509Test;
	
	public void testValid() throws CertificateExpiredException, CertificateNotYetValidException{
		Date date = new Date();
		x509Test.checkValidity(date);
	}
	
	public void main(){
		
	}
}
