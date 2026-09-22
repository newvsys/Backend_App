package com.user.dto;

import lombok.Data;

@Data
public class ProductCreateDTO {

	private String name;

	private String description;

	private Integer categoryId;

	private String slug;

	private Integer priority;

	private String topFlag;

}