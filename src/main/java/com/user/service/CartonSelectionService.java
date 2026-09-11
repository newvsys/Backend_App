package com.user.service;

import com.user.model.CartonEO;
import com.user.model.OrderItemEO;
import com.user.model.ProductVariantEO;
import com.user.repository.CartonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class CartonSelectionService {

	private static final Logger logger = LoggerFactory.getLogger(CartonSelectionService.class);

	@Autowired
	private CartonRepository cartonRepository;

	@Autowired
	private PushNotificationService pushNotificationService;

	/**
	 * Main method — call this when order is placed. Returns the smallest existing
	 * active carton that fits the order (by volume and weight).
	 * <p>
	 * <b>Note:</b> automatic carton auto-creation has been removed. If no existing
	 * carton fits, this throws an {@link IllegalStateException} and notifies admins
	 * so a suitable carton can be added manually via the admin carton API — it will
	 * never silently fabricate a new carton on the fly.
	 */
	public CartonEO selectCarton(List<OrderItemEO> orderItems) {

		// Step 1: Calculate total volume and weight of all items
		double totalVolume = 0;
		double totalWeight = 0;

		for (OrderItemEO item : orderItems) {
			ProductVariantEO variant = item.getProductVar();
			// Quantity must be factored into both volume and weight so an order
			// with multiple units of the same variant is sized/matched correctly
			// against carton capacity — default to 1 defensively if unset.
			int qty = item.getQuantity() != null ? item.getQuantity() : 1;

			totalVolume += variant.getVolume() * qty;
			totalWeight += variant.getWeight() * qty;
		}

		// Step 2: Add 10% buffer volume for padding/air gaps
		double bufferedVolume = totalVolume * 1.10;

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

		// Step 5: No existing carton fits — automatic carton creation has been
		// removed. Notify admins so a suitable carton can be added manually, and
		// fail loudly instead of silently fabricating a carton on the fly.
		logger.error(
				"No existing active carton fits the order (volume={} cm³, weight={} kg). Automatic carton "
						+ "creation is disabled — a carton must be added manually via the admin carton API.",
				bufferedVolume, totalWeight);
		try {
		//	pushNotificationService.notifyAdminsNoCartonFit(bufferedVolume, totalWeight);
		}
		catch (Exception pushEx) {
			logger.error("Failed to send no-carton-fit push notification: {}", pushEx.getMessage(), pushEx);
		}
		throw new IllegalStateException(String.format(
				"No suitable carton found for this order (requires ~%.2f cm³, %.2f kg). Please add a carton "
						+ "via the admin carton API.",
				bufferedVolume, totalWeight));
	}


}
