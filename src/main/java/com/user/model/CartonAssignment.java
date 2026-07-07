package com.user.model;

import java.util.List;
import com.user.model.ProductVariantEO;

public class CartonAssignment {

	private CartonEO carton;

	private List<ProductVariantEO> items;

	private double totalVolume;

	private double totalWeight;

	public CartonAssignment(CartonEO cartonEO, List<ProductVariantEO> items) {
		this.carton = cartonEO;
		this.items = items;
		this.totalWeight = items.stream().mapToDouble(ProductVariantEO::getWeight).sum() + carton.getEmptyWeight();
		this.totalVolume = items.stream().mapToDouble(ProductVariantEO::getVolume).sum();
	}

}
