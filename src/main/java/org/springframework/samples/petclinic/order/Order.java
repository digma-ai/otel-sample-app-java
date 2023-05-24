package org.springframework.samples.petclinic.order;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
	private int orderId;
	private double totalPrice;
	private LocalTime time;

	// Constructor, getters and setters should be here...

	public double getTotalPrice() {
		// Logic to calculate total price
		return 0;
	}

	public int getOrderId() {
		return 0;
	}

	public LocalDateTime getTime() {
		return null;
	}
}

