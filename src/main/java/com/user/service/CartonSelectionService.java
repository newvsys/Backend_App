package com.user.service;

import com.user.model.CartonAssignment;
import com.user.model.CartonEO;
import com.user.model.OrderItemEO;
import com.user.model.ProductVariantEO;
import com.user.repository.CartonRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartonSelectionService {

	@Autowired
	private CartonRepository cartonRepository;

	/**
	 * Main method — call this when order is placed Returns the best carton for the order
	 */
	public CartonEO selectCarton(List<OrderItemEO> orderItems) {

		// Step 1: Calculate total volume and weight of all items
		double totalVolume = 0;
		double totalWeight = 0;

		for (OrderItemEO item : orderItems) {
			ProductVariantEO variant = item.getProductVar();
			int qty = item.getQuantity();

			totalVolume += variant.getVolume() * qty;
			totalWeight += variant.getWeight() * qty;
		}

		// Step 2: Add 20% buffer volume for padding/air gaps
		double bufferedVolume = totalVolume * 1.20;

		System.out.println("Total Volume : " + bufferedVolume + " cm³");
		System.out.println("Total Weight : " + totalWeight + " kg");

		// Step 3: Get all cartons sorted smallest to largest
		List<CartonEO> cartons = cartonRepository.findAllByOrderByLengthAscBreadthAscHeightAsc();

		// Step 4: Find smallest carton that fits everything
		for (CartonEO carton : cartons) {
			boolean volumeFits = carton.getVolume() >= bufferedVolume;
			boolean weightFits = carton.getMaxWeight() >= totalWeight;

			if (volumeFits && weightFits) {
				System.out.println("✅ Selected Carton: " + carton.getName());
				return carton;
			}
		}

		// Step 5: If no single carton fits → use largest + flag for multi-box
		System.out.println("⚠️ Order needs multiple boxes!");
		return cartonRepository.findLargest();
	}

	/**
	 * Multi-box algorithm — splits order into multiple cartons Used when order is too
	 * large for one box
	 */
	public List<CartonAssignment> selectMultipleCartons(List<OrderItemEO> orderItems) {

		List<CartonAssignment> assignments = new ArrayList<>();
		List<CartonEO> cartons = cartonRepository.findAllByOrderByLengthDescBreadthDescHeightDesc();
		CartonEO largestCarton = cartons.get(0);

		// Flatten items list (qty 2 of item A = 2 separate entries)
		List<ProductVariantEO> flatItems = new ArrayList<>();
		for (OrderItemEO item : orderItems) {
			for (int i = 0; i < item.getQuantity(); i++) {
				flatItems.add(item.getProductVar());
			}
		}

		// Sort items largest to smallest (First Fit Decreasing algorithm)
		flatItems.sort((a, b) -> Double.compare(b.getVolume(), a.getVolume()));

		double currentVolume = 0;
		double currentWeight = 0;
		List<ProductVariantEO> currentBoxItems = new ArrayList<>();

		for (ProductVariantEO product : flatItems) {
			boolean volumeExceeds = (currentVolume + product.getVolume() * 1.2) > largestCarton.getVolume();
			boolean weightExceeds = (currentWeight + product.getWeight()) > largestCarton.getMaxWeight();

			if (volumeExceeds || weightExceeds) {
				// Close current box, start new one
				// Convert ProductVariantEO list to OrderItemEO list with quantity 1 for
				// carton selection
				List<OrderItemEO> dummyOrderItems = new ArrayList<>();
				for (ProductVariantEO variant : currentBoxItems) {
					OrderItemEO dummy = new OrderItemEO();
					dummy.setProductVar(variant);
					dummy.setQuantity(1);
					dummyOrderItems.add(dummy);
				}
				CartonEO bestFit = selectCarton(dummyOrderItems);

				assignments.add(new CartonAssignment(bestFit, currentBoxItems));
				currentBoxItems = new ArrayList<>();
				currentVolume = 0;
				currentWeight = 0;
			}

			currentBoxItems.add(product);
			currentVolume += product.getVolume();
			currentWeight += product.getWeight();
		}

		// Add last box
		if (!currentBoxItems.isEmpty()) {
			CartonEO bestFit = selectBestCarton(currentBoxItems);
			assignments.add(new CartonAssignment(bestFit, currentBoxItems));
		}

		return assignments;
	}

	/**
	 * Helper — selects best carton for a flat list of products
	 */
	private CartonEO selectBestCarton(List<ProductVariantEO> products) {
		double vol = products.stream().mapToDouble(ProductVariantEO::getVolume).sum() * 1.2;
		double wt = products.stream().mapToDouble(ProductVariantEO::getWeight).sum();

		return cartonRepository.findAllByOrderByLengthAscBreadthAscHeightAsc()
			.stream()
			.filter(c -> c.getVolume() >= vol && c.getMaxWeight() >= wt)
			.findFirst()
			.orElse(cartonRepository.findLargest());
	}

}
