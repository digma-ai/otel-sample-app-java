package org.springframework.samples.petclinic.sample;

import my.pkg.SqsProvider;

import java.util.concurrent.RecursiveAction;

public class SqSTask extends RecursiveAction {


	@Override
	protected void compute() {
		SqsProvider.getInstance().sqsCall();
	}
}

