package org.springframework.samples.petclinic.order;

import java.util.ArrayList;
import java.util.List;

public class Customer {
	private int customerId;
	private int age;
	private boolean eligible;
	private String address;
	private double creditScore;
	private double accountBalance;
	private double totalSpent;
	private boolean vipStatus;
	private List<Coupon> coupons;
	private int bonusPoints;

	// Constructor, getters and setters should be here...

	public boolean isEligible() {
		// Logic to determine eligibility
		return true;
	}

	public boolean hasValidAddress() {
		// Logic to validate address
		return true;
	}

	public boolean hasGoodCreditScore() {
		// Logic to check credit score
		return true;
	}

	public void addCoupon(Coupon coupon) {
		if (coupons == null) {
			coupons = new ArrayList<>();
		}
		coupons.add(coupon);
	}

	public void addBonusPoints(int points) {
		this.bonusPoints += points;
	}

	public int getCustomerId() {
			return 0;
	}

	public int getAge() {
		return 0;
	}

	public int getAccountBalance() {
		return 0;
	}

	public void setAccountBalance(double v) {

	}

	public double getTotalSpent() {
		return 0;
	}

	public void setVipStatus(boolean b) {

	}
}
