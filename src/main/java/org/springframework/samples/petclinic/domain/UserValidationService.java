package org.springframework.samples.petclinic.domain;


public class UserValidationService {

	public boolean vldtUsr(String usr) {

		try {
			Thread.sleep(300);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return true;
	}

}
