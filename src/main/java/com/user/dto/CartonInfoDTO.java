package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Carton (packaging box) info from the {@code carton} table, best-effort matched to the
 * shipment via its packed dimensions (length/breadth/height), since the shipment does not
 * persist a direct carton foreign key.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartonInfoDTO {

	private Long id;

	private String name;

	private Double length;

	private Double breadth;

	private Double height;

	private Double maxWeight;

	private Double emptyWeight;

	private String status;

}

