package org.springframework.samples.petclinic.sample;

import my.pkg.SqsProvider;
import my.pkg.SqsProviderVersionOne;

import java.util.concurrent.RecursiveAction;

public class SqSTaskTwo extends RecursiveAction {


	@Override
	protected void compute() {
		SqsProviderVersionOne.getInstance().sqsCall();
	}
}
