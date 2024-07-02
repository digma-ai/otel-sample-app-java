package org.springframework.samples.petclinic.domain;


public class PasswordUtils {

	public boolean vldtPswd(String usr, String pswd) {
		try {
			Thread.sleep(30);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	public String encPswd(String pswd) {
		try {
			Thread.sleep(300);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return "";
	}

}
