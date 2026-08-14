package com.user.service;

import com.user.model.CartonAssignment;
import com.user.model.CartonEO;
import com.user.model.OrderItemEO;
import com.user.model.ProductVariantEO;
import com.user.repository.CartonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartonSelectionService {

	private static final Logger logger = LoggerFactory.getLogger(CartonSelectionService.class);

	/** Extra padding applied to computed dimensions/weight when auto-creating a carton. */
	private static final double DIMENSION_BUFFER = 1.10;

	private static final double WEIGHT_BUFFER = 1.15;

	@Autowired
	private CartonRepository cartonRepository;

	@Autowired
	private PushNotificationService pushNotificationService;

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

		// Step 5: No existing carton fits — dynamically create a properly sized new
		// carton based on the order items/quantities (with buffer), persist it, notify
		// admins via Firebase push, and return it to the caller.
		logger.warn("No existing carton fits the order (volume={} cm³, weight={} kg). Auto-creating a new carton.",
				bufferedVolume, totalWeight);
		return createCartonForOrder(orderItems, totalWeight);
	}

	/**
	 * Dynamically builds and persists a new {@link CartonEO} sized to fit the given
	 * order items (with a safety buffer), then sends a best-effort Firebase push
	 * notification to admins informing them a new carton was auto-created.
	 */
	private CartonEO createCartonForOrder(List<OrderItemEO> orderItems, double totalWeight) {
		double maxLength = 0;
		double maxBreadth = 0;
		double totalHeight = 0;

		for (OrderItemEO item : orderItems) {
			ProductVariantEO variant = item.getProductVar();
			int qty = item.getQuantity();

			double itemLength = toDouble(variant.getLength());
			double itemBreadth = toDouble(variant.getBreadth());
			double itemHeight = toDouble(variant.getHeight());

			maxLength = Math.max(maxLength, itemLength);
			maxBreadth = Math.max(maxBreadth, itemBreadth);
			// Items are assumed stacked vertically inside the carton.
			totalHeight += itemHeight * qty;
		}

		double newLength = roundUp(maxLength * DIMENSION_BUFFER);
		double newBreadth = roundUp(maxBreadth * DIMENSION_BUFFER);
		double newHeight = roundUp(totalHeight * DIMENSION_BUFFER);

		// Fallback to a cube derived from total buffered volume if item dimensions were
		// missing/zero (e.g. legacy variants without length/breadth/height set).
		if (newLength <= 0 || newBreadth <= 0 || newHeight <= 0) {
			double totalVolume = 0;
			for (OrderItemEO item : orderItems) {
				ProductVariantEO variant = item.getProductVar();
				totalVolume += variant.getVolume() * item.getQuantity();
			}
			double bufferedVolume = Math.max(totalVolume * DIMENSION_BUFFER, 1);
			double cubeSide = roundUp(Math.cbrt(bufferedVolume));
			newLength = newLength > 0 ? newLength : cubeSide;
			newBreadth = newBreadth > 0 ? newBreadth : cubeSide;
			newHeight = newHeight > 0 ? newHeight : cubeSide;
		}

		double newMaxWeight = roundUp(Math.max(totalWeight * WEIGHT_BUFFER, 0.1));
		double newEmptyWeight = roundUp(Math.max(totalWeight * 0.05, 0.2));

		String cartonName = String.format("AUTO-%.0fx%.0fx%.0f-%d", newLength, newBreadth, newHeight,
				System.currentTimeMillis() % 100000);

		CartonEO newCarton = CartonEO.builder()
			.name(cartonName)
			.length(newLength)
			.breadth(newBreadth)
			.height(newHeight)
			.maxWeight(newMaxWeight)
			.emptyWeight(newEmptyWeight)
			.status("A")
			.who("SYSTEM_AUTO")
			.build();

		CartonEO savedCarton = cartonRepository.save(newCarton);
		System.out.println("🆕 Created new Carton: " + savedCarton.getName());
		logger.info(
				"Auto-created new carton id={}, name={}, dimensions={}x{}x{} cm, maxWeight={} kg (no existing carton fit the order)",
				savedCarton.getId(), savedCarton.getName(), newLength, newBreadth, newHeight, newMaxWeight);

		// Best-effort admin push notification — never breaks order processing.
		try {
			pushNotificationService.notifyAdminsNewCarton(savedCarton);
		}
		catch (Exception pushEx) {
			logger.error("Failed to send new-carton push notification for cartonId={}: {}", savedCarton.getId(),
					pushEx.getMessage(), pushEx);
		}

		return savedCarton;
	}

	private double toDouble(BigDecimal value) {
		return value != null ? value.doubleValue() : 0.0;
	}

	/** Rounds up to 1 decimal place so computed dimensions/weights never undershoot. */
	private double roundUp(double value) {
		return Math.ceil(value * 10.0) / 10.0;
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
