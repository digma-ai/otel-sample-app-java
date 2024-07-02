package org.springframework.samples.petclinic.domain;


public class TwoFactorAuthenticationService {

	public boolean init2FA(String usr) {
		try {
			Thread.sleep(400);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	public String getTokenInput() {
		return "";
	}

	public boolean vldtToken(String usr, String token) {
		try {
			Thread.sleep(40);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

}
