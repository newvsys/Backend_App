package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestCreateCartonDTO {

	private String name; // e.g. "Small", "Medium", "Large"

	private double length; // cm

	private double breadth; // cm

	private double height; // cm

	private double maxWeight; // kg (max weight it can hold)

	private double emptyWeight; // kg (box itself weight)

	/** User / identifier who is creating this carton record. */
	private String who;

}
