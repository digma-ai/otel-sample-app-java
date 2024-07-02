package org.springframework.samples.petclinic.domain;


public class RoleService {

	public boolean vldtUsrRole(String usr, String sysCode) {
		try {
			Thread.sleep(40);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return true;
	}

}
