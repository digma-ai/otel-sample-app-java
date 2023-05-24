package org.springframework.samples.petclinic.order;

import org.springframework.samples.petclinic.order.Customer;
import org.springframework.samples.petclinic.order.Order;
import org.springframework.samples.petclinic.order.PaymentInfo;
import org.springframework.samples.petclinic.order.Product;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class OrderProcessor {
	private final PolicyService policyService	;

	public enum OrderStatus {
		SUCCESS,
		FAILURE,
		PENDING,
		CANCELLED
	}

	public OrderProcessor(){
		this.policyService=new PolicyService();
	}


	public CompletableFuture<OrderStatus> processOrder(Order order, Customer customer, List<Product> productList, PaymentInfo paymentInfo) {
		return CompletableFuture.supplyAsync(() -> {
			if (order == null || customer == null || productList == null || productList.isEmpty() || paymentInfo == null) {
				return OrderStatus.FAILURE;
			}

			// Initial validations
			if (order.getOrderId() <= 0 || customer.getCustomerId() <= 0) {
				return OrderStatus.FAILURE;
			}

			// Checking if the customer is eligible for ordering
			if (!customer.isEligible()) {
				if (customer.getAge() < policyService.GetUnderAgeValue()) {
					System.out.println("Customer is under age");
					return OrderStatus.FAILURE;
				} else if (!customer.hasValidAddress()) {
					System.out.println("Customer address is not valid");
					return OrderStatus.FAILURE;
				} else if (!customer.hasGoodCreditScore()) {
					System.out.println("Customer credit score is not sufficient");
					return OrderStatus.FAILURE;
				}
			}

			// Checking if the products are available in stock
			for (Product product : productList) {
				if (product.getStock() <= 0) {
					System.out.println("Product out of stock: " + product.getProductId());
					return OrderStatus.FAILURE;
				}
			}

			// Checking if the payment method is valid
			if (!paymentInfo.isValid()) {
				System.out.println("Payment method is invalid");
				return OrderStatus.FAILURE;
			}

			// Checking if the customer has sufficient balance for the order
			double totalOrderPrice = order.getTotalPrice();
			if (customer.getAccountBalance() < totalOrderPrice) {
				System.out.println("Customer does not have sufficient balance");
				return OrderStatus.FAILURE;
			}

			// If all conditions are met, then process the order
			for (Product product : productList) {
				// Decrease the stock of each product asynchronously
				CompletableFuture.runAsync(product::decreaseStock);
			}

			// Deduct the order price from the customer account balance
			customer.setAccountBalance(customer.getAccountBalance() - totalOrderPrice);

			// If the customer has spent more than a certain amount, give them VIP status
			CompletableFuture.runAsync(() -> {
				if (customer.getTotalSpent() + totalOrderPrice > this.policyService.GetVIPMinimum()) {
					customer.setVipStatus(true);
				}
			});

			// If the customer has bought a certain product, give them a coupon
			productList.stream()
				.filter(product -> product.getProductId() == 123)
				.findAny()
				.ifPresent(product -> CompletableFuture.runAsync(() -> customer.addCoupon(new Coupon())));

			// If the order is placed during a certain time, add bonus points to the customer
			if (order.getTime().getHour() >= 12 && order.getTime().getHour() <= 17) {
				CompletableFuture.runAsync(() -> customer.addBonusPoints(100));
			}

			System.out.println("Order processed successfully");
			return OrderStatus.SUCCESS;
		});
	}
}
