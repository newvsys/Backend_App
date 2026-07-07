package com.user.utility;

import com.user.dto.CategoryCreateDTO;
import com.user.dto.CategoryDTO;
import com.user.dto.InventoryDTO;
import com.user.dto.OrderDTO;
import com.user.dto.ProdVarCreateDTO;
import com.user.dto.ProdVarDTO;
import com.user.dto.ProductAttributeDTO;
import com.user.dto.ProductCreateDTO;
import com.user.dto.ProductDTO;
import com.user.dto.ProductImageDTO;
import com.user.dto.UserDto;
import com.user.model.InventoryEO;
import com.user.model.OrderEO;
import com.user.model.ProductAttributeEO;
import com.user.model.ProductCategoriesEO;
import com.user.model.ProductEO;
import com.user.model.ProductImageEO;
import com.user.model.ProductVariantEO;
import com.user.model.UserEO;

public final class UserMapper {

	private UserMapper() {
		// utility class
	}

	public static ProductVariantEO toProductVariantEO(ProdVarCreateDTO prodVarCreateDTO) {
		if (prodVarCreateDTO == null) {
			return null;
		}

		return ProductVariantEO.builder()
			.skuCode(prodVarCreateDTO.getSkuCode())
			.packSize(prodVarCreateDTO.getPackSize())
			.uom(prodVarCreateDTO.getUom())
			.containerType(prodVarCreateDTO.getContainerType())
			.mrp(prodVarCreateDTO.getMrp())
			.sellingPrice(prodVarCreateDTO.getSellingPrice())
			.status(prodVarCreateDTO.getStatus())
			.length(prodVarCreateDTO.getLength())
			.breadth(prodVarCreateDTO.getBreadth())
			.height(prodVarCreateDTO.getHeight())
			.weight(prodVarCreateDTO.getWeight())
			.build();
	}

	public static InventoryDTO toInventoryDTO(InventoryEO inventoryEO) {
		if (inventoryEO == null) {
			return null;
		}

		Integer productVarId = null;
		if (inventoryEO.getProductVariant() != null) {
			productVarId = inventoryEO.getProductVariant().getId();
		}

		return InventoryDTO.builder()
			.inventoryId(inventoryEO.getId())
			.productVarId(productVarId)
			.totalQty(inventoryEO.getTotalQty())
			.availableQty(inventoryEO.getAvailableQty())
			.whid(inventoryEO.getWhid())
			.build();
	}

	public static ProdVarDTO toProdVarDTO(ProductVariantEO productVariantEO) {
		if (productVariantEO == null) {
			return null;
		}

		Long productId = null;
		if (productVariantEO.getProduct() != null && productVariantEO.getProduct().getId() != null) {
			productId = productVariantEO.getProduct().getId().longValue();
		}

		return ProdVarDTO.builder()
			.variantId(productVariantEO.getId())
			.skuCode(productVariantEO.getSkuCode())
			.packSize(productVariantEO.getPackSize())
			.uom(productVariantEO.getUom())
			.containerType(productVariantEO.getContainerType())
			.mrp(productVariantEO.getMrp())
			.sellingPrice(productVariantEO.getSellingPrice())
			.status(productVariantEO.getStatus())
			.productId(productId)
			.length(productVariantEO.getLength())
			.breadth(productVariantEO.getBreadth())
			.height(productVariantEO.getHeight())
			.weight(java.math.BigDecimal.valueOf(productVariantEO.getWeight()))
			.build();
	}

	public static UserDto toDto(UserEO user) {
		if (user == null) {
			return null;
		}

		return UserDto.builder()
			.id(user.getId())
			.email(user.getEmail())
			.phone(user.getPhone())
			.firstName(user.getFirstName())
			// changed from getIsActive() to isActive()

			.phoneVerifiedAt(user.getPhoneVerifiedAt())
			.passwordHash(user.getPasswordHash())
			.createdAt(user.getCreatedAt())
			.updatedAt(user.getUpdatedAt())
			.role(user.getRole())
			.build();
	}

	public static UserEO fromDto(UserDto userDto) {
		if (userDto == null) {
			return null;
		}

		return UserEO.builder()
			.id(userDto.getId())
			.email(userDto.getEmail())
			.phone(userDto.getPhone())
			.firstName(userDto.getFirstName())

			.phoneVerifiedAt(userDto.getPhoneVerifiedAt())
			.createdAt(userDto.getCreatedAt())
			.updatedAt(userDto.getUpdatedAt())
			.role(userDto.getRole())
			.build();
	}

	public static ProductCategoriesEO toProductCategoriesEO(CategoryCreateDTO category) {
		if (category == null) {
			return null;
		}

		return ProductCategoriesEO.builder()
			.name(category.getName())
			.description(category.getDescription())
			.href("/shop/" + category.getName())
			.build();
	}

	public static CategoryDTO toCategoryDTO(ProductCategoriesEO category) {
		if (category == null) {
			return null;
		}

		return CategoryDTO.builder()
			.id(category.getId())
			.title(category.getName())
			.description(category.getDescription())
			.href(category.getHref())
			.src(category.getSrc())
			.build();
	}

	public static ProductEO toProductEO(ProductCreateDTO productCreateDTO) {
		if (productCreateDTO == null) {
			return null;
		}

		return ProductEO.builder()
			.name(productCreateDTO.getName())
			.description(productCreateDTO.getDescription())
			.slug(productCreateDTO.getSlug())
			.build();
	}

	public static ProductDTO toProductDTO(ProductEO productEO) {
		if (productEO == null) {
			return null;
		}

		return ProductDTO.builder()
			.title(productEO.getName())
			.description(productEO.getDescription())
			.slug(productEO.getSlug())
			.build();
	}

	public static ProductImageDTO toProductImageDTO(ProductImageEO productImageEO) {
		if (productImageEO == null) {
			return null;
		}

		Integer productId = null;
		String productName = null;
		if (productImageEO.getProductVar() != null && productImageEO.getProductVar().getProduct() != null) {
			productId = productImageEO.getProductVar().getProduct().getId();
			productName = productImageEO.getProductVar().getProduct().getName();
		}

		return ProductImageDTO.builder()
			.id(productImageEO.getId())
			.productID(productId != null ? String.valueOf(productId) : null)
			.productName(productName)
			.image(productImageEO.getImage())
			.imagePath(productImageEO.getImagePath())
			.isMainImage(productImageEO.getIsMainImage())
			.build();
	}

	public static ProductAttributeDTO toProductAttributeDTO(ProductAttributeEO entity) {
		if (entity == null) {
			return null;
		}

		Integer variantId = null;
		if (entity.getProductVar() != null) {
			variantId = entity.getProductVar().getId();
		}

		return ProductAttributeDTO.builder()
			.id(entity.getId())
			.variantId(variantId)
			.attributeName(entity.getAttributeName())
			.attributeValue(entity.getAttributeValue())
			.build();
	}

	public static OrderDTO toOrderDTO(OrderEO order) {
		if (order == null) {
			return null;
		}

		return OrderDTO.builder().orderId(order.getOrderId()).status(order.getOrderStatus()).build();
	}

}
