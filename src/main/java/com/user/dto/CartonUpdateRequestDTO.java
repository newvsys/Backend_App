package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartonUpdateRequestDTO {

	private String name; // e.g. "Small", "Medium", "Large"

	private double length; // cm

	private double breadth; // cm

	private double height; // cm

	private double maxWeight; // kg

	private double emptyWeight; // kg

	/** User / identifier who is updating this carton record. */
	private String who;

}
