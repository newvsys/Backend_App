package com.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carton")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartonEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "name")
	private String name; // e.g. "Small", "Medium", "Large"

	@Column(name = "length")
	private double length; // cm

	@Column(name = "breadth")
	private double breadth; // cm

	@Column(name = "height")
	private double height; // cm

	@Column(name = "maxWeight")
	private double maxWeight; // kg (max weight it can hold)

	@Column(name = "emptyWeight")
	private double emptyWeight; // kg (box itself weight)

	/** 'A' = Active, 'I' = Inactive. Only Active cartons are used for selection. */
	@Column(name = "status")
	private String status = "A";

	/** User / identifier who created or last modified this record. */
	@Column(name = "who", length = 100)
	private String who;

	// Calculated volume
	public double getVolume() {
		return length * breadth * height;
	}

	public double getEmptyWeight() {
		return emptyWeight;
	}

	public double getMaxWeight() {
		return maxWeight;
	}

	public String getName() {
		return name;
	}

}
