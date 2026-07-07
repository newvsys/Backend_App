package com.user.service;

import com.user.dto.*;
import com.user.model.*;
import com.user.repository.InventoryDetailsRepository;
import com.user.repository.InventoryRepository;
import com.user.repository.LabelConfigRepository;
import com.user.repository.LabelPrintJobRepository;
import com.user.repository.ProductAttributeRepository;
import com.user.repository.ProductCatRepository;
import com.user.repository.ProductImageRepository;
import com.user.repository.ProductRepository;
import com.user.repository.ProductVariantRepository;
import com.user.repository.UnitOfMeasureRepository;
import com.user.repository.WarehouseRepository;
import com.user.utility.Constants;
import com.user.utility.UserMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Service
public class ProductServiceImpl implements ProductService {

	private static final Logger logger = LogManager.getLogger(ProductServiceImpl.class);

	@Value("${category.image.upload-dir}")
	private String categoryImageUploadDir;

	@Value("${label.pdf.dir:/public/labels/}")
	private String labelPdfDir;

	@Autowired
	private ProductCatRepository productCatRepository;

	@Autowired
	private InventoryDetailsRepository inventoryDetailsRepository;

	@Autowired
	private ProductImageRepository productImageRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductVariantRepository productVariantRepository;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private ProductAttributeRepository productAttributeRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private UnitOfMeasureRepository unitOfMeasureRepository;

	@Autowired
	private LabelPrintJobRepository labelPrintJobRepository;

	@Autowired
	private LabelConfigRepository labelConfigRepository;

	@Autowired
	@org.springframework.context.annotation.Lazy
	private OrderService orderService;

	@Override
	@CacheEvict(value = "categories", allEntries = true)
	public CategoryDTO createProductCat(CategoryCreateDTO categoryCreateDTO, MultipartFile imageFile) {
		logger.info("Creating product category with data: {}", categoryCreateDTO);
		CategoryDTO categoryDTO = new CategoryDTO();
		try {
			ProductCategoriesEO productCategoriesEO = UserMapper.toProductCategoriesEO(categoryCreateDTO);

			if (imageFile != null && !imageFile.isEmpty()) {
				savePngToUploadDirAndSetSrc(productCategoriesEO, imageFile);
			}

			productCategoriesEO.setStatus("A");
			ProductCategoriesEO savedEntity = productCatRepository.save(productCategoriesEO);
			logger.info("Product category created with ID: {}", savedEntity.getId());
			categoryDTO = UserMapper.toCategoryDTO(savedEntity);
		}
		catch (Exception e) {
			logger.error("Error creating product category", e);

		}
		return categoryDTO;
	}

	@Override
	@CacheEvict(value = "categories", allEntries = true)
	public CategoryDTO updateProductCat(String catId, CategoryCreateDTO productCategory, MultipartFile imageFile) {
		ProductCategoriesEO existing = productCatRepository.findById(Long.parseLong(catId))
			.orElseThrow(() -> new RuntimeException("Category not found"));

		if (productCategory.getName() != null) {
			existing.setName(productCategory.getName());
			existing.setHref("/shop/" + productCategory.getName());
		}
		if (productCategory.getDescription() != null) {
			existing.setDescription(productCategory.getDescription());
		}

		if (imageFile != null && !imageFile.isEmpty()) {
			savePngToUploadDirAndSetSrc(existing, imageFile);
		}

		ProductCategoriesEO savedEntity = productCatRepository.save(existing);
		return UserMapper.toCategoryDTO(savedEntity);
	}

	@Override
	@CacheEvict(value = "categories", allEntries = true)
	public void deleteCategoriesById(String categoryId) {
		ProductCategoriesEO existing = productCatRepository.findById(Long.parseLong(categoryId))
			.orElseThrow(() -> new RuntimeException("Category not found"));
		existing.setStatus("I");
		productCatRepository.save(existing);
	}

	@Override
	@Cacheable("categories")
	@Transactional(readOnly = true)
	public List<CategoryDTO> getAllCategories() {
		try {
			List<CategoryDTO> result = productCatRepository.findActiveCategories();
			logger.info("getAllCategories: loaded {} categories from DB", result.size());
			return result;
		}
		catch (Exception e) {
			logger.error("Error fetching all categories", e);
			return new ArrayList<>();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public CategoryDTO getCategoryById(String categoryId) {
		CategoryDTO returnobj = null;
		try {
			ProductCategoriesEO category = productCatRepository.findById(Long.parseLong(categoryId)).orElse(null);
			returnobj = UserMapper.toCategoryDTO(category);
		}
		catch (Exception e) {
			logger.error("Error fetching category by id: {}", categoryId, e);

		}
		return returnobj;
	}

	@Override
	public ProductDTO createProduct(ProductCreateDTO productCreateDTO) {
		try {
			ProductEO productEO = UserMapper.toProductEO(productCreateDTO);
			ProductCategoriesEO category = productCatRepository.findById(productCreateDTO.getCategoryId().longValue())
				.orElseThrow(() -> new RuntimeException("Category not found"));
			productEO.setCategory(category);
			productEO.setStatus("A");
			ProductEO savedProduct = productRepository.save(productEO);
			return UserMapper.toProductDTO(savedProduct);
		}
		catch (Exception e) {
			logger.error("Error creating product: {}", e.getMessage(), e);
			return null;
		}
	}

	@Override
	public List<ProductDTO> getAllProducts() {
		List<ProductDTO> productDTOs = new ArrayList<>();
		try {
			logger.info("Fetching all active products");
			List<ProductEO> products = productRepository.findByStatus(Constants.STATUS_ACTIVE);
			if (products == null || products.isEmpty()) {
				return productDTOs;
			}
			for (ProductEO product : products) {
				ProductDTO productDTO = UserMapper.toProductDTO(product);
				productDTO.setId(product.getId());
				if (product.getCategory() != null) {
					productDTO.setCategory(product.getCategory().getName());
				}
				List<ProductVariantEO> variants = productVariantRepository.findByProduct(product);
				if (variants != null && !variants.isEmpty()) {
					ProductVariantEO lowestVariant = variants.stream()
						.filter(v -> v.getSellingPrice() != null)
						.min(java.util.Comparator.comparing(ProductVariantEO::getSellingPrice))
						.orElse(variants.get(0));
					productDTO.setPrice(lowestVariant.getSellingPrice());
					productDTO.setMrp(lowestVariant.getMrp());
					productDTO.setCurrency(Constants.PAYMENT_CURRENCY);
					productDTO.setSku(lowestVariant.getSkuCode());
					List<ProductImageEO> images = productImageRepository.findByProductVar(lowestVariant);
					if (images != null && !images.isEmpty()) {
						productDTO.setMainImage(resolveMainImage(images).getImage());
					}
					InventoryEO inventory = inventoryRepository.findByProductVariant(lowestVariant);
					if (inventory != null) {
						productDTO.setStock(inventory.getAvailableQty());
						productDTO
							.setInStock(inventory.getAvailableQty() != null && inventory.getAvailableQty() > 0 ? 1 : 0);
					}
				}
				productDTOs.add(productDTO);
			}
			logger.info("Fetched {} active products", productDTOs.size());
		}
		catch (Exception e) {
			logger.error("Error fetching all products: {}", e.getMessage(), e);
		}
		return productDTOs;
	}

	@Override
	public ProductDTO getProductById(Long productId) {
		ProductDTO productDTO = new ProductDTO();
		try {
			logger.info("Fetching product details for productId={}", productId);
			ProductEO product = productRepository.findByIdAndStatus(productId, Constants.STATUS_ACTIVE);
			if (product == null) {
				logger.warn("Product not found or inactive for productId={}", productId);
				return null;
			}
			productDTO = UserMapper.toProductDTO(product);
			productDTO.setId(product.getId());
			if (product.getCategory() != null) {
				productDTO.setCategory(product.getCategory().getName());
			}
			List<ProductVariantEO> variants = productVariantRepository.findByProduct(product);
			if (variants != null && !variants.isEmpty()) {
				ProductVariantEO lowestVariant = variants.stream()
					.filter(v -> v.getSellingPrice() != null)
					.min(java.util.Comparator.comparing(ProductVariantEO::getSellingPrice))
					.orElse(variants.get(0));
				productDTO.setPrice(lowestVariant.getSellingPrice());
				productDTO.setMrp(lowestVariant.getMrp());
				productDTO.setCurrency(Constants.PAYMENT_CURRENCY);
				productDTO.setSku(lowestVariant.getSkuCode());

				List<ProductImageEO> images = productImageRepository.findByProductVar(lowestVariant);
				if (images != null && !images.isEmpty()) {
					productDTO.setMainImage(resolveMainImage(images).getImage());
				}

				List<ProductAttributeEO> attributes = productAttributeRepository.findByProductVar(lowestVariant);
				if (attributes != null && !attributes.isEmpty()) {
					List<ProductAttributeDTO> attributeDTOs = attributes.stream().map(attr -> {
						ProductAttributeDTO dto = new ProductAttributeDTO();
						dto.setId(attr.getId());
						dto.setVariantId(attr.getProductVar().getId());
						dto.setAttributeName(attr.getAttributeName());
						dto.setAttributeValue(attr.getAttributeValue());
						return dto;
					}).collect(Collectors.toList());
					productDTO.setAttributes(attributeDTOs);
				}

				InventoryEO inventory = inventoryRepository.findByProductVariant(lowestVariant);
				if (inventory != null) {
					productDTO.setStock(inventory.getAvailableQty());
					productDTO
						.setInStock(inventory.getAvailableQty() != null && inventory.getAvailableQty() > 0 ? 1 : 0);
				}

				ReturnPolicyDetailDTO returnPolicy = orderService
					.getReturnPolicyByProductVariantId(lowestVariant.getId().longValue());
				if (returnPolicy != null) {
					productDTO.setReturnPolicy(returnPolicy);
					productDTO.setIsReturnable(returnPolicy.getIsReturnable() != null
							? (returnPolicy.getIsReturnable() ? "Y" : "N") : "N");
				}
				else {
					productDTO.setIsReturnable("N");
				}
			}
			logger.info("Fetched product details for productId={}", productId);
		}
		catch (Exception e) {
			logger.error("Error fetching product details for productId={}: {}", productId, e.getMessage(), e);
		}
		return productDTO;
	}

	@Override
	public ProductDTO updateProduct(Integer productId, ProductCreateDTO productUpdateDTO) {
		try {
			ProductEO product = productRepository.findById(productId.longValue())
				.orElseThrow(() -> new RuntimeException("Product not found"));

			if (productUpdateDTO.getName() != null) {
				product.setName(productUpdateDTO.getName());
			}
			if (productUpdateDTO.getDescription() != null) {
				product.setDescription(productUpdateDTO.getDescription());
			}
			if (productUpdateDTO.getSlug() != null) {
				product.setSlug(productUpdateDTO.getSlug());
			}

			ProductEO updatedProduct = productRepository.save(product);
			ProductDTO productDTO = UserMapper.toProductDTO(updatedProduct);
			productDTO.setId(product.getId());
			return productDTO;
		}
		catch (Exception e) {
			logger.error("Error updating product: {}", e.getMessage(), e);
			return null;
		}
	}

	@Override
	public void deleteProductById(Integer productId) {
		ProductEO existing = productRepository.findById(productId.longValue())
			.orElseThrow(() -> new RuntimeException("Product not found"));
		existing.setStatus("I");
		productRepository.save(existing);
	}

	@Override
	public ProdVarDTO createProductVariant(ProdVarCreateDTO prodVarCreateDTO, List<MultipartFile> images,
			MultipartFile video, int mainImageIndex) {
		try {
			ProductVariantEO productVarEO = UserMapper.toProductVariantEO(prodVarCreateDTO);
			ProductEO product = productRepository.findById(prodVarCreateDTO.getProductId().longValue()).orElse(null);
			productVarEO.setProduct(product);
			productVarEO.setStatus("A");

			// Handle video upload before saving entity
			if (video != null && !video.isEmpty()) {
				String videoFilename = video.getOriginalFilename();
				if (videoFilename == null || (!videoFilename.toLowerCase().endsWith(".mp4")
						&& !videoFilename.toLowerCase().endsWith(".mov")
						&& !videoFilename.toLowerCase().endsWith(".avi")
						&& !videoFilename.toLowerCase().endsWith(".webm"))) {
					throw new IllegalArgumentException("Only MP4, MOV, AVI, WEBM video files are allowed.");
				}
				String uploadDir = normalizedUploadDir(categoryImageUploadDir);
				File dir = new File(uploadDir);
				if (!dir.exists()) {
					dir.mkdirs();
				}
				String timestamp = String.valueOf(System.currentTimeMillis());
				String filename = timestamp + "_" + videoFilename;
				video.transferTo(new File(uploadDir + filename));
				productVarEO.setVideoUrl(filename);
			}

			ProductVariantEO savedEntity = productVariantRepository.save(productVarEO);
			List<ProductImageDTO> productImageDTOs = new ArrayList<>();
			if (images != null && !images.isEmpty()) {
				// Clamp mainImageIndex to valid range
				int effectiveMainIndex = (mainImageIndex >= 0 && mainImageIndex < images.size()) ? mainImageIndex : 0;
				int imageCounter = 0;
				for (MultipartFile image : images) {
					if (image == null || image.isEmpty()) {
						imageCounter++;
						continue;
					}

					String originalFilename = image.getOriginalFilename();
					if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".png")) {
						throw new IllegalArgumentException("Only PNG images are allowed.");
					}
					String uploadDir = normalizedUploadDir(categoryImageUploadDir);
					File dir = new File(uploadDir);
					if (!dir.exists()) {
						dir.mkdirs();
					}
					String timestamp = String.valueOf(System.currentTimeMillis());
					String filename = timestamp + "_" + originalFilename;
					String filePath = uploadDir + filename;
					image.transferTo(new File(filePath));
					ProductImageEO productImageEO = new ProductImageEO();
					productImageEO.setProductVar(savedEntity);
					productImageEO.setImagePath(filePath);
					productImageEO.setImage(filename);
					productImageEO.setIsMainImage(imageCounter == effectiveMainIndex ? "Y" : "N");
					imageCounter++;
					productImageEO = productImageRepository.save(productImageEO);
					productImageDTOs.add(UserMapper.toProductImageDTO(productImageEO));

				}
			}
			ProdVarDTO dto = UserMapper.toProdVarDTO(savedEntity);
			dto.setProductImages(productImageDTOs);
			dto.setVideoUrl(savedEntity.getVideoUrl());
			return dto;
		}
		catch (Exception e) {
			logger.error("Error creating product variant: {}", e.getMessage(), e);
			return null;
		}
	}

	@Override
	public ProdVarDTO updateProductVariant(ProdVarUpdateDTO prodVarUpdateDTO, List<MultipartFile> images,
			MultipartFile video, int mainImageIndex) {
		try {
			ProductVariantEO existingVariant = productVariantRepository.findById(prodVarUpdateDTO.getVariantId())
				.orElse(null);
			if (existingVariant == null) {
				logger.error("Product Variant not found for id: {}", prodVarUpdateDTO.getVariantId());
				return null;
			}
			if (prodVarUpdateDTO.getSkuCode() != null) {
				existingVariant.setSkuCode(prodVarUpdateDTO.getSkuCode());
			}
			if (prodVarUpdateDTO.getPackSize() != null) {
				existingVariant.setPackSize(prodVarUpdateDTO.getPackSize());
			}
			if (prodVarUpdateDTO.getUom() != null) {
				existingVariant.setUom(prodVarUpdateDTO.getUom());
			}
			if (prodVarUpdateDTO.getContainerType() != null) {
				existingVariant.setContainerType(prodVarUpdateDTO.getContainerType());
			}
			if (prodVarUpdateDTO.getMrp() != null) {
				existingVariant.setMrp(prodVarUpdateDTO.getMrp());
			}
			if (prodVarUpdateDTO.getSellingPrice() != null) {
				existingVariant.setSellingPrice(prodVarUpdateDTO.getSellingPrice());
			}
			if (prodVarUpdateDTO.getStatus() != null) {
				existingVariant.setStatus(prodVarUpdateDTO.getStatus());
			}
			if (prodVarUpdateDTO.getLength() != null) {
				existingVariant.setLength(prodVarUpdateDTO.getLength());
			}
			if (prodVarUpdateDTO.getBreadth() != null) {
				existingVariant.setBreadth(prodVarUpdateDTO.getBreadth());
			}
			if (prodVarUpdateDTO.getHeight() != null) {
				existingVariant.setHeight(prodVarUpdateDTO.getHeight());
			}
			if (prodVarUpdateDTO.getWeight() != null) {
				existingVariant.setWeight(prodVarUpdateDTO.getWeight());
			}

			// Handle video upload — if a new video is provided, delete old one and
			// replace
			if (video != null && !video.isEmpty()) {
				String videoFilename = video.getOriginalFilename();
				if (videoFilename == null || (!videoFilename.toLowerCase().endsWith(".mp4")
						&& !videoFilename.toLowerCase().endsWith(".mov")
						&& !videoFilename.toLowerCase().endsWith(".avi")
						&& !videoFilename.toLowerCase().endsWith(".webm"))) {
					throw new IllegalArgumentException("Only MP4, MOV, AVI, WEBM video files are allowed.");
				}
				String uploadDir = normalizedUploadDir(categoryImageUploadDir);
				new File(uploadDir).mkdirs();
				// Delete the existing video file from filesystem before saving new one
				if (existingVariant.getVideoUrl() != null && !existingVariant.getVideoUrl().isEmpty()) {
					File oldFile = new File(uploadDir + existingVariant.getVideoUrl());
					if (oldFile.exists()) {
						oldFile.delete();
						logger.info("Deleted old video file: {}", oldFile.getAbsolutePath());
					}
				}
				String filename = System.currentTimeMillis() + "_" + videoFilename;
				video.transferTo(new File(uploadDir + filename));
				existingVariant.setVideoUrl(filename);
			}

			ProductVariantEO savedVariant = productVariantRepository.save(existingVariant);

			// Handle new image uploads — append to existing images
			if (images != null && !images.isEmpty()) {
				int effectiveMainIndex = (mainImageIndex >= 0 && mainImageIndex < images.size()) ? mainImageIndex : 0;
				// Reset isMainImage on all existing images so the new main takes over
				List<ProductImageEO> existingImages = productImageRepository.findByProductVar(savedVariant);
				if (existingImages != null && !existingImages.isEmpty()) {
					for (ProductImageEO img : existingImages) {
						img.setIsMainImage("N");
					}
					productImageRepository.saveAll(existingImages);
				}
				String uploadDir = normalizedUploadDir(categoryImageUploadDir);
				new File(uploadDir).mkdirs();
				int imageCounter = 0;
				for (MultipartFile image : images) {
					if (image == null || image.isEmpty()) {
						imageCounter++;
						continue;
					}
					String originalFilename = image.getOriginalFilename();
					if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".png")) {
						throw new IllegalArgumentException("Only PNG images are allowed.");
					}
					String filename = System.currentTimeMillis() + "_" + originalFilename;
					String filePath = uploadDir + filename;
					image.transferTo(new File(filePath));
					ProductImageEO productImageEO = new ProductImageEO();
					productImageEO.setProductVar(savedVariant);
					productImageEO.setImagePath(filePath);
					productImageEO.setImage(filename);
					productImageEO.setIsMainImage(imageCounter == effectiveMainIndex ? "Y" : "N");
					imageCounter++;
					productImageRepository.save(productImageEO);
				}
			}

			// Build full response DTO — fetch all images (existing + newly added)
			ProdVarDTO dto = UserMapper.toProdVarDTO(savedVariant);
			dto.setVideoUrl(savedVariant.getVideoUrl());

			List<ProductImageEO> allImages = productImageRepository.findByProductVar(savedVariant);
			if (allImages != null && !allImages.isEmpty()) {
				resolveMainImage(allImages); // ensure one is marked as main
				dto.setProductImages(
						allImages.stream().map(UserMapper::toProductImageDTO).collect(Collectors.toList()));
			}

			List<ProductAttributeEO> attributes = productAttributeRepository.findByProductVar(savedVariant);
			if (attributes != null && !attributes.isEmpty()) {
				List<ProductAttributeDTO> attrDTOs = attributes.stream().map(attr -> {
					ProductAttributeDTO attrDto = new ProductAttributeDTO();
					attrDto.setId(attr.getId());
					attrDto.setVariantId(attr.getProductVar().getId());
					attrDto.setAttributeName(attr.getAttributeName());
					attrDto.setAttributeValue(attr.getAttributeValue());
					return attrDto;
				}).collect(Collectors.toList());
				dto.setAttributes(attrDTOs);
			}

			return dto;
		}
		catch (Exception e) {
			logger.error("Error updating product variant: {}", e.getMessage(), e);
			return null;
		}
	}

	@Override
	public void deleteProductVariantById(String variantId) {
		ProductVariantEO existing = productVariantRepository.findById(Long.parseLong(variantId))
			.orElseThrow(() -> new RuntimeException("Product Variant not found"));
		existing.setStatus("I");
		productVariantRepository.save(existing);
	}

	@Override
	public List<ProdVarDTO> getVariantsByProductId(Long productId) {
		List<ProdVarDTO> variantDTOs = new ArrayList<>();
		try {
			logger.info("Fetching product variants for productId={}", productId);
			ProductEO product = productRepository.findById(productId).orElse(null);
			if (product == null) {
				logger.warn("Product not found for productId={}", productId);
				return variantDTOs;
			}
			List<ProductVariantEO> variants = productVariantRepository.findByProduct(product);
			if (variants == null || variants.isEmpty()) {
				return variantDTOs;
			}
			for (ProductVariantEO variant : variants) {
				ProdVarDTO dto = UserMapper.toProdVarDTO(variant);
				dto.setVideoUrl(variant.getVideoUrl());
				// attach images
				List<ProductImageEO> images = productImageRepository.findByProductVar(variant);
				if (images != null && !images.isEmpty()) {
					resolveMainImage(images); // ensure one image is marked as main
					List<ProductImageDTO> imageDTOs = images.stream()
						.map(UserMapper::toProductImageDTO)
						.collect(Collectors.toList());
					dto.setProductImages(imageDTOs);
				}
				// attach attributes
				List<ProductAttributeEO> attributes = productAttributeRepository.findByProductVar(variant);
				if (attributes != null && !attributes.isEmpty()) {
					List<ProductAttributeDTO> attrDTOs = attributes.stream().map(attr -> {
						ProductAttributeDTO attrDto = new ProductAttributeDTO();
						attrDto.setId(attr.getId());
						attrDto.setVariantId(attr.getProductVar().getId());
						attrDto.setAttributeName(attr.getAttributeName());
						attrDto.setAttributeValue(attr.getAttributeValue());
						return attrDto;
					}).collect(Collectors.toList());
					dto.setAttributes(attrDTOs);
				}
				variantDTOs.add(dto);
			}
			logger.info("Fetched {} variants for productId={}", variantDTOs.size(), productId);
		}
		catch (Exception e) {
			logger.error("Error fetching variants for productId={}: {}", productId, e.getMessage(), e);
		}
		return variantDTOs;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductDTO> searchProduct(String query, List<Long> categoryIds, Boolean inStock, Integer minPrice,
			Integer price, String sort, int page, String rsc, int limit) {
		logger.debug(
				"searchProduct: query={}, categoryIds={}, inStock={}, minPrice={}, price={}, sort={}, page={}, limit={}",
				query, categoryIds, inStock, minPrice, price, sort, page, limit);
		List<ProductDTO> productDTOs = new ArrayList<>();
		try {
			// ── Step 1: resolve category / query flags ──────────────────────────────────
			boolean hasCategoryIds = categoryIds != null && !categoryIds.isEmpty();
			boolean isAllCategories = hasCategoryIds && categoryIds.size() == 1
					&& Long.valueOf(0L).equals(categoryIds.get(0));
			if (isAllCategories)
				hasCategoryIds = false;
			boolean hasQuery = query != null && !query.isBlank();
			String queryPattern = hasQuery ? "%" + query.toLowerCase().trim() + "%" : "%%";
			String stemPattern = hasQuery ? "%" + buildStem(query) + "%" : "%%";

			// ── Step 2: fetch matching products (1–2 queries) ───────────────────────────
			List<ProductEO> products;
			if (hasCategoryIds && hasQuery) {
				// Combined filter: products in the given categories whose name/description matches
				products = productRepository.findByCategoryIdsAndNameOrDescriptionAndStatus(categoryIds, queryPattern,
						stemPattern, Constants.STATUS_ACTIVE);
				// No fallback — if the keyword isn't in those category's products, return empty
			}
			else if (hasCategoryIds) {
				products = productRepository.findByCategoryIdsAndStatus(categoryIds, Constants.STATUS_ACTIVE);
			}
			else if (hasQuery) {
				products = productRepository.findByNameOrDescriptionAndStatus(queryPattern, stemPattern,
						Constants.STATUS_ACTIVE);
				// Fallback: search category name/description
				if (products == null || products.isEmpty()) {
					logger.debug(
							"No products found by name/description for query='{}', falling back to category search",
							query);
					List<ProductCategoriesEO> matchingCategories = productCatRepository
						.findByNameOrDescriptionAndStatus(query, Constants.STATUS_ACTIVE);
					if (matchingCategories != null && !matchingCategories.isEmpty()) {
						products = new ArrayList<>();
						for (ProductCategoriesEO category : matchingCategories) {
							List<ProductEO> categoryProducts = productRepository.findByCategoryAndStatus(category,
									Constants.STATUS_ACTIVE);
							if (categoryProducts != null)
								products.addAll(categoryProducts);
						}
					}
				}
			}
			else if (isAllCategories) {
				logger.debug("categoryId=0 received — fetching all active products (max 500)");
				products = productRepository.findByStatus(Constants.STATUS_ACTIVE);
			}
			else {
				return productDTOs;
			}

			if (products == null || products.isEmpty())
				return productDTOs;

			// Cap all-categories mode at 500
			if (isAllCategories && products.size() > 500)
				products = products.subList(0, 500);

			// ── Step 3: deduplicate by product ID ──────────────────────────────────────
			// HashSet gives O(1) add/contains vs LinkedHashMap O(log n); insertion order
			// is preserved by the ArrayList, so result order is identical.
			Set<Integer> seenIds = new HashSet<>(products.size() * 2);
			List<ProductEO> uniqueProducts = new ArrayList<>(products.size());
			for (ProductEO p : products) {
				if (seenIds.add(p.getId()))
					uniqueProducts.add(p);
			}

			// ── Step 4: batch-fetch all variants for all products (1 query) ─────────────
			List<ProductVariantEO> allVariants;
			if (minPrice != null && price != null) {
				allVariants = productVariantRepository.findByProductInAndPriceRangeAndStatus(uniqueProducts,
						minPrice.doubleValue(), price.doubleValue(), Constants.STATUS_ACTIVE);
			}
			else if (price != null) {
				allVariants = productVariantRepository.findByProductInAndSellingPriceLessThanEqualAndStatus(
						uniqueProducts, price.doubleValue(), Constants.STATUS_ACTIVE);
			}
			else {
				allVariants = productVariantRepository.findByProductInAndStatus(uniqueProducts,
						Constants.STATUS_ACTIVE);
			}
			if (allVariants == null || allVariants.isEmpty())
				return productDTOs;

			// Group variants by product ID using a manual loop — avoids stream overhead
			// and the LinkedHashMap factory. Variants arrive price-ASC from the DB, so the
			// per-product list retains that order automatically.
			Map<Integer, List<ProductVariantEO>> variantsByProductId = new HashMap<>(allVariants.size() * 2);
			for (ProductVariantEO v : allVariants) {
				variantsByProductId.computeIfAbsent(v.getProduct().getId(), k -> new ArrayList<>()).add(v);
			}

			// ── Step 5: batch-fetch inventory (1 query) — done BEFORE images so we can ──
			// identify the one chosen variant per product and load images only for those. ─
			List<Long> allVariantIds = new ArrayList<>(allVariants.size());
			for (ProductVariantEO v : allVariants)
				allVariantIds.add(v.getId().longValue());

			List<InventoryEO> allInventory = Boolean.TRUE.equals(inStock)
					? inventoryRepository.findAllByProductVariantIdInAndInStock(allVariantIds)
					: inventoryRepository.findAllByProductVariantIdIn(allVariantIds);

			// Manual loop is faster than stream collect(toMap) for large lists
			Map<Long, InventoryEO> inventoryByVariantId = new HashMap<>(allInventory.size() * 2);
			for (InventoryEO inv : allInventory)
				inventoryByVariantId.putIfAbsent(inv.getProductVariant().getId().longValue(), inv);

			// ── Step 6: pre-select one variant per product (cheapest with inventory) ─────
			// This lets us load images only for the winning variant, not every variant.
			Map<Integer, ProductVariantEO> chosenVariantByProductId = new HashMap<>(uniqueProducts.size() * 2);
			for (ProductEO product : uniqueProducts) {
				List<ProductVariantEO> variants = variantsByProductId.get(product.getId());
				if (variants == null)
					continue;
				for (ProductVariantEO v : variants) {
					if (inventoryByVariantId.containsKey(v.getId().longValue())) {
						chosenVariantByProductId.put(product.getId(), v);
						break; // variants sorted price ASC — first match = cheapest in-stock
					}
				}
			}

			// ── Step 7: batch-fetch images — only for chosen variants (1 per product) ───
			// Loading images for all variants would pull far more rows; we only ever use
			// the main image for the single displayed variant.
			Collection<ProductVariantEO> chosenVariants = chosenVariantByProductId.values();
			Map<Integer, List<ProductImageEO>> imagesByVariantId;
			if (!chosenVariants.isEmpty()) {
				List<ProductImageEO> allImages = productImageRepository.findByProductVarIn(chosenVariants);
				if (allImages != null && !allImages.isEmpty()) {
					imagesByVariantId = new HashMap<>(allImages.size() * 2);
					for (ProductImageEO img : allImages)
						imagesByVariantId.computeIfAbsent(img.getProductVar().getId(), k -> new ArrayList<>()).add(img);
				}
				else {
					imagesByVariantId = Collections.emptyMap();
				}
			}
			else {
				imagesByVariantId = Collections.emptyMap();
			}

			// ── Step 8: build DTOs — 0 additional DB calls ──────────────────────────────
			productDTOs = new ArrayList<>(Math.min(uniqueProducts.size(), Math.max(limit, 1)));
			for (ProductEO product : uniqueProducts) {
				ProductVariantEO chosenVariant = chosenVariantByProductId.get(product.getId());
				if (chosenVariant == null)
					continue; // no variant with a matching inventory record

				// Inventory is guaranteed non-null: chosenVariant was selected because it
				// has an entry in inventoryByVariantId.
				InventoryEO inventory = inventoryByVariantId.get(chosenVariant.getId().longValue());

				ProductDTO productDTO = UserMapper.toProductDTO(product);
				productDTO.setId(product.getId());
				productDTO.setIsReturnable("N"); // detail page fetches the real return policy

				List<ProductImageEO> images = imagesByVariantId.get(chosenVariant.getId());
				if (images != null && !images.isEmpty()) {
					productDTO.setMainImage(resolveMainImage(images).getImage());
				}
				productDTO.setPrice(chosenVariant.getSellingPrice());
				productDTO.setMrp(chosenVariant.getMrp());
				productDTO.setCurrency("INR");
				productDTO.setSku(chosenVariant.getSkuCode());
				productDTO.setStock(inventory.getAvailableQty());
				productDTO.setInStock(
						inventory.getAvailableQty() != null && inventory.getAvailableQty() > 0 ? 1 : 0);
				productDTOs.add(productDTO);
			}

			// ── Step 9: sort ────────────────────────────────────────────────────────────
			if ("lowPrice".equals(sort)) {
				productDTOs.sort(Comparator.comparing(ProductDTO::getPrice,
						Comparator.nullsLast(Comparator.naturalOrder())));
			}
			else if ("highPrice".equals(sort)) {
				productDTOs.sort(Comparator.comparing(ProductDTO::getPrice,
						Comparator.nullsLast(Comparator.reverseOrder())));
			}

			// ── Step 10: paginate ────────────────────────────────────────────────────────
			if (isAllCategories) {
				logger.debug("All-categories mode: returning up to 500 products, total built: {}",
						productDTOs.size());
				return productDTOs.size() > 500 ? productDTOs.subList(0, 500) : productDTOs;
			}
			int fromIndex = (page - 1) * limit;
			if (fromIndex >= productDTOs.size())
				return Collections.emptyList();
			return productDTOs.subList(fromIndex, Math.min(fromIndex + limit, productDTOs.size()));
		}
		catch (Exception e) {
			logger.error("Error in searchProduct: ", e);
			return productDTOs;
		}
	}

	@Override
	public ProductDTO findBySlug(String productSlug) {
		ProductDTO productDTO = null;
		try {
			logger.info("Finding product by slug: {}", productSlug);
			ProductEO product = productRepository.findBySlug(productSlug);
			List<ProductVariantEO> existingVariants = null;
			if (product != null) {
				existingVariants = productVariantRepository.findByProduct(product);
			}

			if (product != null && existingVariants != null) {

				productDTO = UserMapper.toProductDTO(product);

				// ProductVariantEO existingVariant = existingVariants.get(0);
				ProductVariantEO existingVariant = existingVariants.stream()
					.filter(v -> v.getSellingPrice() != null)
					.min((v1, v2) -> v1.getSellingPrice().compareTo(v2.getSellingPrice()))
					.orElse(existingVariants.get(0));
				productDTO.setId(existingVariant.getId());
				productDTO.setProductId(product.getId());
				List<ProductImageEO> productImages = productImageRepository.findByProductVar(existingVariant);
				List<ProductAttributeEO> attributes = productAttributeRepository.findByProductVar(existingVariant);
				if (attributes != null && attributes.size() > 0) {
					List<ProductAttributeDTO> attributeDTOs = attributes.stream().map(attr -> {
						ProductAttributeDTO dto = new ProductAttributeDTO();
						dto.setId(attr.getId());
						dto.setVariantId(attr.getProductVar().getId());
						dto.setAttributeName(attr.getAttributeName());
						dto.setAttributeValue(attr.getAttributeValue());
						return dto;
					}).collect(Collectors.toList());

					productDTO.setAttributes(attributeDTOs);
				}

				if (productImages != null && !productImages.isEmpty()) {
					productDTO.setMainImage(resolveMainImage(productImages).getImage());
				}

				productDTO.setPrice(existingVariant.getSellingPrice());
				productDTO.setMrp(existingVariant.getMrp());
				productDTO.setCurrency(Constants.PAYMENT_CURRENCY);
				productDTO.setSku(existingVariant.getSkuCode());
				productDTO.setVideoUrl(existingVariant.getVideoUrl());

				InventoryEO inventory = inventoryRepository.findByProductVariant(existingVariant);
				if (inventory != null) {
					productDTO.setStock(inventory.getAvailableQty());
					productDTO
						.setInStock(inventory.getAvailableQty() != null && inventory.getAvailableQty() > 0 ? 1 : 0);
				}

				ReturnPolicyDetailDTO returnPolicy = orderService
					.getReturnPolicyByProductVariantId(existingVariant.getId().longValue());
				if (returnPolicy != null) {
					productDTO.setReturnPolicy(returnPolicy);
					productDTO.setIsReturnable(returnPolicy.getIsReturnable() != null
							? (returnPolicy.getIsReturnable() ? "Y" : "N") : "N");
				}
				else {
					productDTO.setIsReturnable("N");
				}

				// add other product var also
				List<ProductDTO> productVarList = new ArrayList<>();
				for (ProductVariantEO variant : existingVariants) {
					if (variant.getId().equals(existingVariant.getId())) {
						continue; // skip the main variant already used
					}
					ProductDTO varDTO = UserMapper.toProductDTO(product);
					// Set variant-specific fields
					varDTO.setPrice(variant.getSellingPrice());
					varDTO.setMrp(variant.getMrp());
					varDTO.setSku(variant.getSkuCode());
					varDTO.setCurrency(Constants.PAYMENT_CURRENCY);
					varDTO.setId(variant.getId());
					varDTO.setVideoUrl(variant.getVideoUrl());
					varDTO.setProductId(product.getId());
					// Set main image for this variant
					List<ProductImageEO> varImages = productImageRepository.findByProductVar(variant);
					if (varImages != null && !varImages.isEmpty()) {
						varDTO.setMainImage(resolveMainImage(varImages).getImage());
					}
					// Set attributes for this variant
					List<ProductAttributeEO> varAttributes = productAttributeRepository.findByProductVar(variant);
					if (varAttributes != null && !varAttributes.isEmpty()) {
						List<ProductAttributeDTO> varAttributeDTOs = varAttributes.stream().map(attr -> {
							ProductAttributeDTO dto = new ProductAttributeDTO();
							dto.setId(attr.getId());
							dto.setVariantId(attr.getProductVar().getId());
							dto.setAttributeName(attr.getAttributeName());
							dto.setAttributeValue(attr.getAttributeValue());
							return dto;
						}).collect(Collectors.toList());
						varDTO.setAttributes(varAttributeDTOs);
					}
					// Set stock and inStock for this variant
					InventoryEO varInventory = inventoryRepository.findByProductVariant(variant);
					if (varInventory != null) {
						varDTO.setStock(varInventory.getAvailableQty());
						varDTO.setInStock(
								varInventory.getAvailableQty() != null && varInventory.getAvailableQty() > 0 ? 1 : 0);
					}
					// Set return policy for this variant
					ReturnPolicyDetailDTO varReturnPolicy = orderService
						.getReturnPolicyByProductVariantId(variant.getId().longValue());
					if (varReturnPolicy != null) {
						varDTO.setReturnPolicy(varReturnPolicy);
						varDTO.setIsReturnable(varReturnPolicy.getIsReturnable() != null
								? (varReturnPolicy.getIsReturnable() ? "Y" : "N") : "N");
					}
					else {
						varDTO.setIsReturnable("N");
					}
					productVarList.add(varDTO);
				}
				productDTO.setProductvarlist(productVarList);

			}

			logger.info("Product found by slug: {}, ProductDTO: {}", productSlug, productDTO);

		}
		catch (Exception e) {
			logger.error("Error in findBySlug for slug: {}: {}", productSlug, e.getMessage(), e);

		}
		return productDTO;
	}

	@Override
	@Transactional
	public List<ProductImageDTO> findProductImageByProductId(String productId) {
		ProductVariantEO existingVariant = productVariantRepository.findById(Long.parseLong(productId))
			.orElseThrow(() -> new RuntimeException("Product Variant not found for id: " + productId));

		List<ProductImageEO> productImages = productImageRepository.findByProductVar(existingVariant);

		return productImages.stream().map(UserMapper::toProductImageDTO).collect(Collectors.toList());
	}

	@Override
	@Transactional
	public ResponseDTO deleteProductImage(Long imageId) {
		ResponseDTO result = new ResponseDTO();
		try {
			ProductImageEO image = productImageRepository.findById(imageId).orElse(null);
			if (image == null) {
				result.setResponseStatus(Constants.FAILURE_STATUS);
				result.setResponseMessage("Image not found with ID: " + imageId);
				return result;
			}
			boolean wasMain = "Y".equals(image.getIsMainImage());
			ProductVariantEO variant = image.getProductVar();
			productImageRepository.delete(image);
			// If the deleted image was the main image, promote the first remaining image
			if (wasMain) {
				List<ProductImageEO> remaining = productImageRepository.findByProductVar(variant);
				if (remaining != null && !remaining.isEmpty()) {
					ProductImageEO newMain = remaining.get(0);
					newMain.setIsMainImage("Y");
					productImageRepository.save(newMain);
				}
			}
			result.setResponseStatus(Constants.SUCCESS_STATUS);
			result.setResponseMessage("Image deleted successfully");
		}
		catch (Exception e) {
			logger.error("Error deleting image {}: {}", imageId, e.getMessage(), e);
			result.setResponseStatus(Constants.FAILURE_STATUS);
			result.setResponseMessage("Failed to delete image: " + e.getMessage());
		}
		return result;
	}

	@Override
	public ResponseDTO deleteProductVideo(Long variantId) {
		ResponseDTO result = new ResponseDTO();
		try {
			ProductVariantEO variant = productVariantRepository.findById(variantId).orElse(null);
			if (variant == null) {
				result.setResponseStatus(Constants.FAILURE_STATUS);
				result.setResponseMessage("Product variant not found with ID: " + variantId);
				return result;
			}
			if (variant.getVideoUrl() == null || variant.getVideoUrl().isEmpty()) {
				result.setResponseStatus(Constants.FAILURE_STATUS);
				result.setResponseMessage("No video exists for variant ID: " + variantId);
				return result;
			}
			// Delete the video file from the filesystem
			String uploadDir = normalizedUploadDir(categoryImageUploadDir);
			File videoFile = new File(uploadDir + variant.getVideoUrl());
			if (videoFile.exists()) {
				videoFile.delete();
				logger.info("Deleted video file: {}", videoFile.getAbsolutePath());
			}
			// Clear the videoUrl in DB
			variant.setVideoUrl(null);
			productVariantRepository.save(variant);
			result.setResponseStatus(Constants.SUCCESS_STATUS);
			result.setResponseMessage("Video deleted successfully");
		}
		catch (Exception e) {
			logger.error("Error deleting video for variant {}: {}", variantId, e.getMessage(), e);
			result.setResponseStatus(Constants.FAILURE_STATUS);
			result.setResponseMessage("Failed to delete video: " + e.getMessage());
		}
		return result;
	}

	@Override
	@Transactional
	public ResponseDTO setMainImage(Long imageId) {
		ResponseDTO result = new ResponseDTO();
		try {
			ProductImageEO targetImage = productImageRepository.findById(imageId).orElse(null);
			if (targetImage == null) {
				result.setResponseStatus(Constants.FAILURE_STATUS);
				result.setResponseMessage("Image not found with ID: " + imageId);
				return result;
			}
			// Unset isMainImage for all other images of the same variant
			List<ProductImageEO> allImages = productImageRepository.findByProductVar(targetImage.getProductVar());
			for (ProductImageEO img : allImages) {
				img.setIsMainImage("N");
			}
			productImageRepository.saveAll(allImages);
			// Set the target image as main
			targetImage.setIsMainImage("Y");
			productImageRepository.save(targetImage);
			result.setResponseStatus(Constants.SUCCESS_STATUS);
			result.setResponseMessage("Main image updated successfully");
		}
		catch (Exception e) {
			logger.error("Error setting main image for imageId={}: {}", imageId, e.getMessage(), e);
			result.setResponseStatus(Constants.FAILURE_STATUS);
			result.setResponseMessage("Failed to set main image: " + e.getMessage());
		}
		return result;
	}

	@Override
	public ProductAttributeDTO createProductAttribute(ProductAttributeCreateDTO productAttributeCreateDTO) {
		ProductAttributeEO productAttributeEO = new ProductAttributeEO();

		ProductVariantEO existingVariant = productVariantRepository.findById(productAttributeCreateDTO.getVariantId())
			.orElseThrow(() -> new RuntimeException("Product Variant not found"));

		productAttributeEO.setProductVar(existingVariant);
		productAttributeEO.setAttributeName(productAttributeCreateDTO.getAttributeName());
		productAttributeEO.setAttributeValue(productAttributeCreateDTO.getAttributeValue());

		ProductAttributeEO savedEntity = productAttributeRepository.save(productAttributeEO);

		ProductAttributeDTO productAttributeDTO = new ProductAttributeDTO();
		productAttributeDTO.setId(savedEntity.getId());
		productAttributeDTO.setVariantId(savedEntity.getProductVar().getId());
		productAttributeDTO.setAttributeName(savedEntity.getAttributeName());
		productAttributeDTO.setAttributeValue(savedEntity.getAttributeValue());

		return productAttributeDTO;
	}

	@Override
	public ProductAttributeDTO updateProductAttribute(Long attributeId,
			ProductAttributeCreateDTO productAttributeCreateDTO) {
		Optional<ProductAttributeEO> optionalAttribute = productAttributeRepository.findById(attributeId);

		if (optionalAttribute.isEmpty()) {
			throw new RuntimeException("Product Attribute not found with ID: " + attributeId);
		}

		ProductAttributeEO productAttributeEO = optionalAttribute.get();

		if (productAttributeCreateDTO.getAttributeName() != null) {
			productAttributeEO.setAttributeName(productAttributeCreateDTO.getAttributeName());
		}
		if (productAttributeCreateDTO.getAttributeValue() != null) {
			productAttributeEO.setAttributeValue(productAttributeCreateDTO.getAttributeValue());
		}

		ProductAttributeEO updatedEntity = productAttributeRepository.save(productAttributeEO);
		return UserMapper.toProductAttributeDTO(updatedEntity);
	}

	@Override
	public void deleteProductAttribute(Long attributeId) {
		if (!productAttributeRepository.existsById(attributeId)) {
			throw new RuntimeException("Product Attribute not found with ID: " + attributeId);
		}
		productAttributeRepository.deleteById(attributeId);
	}

	@Override
	public List<ProductDTO> getProductsByCategoryId(Integer categoryId) {
		List<ProductDTO> productDTOs = new ArrayList<>();
		try {
			if (categoryId == null) {
				return productDTOs;
			}
			ProductCategoriesEO category = productCatRepository.findById(Long.valueOf(categoryId)).orElse(null);
			if (category == null || category.getStatus() == null
					|| !category.getStatus().equals(Constants.STATUS_ACTIVE)) {
				return productDTOs;
			}
			List<ProductEO> products = productRepository.findByCategoryAndStatus(category, Constants.STATUS_ACTIVE);
			if (products == null || products.isEmpty()) {
				return productDTOs;
			}
			for (ProductEO product : products) {
				ProductDTO productDTO = UserMapper.toProductDTO(product);
				productDTO.setId(product.getId());
				List<ProductVariantEO> existingVariants = productVariantRepository.findByProduct(product);
				if (existingVariants != null && !existingVariants.isEmpty()) {
					ProductVariantEO cheapestVariant = existingVariants.stream()
						.filter(v -> v.getSellingPrice() != null)
						.min((v1, v2) -> v1.getSellingPrice().compareTo(v2.getSellingPrice()))
						.orElse(existingVariants.get(0));
					List<ProductImageEO> images = productImageRepository.findByProductVar(cheapestVariant);
					if (images != null && !images.isEmpty()) {
						productDTO.setMainImage(resolveMainImage(images).getImage());
					}
					productDTO.setPrice(cheapestVariant.getSellingPrice());
					productDTO.setMrp(cheapestVariant.getMrp());
					productDTO.setCurrency(Constants.PAYMENT_CURRENCY);
					productDTO.setSku(cheapestVariant.getSkuCode());
					InventoryEO inventory = inventoryRepository.findByProductVariant(cheapestVariant);
					if (inventory != null) {
						productDTO.setStock(inventory.getAvailableQty());
						productDTO
							.setInStock(inventory.getAvailableQty() != null && inventory.getAvailableQty() > 0 ? 1 : 0);
					}
				}
				productDTOs.add(productDTO);
			}
		}
		catch (Exception e) {
			logger.error("Error in getProductsByCategoryId for categoryId={}", categoryId, e);
		}
		return productDTOs;
	}

	/**
	 * Strips trailing plural suffix to produce a basic stem. e.g. "sweets" → "sweet",
	 * "cookies" → "cooki", "milk" → "milk"
	 */
	private static String buildStem(String query) {
		if (query == null)
			return "";
		String lower = query.toLowerCase().trim();
		if (lower.endsWith("es") && lower.length() > 3) {
			return lower.substring(0, lower.length() - 2);
		}
		else if (lower.endsWith("s") && lower.length() > 2) {
			return lower.substring(0, lower.length() - 1);
		}
		return lower;
	}

	private void savePngToUploadDirAndSetSrc(ProductCategoriesEO category, MultipartFile imageFile) {
		try {
			String originalFilename = imageFile.getOriginalFilename();
			if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".png")) {
				throw new IllegalArgumentException("Only PNG images are allowed.");
			}

			String uploadDir = normalizedUploadDir(categoryImageUploadDir);
			File dir = new File(uploadDir);
			if (!dir.exists()) {
				dir.mkdirs();
			}

			String timestamp = String.valueOf(System.currentTimeMillis());
			String filename = timestamp + "_" + originalFilename;
			String filePath = uploadDir + filename;

			imageFile.transferTo(new File(filePath));
			category.setSrc("/" + filename);

		}
		catch (Exception e) {
			throw new RuntimeException("Failed to save image file", e);
		}
	}

	/**
	 * Returns the image marked as main (isMainImage = "Y"). If none is found, promotes
	 * the first image to main by persisting isMainImage = "Y" and returns it.
	 */
	private ProductImageEO resolveMainImage(List<ProductImageEO> images) {
		return images.stream().filter(img -> "Y".equals(img.getIsMainImage())).findFirst().orElseGet(() -> {
			ProductImageEO first = images.get(0);
			first.setIsMainImage("Y");
			productImageRepository.save(first);
			return first;
		});
	}

	private static String normalizedUploadDir(String uploadDir) {
		if (uploadDir == null || uploadDir.isBlank()) {
			return "";
		}
		if (!uploadDir.endsWith("/") && !uploadDir.endsWith("\\")) {
			return uploadDir + "/";
		}
		return uploadDir;
	}

	// ─── Unit of Measure ────────────────────────────────────────────────────────

	@Override
	public ResponseDTO createUom(UomCreateDTO dto) {
		ResponseDTO result = new ResponseDTO();
		try {
			if (dto.getUomCode() != null
					&& unitOfMeasureRepository.existsByUomCodeAndStatus(dto.getUomCode(), "ACTIVE")) {
				result.setResponseStatus(Constants.FAILURE_STATUS);
				result.setResponseMessage("UOM with code '" + dto.getUomCode() + "' already exists");
				return result;
			}
			UnitOfMeasureEO eo = UnitOfMeasureEO.builder()
				.uomCode(dto.getUomCode())
				.uomName(dto.getUomName())
				.uomType(dto.getUomType())
				.baseUomFlag(dto.getBaseUomFlag() != null ? dto.getBaseUomFlag() : "N")
				.decimalAllowed(dto.getDecimalAllowed() != null ? dto.getDecimalAllowed() : "N")
				.description(dto.getDescription())
				.createdBy(dto.getCreatedBy())
				.status("ACTIVE")
				.build();
			unitOfMeasureRepository.save(eo);
			result.setResponseStatus(Constants.SUCCESS_STATUS);
			result.setResponseMessage("Unit of Measure created successfully");
		}
		catch (Exception e) {
			logger.error("Error creating UOM: {}", e.getMessage(), e);
			result.setResponseStatus(Constants.FAILURE_STATUS);
			result.setResponseMessage("Failed to create UOM: " + e.getMessage());
		}
		return result;
	}

	@Override
	public ResponseDTO updateUom(Long uomId, UomUpdateDTO dto) {
		ResponseDTO result = new ResponseDTO();
		try {
			Optional<UnitOfMeasureEO> optional = unitOfMeasureRepository.findByUomIdAndStatus(uomId, "ACTIVE");
			if (optional.isEmpty()) {
				result.setResponseStatus(Constants.FAILURE_STATUS);
				result.setResponseMessage("UOM not found or inactive");
				return result;
			}
			UnitOfMeasureEO eo = optional.get();
			if (dto.getUomCode() != null)
				eo.setUomCode(dto.getUomCode());
			if (dto.getUomName() != null)
				eo.setUomName(dto.getUomName());
			if (dto.getUomType() != null)
				eo.setUomType(dto.getUomType());
			if (dto.getBaseUomFlag() != null)
				eo.setBaseUomFlag(dto.getBaseUomFlag());
			if (dto.getDecimalAllowed() != null)
				eo.setDecimalAllowed(dto.getDecimalAllowed());
			if (dto.getDescription() != null)
				eo.setDescription(dto.getDescription());
			if (dto.getStatus() != null)
				eo.setStatus(dto.getStatus());
			if (dto.getUpdatedBy() != null)
				eo.setUpdatedBy(dto.getUpdatedBy());
			unitOfMeasureRepository.save(eo);
			result.setResponseStatus(Constants.SUCCESS_STATUS);
			result.setResponseMessage("Unit of Measure updated successfully");
		}
		catch (Exception e) {
			logger.error("Error updating UOM {}: {}", uomId, e.getMessage(), e);
			result.setResponseStatus(Constants.FAILURE_STATUS);
			result.setResponseMessage("Failed to update UOM: " + e.getMessage());
		}
		return result;
	}

	@Override
	public ResponseDTO deleteUom(Long uomId) {
		ResponseDTO result = new ResponseDTO();
		try {
			Optional<UnitOfMeasureEO> optional = unitOfMeasureRepository.findByUomIdAndStatus(uomId, "ACTIVE");
			if (optional.isEmpty()) {
				result.setResponseStatus(Constants.FAILURE_STATUS);
				result.setResponseMessage("UOM not found or already inactive");
				return result;
			}
			UnitOfMeasureEO eo = optional.get();
			eo.setStatus("INACTIVE");
			unitOfMeasureRepository.save(eo);
			result.setResponseStatus(Constants.SUCCESS_STATUS);
			result.setResponseMessage("Unit of Measure deleted successfully");
		}
		catch (Exception e) {
			logger.error("Error deleting UOM {}: {}", uomId, e.getMessage(), e);
			result.setResponseStatus(Constants.FAILURE_STATUS);
			result.setResponseMessage("Failed to delete UOM: " + e.getMessage());
		}
		return result;
	}

	@Override
	public UomResponseDTO getUomById(Long uomId) {
		return unitOfMeasureRepository.findByUomIdAndStatus(uomId, "ACTIVE")
			.map(this::mapToUomResponseDTO)
			.orElse(null);
	}

	@Override
	public List<UomResponseDTO> getAllUoms() {
		return unitOfMeasureRepository.findAllByStatus("ACTIVE")
			.stream()
			.map(this::mapToUomResponseDTO)
			.collect(Collectors.toList());
	}

	private UomResponseDTO mapToUomResponseDTO(UnitOfMeasureEO eo) {
		return UomResponseDTO.builder()
			.uomId(eo.getUomId())
			.uomCode(eo.getUomCode())
			.uomName(eo.getUomName())
			.uomType(eo.getUomType())
			.baseUomFlag(eo.getBaseUomFlag())
			.decimalAllowed(eo.getDecimalAllowed())
			.status(eo.getStatus())
			.description(eo.getDescription())
			.createdAt(eo.getCreatedAt())
			.createdBy(eo.getCreatedBy())
			.updatedAt(eo.getUpdatedAt())
			.updatedBy(eo.getUpdatedBy())
			.build();
	}

	// ─── Label PDF Generation ─────────────────────────────────────────────────

	@Override
	@Transactional
	public LabelPrintResponseDTO generateLabelPdf(LabelPrintRequestDTO request) {
		LabelPrintResponseDTO response = new LabelPrintResponseDTO();
		LabelPrintJobEO job = new LabelPrintJobEO();
		job.setBrandName(request.getBrandName() != null ? request.getBrandName().trim() : "");
		job.setBatchNo(request.getBatchNo());
		job.setBarcodes(request.getBarcodes());
		job.setLabelConfigId(request.getLabelConfigId());
		try {
			// ── Validation ──────────────────────────────────────────────────
			if (request.getBrandName() == null || request.getBrandName().trim().isEmpty()) {
				response.setStatus(Constants.FAILURE_STATUS);
				response.setMessage("Brand Name is mandatory");
				saveFailureJob(job, response.getMessage());
				return response;
			}
			boolean hasBatchNo = request.getBatchNo() != null && !request.getBatchNo().trim().isEmpty();
			boolean hasBarcodes = request.getBarcodes() != null && !request.getBarcodes().trim().isEmpty();
			if (!hasBatchNo && !hasBarcodes) {
				response.setStatus(Constants.FAILURE_STATUS);
				response.setMessage("Either Batch No or at least one Barcode value must be provided");
				saveFailureJob(job, response.getMessage());
				return response;
			}

			// ── Resolve label configuration ──────────────────────────────────
			LabelConfigEO labelConfig = null;
			if (request.getLabelConfigId() != null) {
				labelConfig = labelConfigRepository.findById(request.getLabelConfigId()).orElse(null);
				if (labelConfig == null) {
					response.setStatus(Constants.FAILURE_STATUS);
					response.setMessage("Label configuration not found for ID: " + request.getLabelConfigId());
					saveFailureJob(job, response.getMessage());
					return response;
				}
				if (!"ACTIVE".equalsIgnoreCase(labelConfig.getStatus())) {
					response.setStatus(Constants.FAILURE_STATUS);
					response.setMessage("Label configuration ID " + request.getLabelConfigId() + " is not ACTIVE");
					saveFailureJob(job, response.getMessage());
					return response;
				}
			}
			else {
				// Fall back to the system default config (if any)
				labelConfig = labelConfigRepository.findByIsDefaultTrueAndStatus("ACTIVE").orElse(null);
			}

			// ── Fetch InventoryDetails records ───────────────────────────────
			List<InventoryDetailsEO> detailsList;
			if (hasBatchNo) {
				detailsList = inventoryDetailsRepository.findByBatchNo(request.getBatchNo().trim());
				if (detailsList.isEmpty()) {
					response.setStatus(Constants.FAILURE_STATUS);
					response.setMessage("No inventory details found for Batch No: " + request.getBatchNo().trim());
					saveFailureJob(job, response.getMessage());
					return response;
				}
			}
			else {
				List<String> barcodeList = Arrays.stream(request.getBarcodes().split(","))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.collect(Collectors.toList());
				detailsList = inventoryDetailsRepository.findByBarcodeIn(barcodeList);
				if (detailsList.isEmpty()) {
					response.setStatus(Constants.FAILURE_STATUS);
					response.setMessage("No inventory details found for provided barcode(s)");
					saveFailureJob(job, response.getMessage());
					return response;
				}
			}

			// ── Build label data objects ──────────────────────────────────────
			DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MM/yyyy");
			List<LabelData> labels = new ArrayList<>();
			for (InventoryDetailsEO detail : detailsList) {
				InventoryEO inventory = detail.getInventory();
				ProductVariantEO variant = inventory.getProductVariant();
				ProductEO product = variant.getProduct();

				LabelData ld = new LabelData();
				ld.brandName = sanitize(request.getBrandName().trim());
				ld.productName = sanitize(product.getName());

				StringBuilder varDet = new StringBuilder();
				if (variant.getPackSize() != null)
					varDet.append(variant.getPackSize());
				if (variant.getUom() != null) {
					if (varDet.length() > 0)
						varDet.append(" ");
					varDet.append(variant.getUom());
				}
				if (variant.getContainerType() != null) {
					if (varDet.length() > 0)
						varDet.append(" ");
					varDet.append(variant.getContainerType());
				}
				ld.variantDetails = varDet.length() > 0 ? sanitize(varDet.toString()) : "N/A";

				ld.netQuantity = (variant.getPackSize() != null)
						? sanitize(variant.getPackSize() + (variant.getUom() != null ? " " + variant.getUom() : ""))
						: "N/A";

				ld.mfgDate = detail.getMfd() != null ? detail.getMfd().format(monthFmt) : "N/A";
				ld.expDate = detail.getExpiryDate() != null ? detail.getExpiryDate().format(monthFmt) : "N/A";
				ld.batchNo = sanitize(detail.getBatchNo());
				ld.mrp = variant.getMrp() != null ? "Rs." + variant.getMrp().toPlainString() : "N/A";
				ld.barcodeValue = detail.getBarcode();
				// FSSAI code from the resolved label configuration (blank when config is
				// null or code not set)
				ld.fssaiCode = (labelConfig != null && labelConfig.getFssaiCode() != null
						&& !labelConfig.getFssaiCode().trim().isEmpty()) ? sanitize(labelConfig.getFssaiCode().trim())
								: "";
				labels.add(ld);
			}

			// ── Generate PDF ─────────────────────────────────────────────────
			String dir = labelPdfDir.endsWith("/") || labelPdfDir.endsWith("\\") ? labelPdfDir : labelPdfDir + "/";
			new File(dir).mkdirs();
			String filename = "labels_" + System.currentTimeMillis() + ".pdf";
			String filePath = dir + filename;
			buildLabelPdf(labels, filePath, labelConfig);

			String pdfUrl = "/products/labels/download/" + filename;
			response.setStatus(Constants.SUCCESS_STATUS);
			response.setMessage("Label PDF generated successfully");
			response.setPdfUrl(pdfUrl);
			response.setLabelCount(labels.size());

			// ── Persist job record ──────────────────────────────────────────
			String resolved = labels.stream().map(l -> l.barcodeValue).collect(Collectors.joining(","));
			job.setResolvedBarcodes(resolved);
			job.setLabelCount(labels.size());
			job.setPdfUrl(pdfUrl);
			job.setPdfFilePath(filePath);
			job.setStatus(Constants.SUCCESS_STATUS);
			labelPrintJobRepository.save(job);

		}
		catch (Exception e) {
			logger.error("Error generating label PDF: {}", e.getMessage(), e);
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage("Failed to generate label PDF: " + e.getMessage());
			saveFailureJob(job, e.getMessage());
		}
		return response;
	}

	// ─── Label Job History ────────────────────────────────────────────────────

	@Override
	public List<LabelPrintJobDTO> getLabelPrintJobs(String batchNo, String barcode, int page, int limit) {
		int pageIndex = Math.max(0, page - 1);
		PageRequest pageable = PageRequest.of(pageIndex, limit > 0 ? limit : 20);

		List<LabelPrintJobEO> jobs;
		if (batchNo != null && !batchNo.trim().isEmpty()) {
			jobs = labelPrintJobRepository.findByBatchNoOrderByPrintedAtDesc(batchNo.trim(), pageable).getContent();
		}
		else if (barcode != null && !barcode.trim().isEmpty()) {
			jobs = labelPrintJobRepository.findByResolvedBarcodesContaining(barcode.trim());
			int from = pageIndex * pageable.getPageSize();
			int to = Math.min(from + pageable.getPageSize(), jobs.size());
			jobs = from >= jobs.size() ? Collections.emptyList() : jobs.subList(from, to);
		}
		else {
			jobs = labelPrintJobRepository.findAllByOrderByPrintedAtDesc(pageable).getContent();
		}
		return jobs.stream().map(this::toJobDTO).collect(Collectors.toList());
	}

	@Override
	public LabelPrintJobDTO getLabelPrintJobById(Long jobId) {
		return labelPrintJobRepository.findById(jobId).map(this::toJobDTO).orElse(null);
	}

	@Override
	@Transactional
	public LabelPrintResponseDTO reprintLabelJob(Long jobId) {
		LabelPrintJobEO original = labelPrintJobRepository.findById(jobId).orElse(null);
		if (original == null) {
			LabelPrintResponseDTO r = new LabelPrintResponseDTO();
			r.setStatus(Constants.FAILURE_STATUS);
			r.setMessage("Label print job not found for ID: " + jobId);
			return r;
		}
		// Re-use original request parameters
		LabelPrintRequestDTO req = new LabelPrintRequestDTO();
		req.setBrandName(original.getBrandName());
		req.setBatchNo(original.getBatchNo());
		req.setBarcodes(original.getBarcodes());
		req.setLabelConfigId(original.getLabelConfigId());
		return generateLabelPdf(req);
	}

	/** Converts a job entity to DTO, including pdfFileExists check. */
	private LabelPrintJobDTO toJobDTO(LabelPrintJobEO job) {
		boolean exists = job.getPdfFilePath() != null && new File(job.getPdfFilePath()).exists();
		String configName = null;
		if (job.getLabelConfigId() != null) {
			configName = labelConfigRepository.findById(job.getLabelConfigId())
				.map(LabelConfigEO::getConfigName)
				.orElse(null);
		}
		return LabelPrintJobDTO.builder()
			.jobId(job.getId())
			.brandName(job.getBrandName())
			.batchNo(job.getBatchNo())
			.barcodes(job.getBarcodes())
			.resolvedBarcodes(job.getResolvedBarcodes())
			.labelCount(job.getLabelCount())
			.pdfUrl(job.getPdfUrl())
			.pdfFileExists(exists)
			.status(job.getStatus())
			.errorMessage(job.getErrorMessage())
			.printedAt(job.getPrintedAt())
			.printedBy(job.getPrintedBy())
			.labelConfigId(job.getLabelConfigId())
			.labelConfigName(configName)
			.build();
	}

	/** Saves a FAILURE job record (best-effort — does not throw). */
	private void saveFailureJob(LabelPrintJobEO job, String errorMessage) {
		try {
			job.setStatus(Constants.FAILURE_STATUS);
			job.setErrorMessage(errorMessage);
			job.setLabelCount(0);
			labelPrintJobRepository.save(job);
		}
		catch (Exception ex) {
			logger.warn("Could not persist FAILURE label job record: {}", ex.getMessage());
		}
	}

	/** Internal label data carrier. */
	private static class LabelData {

		String brandName, productName, variantDetails, netQuantity;

		String mfgDate, expDate, batchNo, mrp, barcodeValue;

		String fssaiCode;

	}

	/**
	 * Builds a thermal-printer-friendly PDF. Label dimensions, field visibility, and logo
	 * rendering are driven by the supplied {@code LabelConfigEO}. When {@code config} is
	 * null the built-in defaults (2" × 2", all fields, no logo) are applied.
	 */
	private void buildLabelPdf(List<LabelData> labels, String outputPath, LabelConfigEO config) throws Exception {

		// ── Dimensions from config or hard-coded defaults ─────────────────────
		float labelWidthInch = (config != null && config.getLabelWidthInches() != null) ? config.getLabelWidthInches()
				: 2.0f;
		float labelHeightInch = (config != null && config.getLabelHeightInches() != null)
				? config.getLabelHeightInches() : 2.0f;

		final float PW = labelWidthInch * 72f; // points
		final float PH = labelHeightInch * 72f;

		// Scale typographic constants proportionally (base = 2"×2" = 144 pt wide)
		float scale = PW / 144f;
		final float LM = 3f;
		final float TM = 3f;
		final float LW = PW - LM * 2;
		final float LH = PH - TM * 2;
		final float PAD = Math.max(2f, 3f * scale);
		final float LINEH = Math.max(8f, 10f * scale);
		final float FSZNORM = Math.max(5f, 5.5f * scale);
		final float FSZBOLD = FSZNORM;
		final float BAR_H = Math.max(15f, 22f * scale);
		final float BAR_TXT = Math.max(4f, 5f * scale);

		// ── Feature flags from config (null config = all on / no logo) ────────
		boolean showLogo = config != null && Boolean.TRUE.equals(config.getShowLogo());
		boolean showBrandName = config == null || !Boolean.FALSE.equals(config.getShowBrandName());
		boolean showProductName = config == null || !Boolean.FALSE.equals(config.getShowProductName());
		boolean showVariantDetails = config == null || !Boolean.FALSE.equals(config.getShowVariantDetails());
		boolean showNetQuantity = config == null || !Boolean.FALSE.equals(config.getShowNetQuantity());
		boolean showMfgDate = config == null || !Boolean.FALSE.equals(config.getShowMfgDate());
		boolean showExpDate = config == null || !Boolean.FALSE.equals(config.getShowExpDate());
		boolean showBatchNo = config == null || !Boolean.FALSE.equals(config.getShowBatchNo());
		boolean showMrp = config == null || !Boolean.FALSE.equals(config.getShowMrp());
		boolean showBarcode = config == null || !Boolean.FALSE.equals(config.getShowBarcode());
		boolean showFssaiCode = (config == null || !Boolean.FALSE.equals(config.getShowFssaiCode()));

		PDFont bold = PDType1Font.HELVETICA_BOLD;
		PDFont regular = PDType1Font.HELVETICA;
		PDRectangle pageSize = new PDRectangle(PW, PH);

		try (PDDocument doc = new PDDocument()) {

			// ── Pre-load logo once (shared across all label pages) ────────────
			PDImageXObject logoImg = null;
			float logoH = 0f;
			if (showLogo) {
				// Resolve the logo path: use config path if set, otherwise fall back to
				// default
				String rawLogoPath = (config != null && config.getLogoPath() != null
						&& !config.getLogoPath().trim().isEmpty()) ? config.getLogoPath().trim()
								: com.user.utility.Constants.DEFAULT_LABEL_LOGO_PATH;

				// Normalize path: replace Windows-style backslashes with forward slashes
				// so the path works correctly on Linux (Docker container).
				String logoPath = rawLogoPath.replace('\\', '/');

				// If the path does not start with '/', treat it as absolute by prepending
				// '/'.
				// Paths stored as e.g. "public/companyLogo/..." should resolve to
				// "/public/...".
				if (!logoPath.startsWith("/")) {
					logoPath = "/" + logoPath;
				}

				try {
					File logoFile = new File(logoPath);
					if (logoFile.exists()) {
						BufferedImage logoRaw = javax.imageio.ImageIO.read(logoFile);
						if (logoRaw != null) {
							logoImg = LosslessFactory.createFromImage(doc, logoRaw);
							// Logo height: 20% of label height, capped at 36 pt × scale
							logoH = Math.min(PH * 0.20f, 36f * scale);
						}
						else {
							logger.warn("Logo image could not be decoded at path: {}", logoPath);
						}
					}
					else {
						logger.warn("Logo file not found at path: {} (raw config value: {})", logoPath, rawLogoPath);
					}
				}
				catch (Exception ex) {
					logger.warn("Failed to load logo from path {}: {}", logoPath, ex.getMessage());
				}
			}

			for (LabelData ld : labels) {
				// ── One page per label ─────────────────────────────────────
				PDPage page = new PDPage(pageSize);
				doc.addPage(page);

				try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

					float lx = LM;
					float ly = TM;

					// ── Draw label border ──────────────────────────────────
					cs.setStrokingColor(0f, 0f, 0f);
					cs.setLineWidth(0.5f);
					cs.addRect(lx, ly, LW, LH);
					cs.stroke();

					float tx = lx + PAD;
					float ty = ly + LH - PAD - FSZBOLD;

					// ── Logo (centred at top of label) ─────────────────────
					if (logoImg != null) {
						float logoAspect = (float) logoImg.getWidth() / (float) logoImg.getHeight();
						float logoW = logoH * logoAspect;
						if (logoW > LW - PAD * 2) {
							logoW = LW - PAD * 2;
						}
						float logoX = lx + (LW - logoW) / 2f;
						float logoY = ty - logoH + FSZBOLD;
						cs.drawImage(logoImg, logoX, logoY, logoW, logoH);
						ty -= (logoH + PAD);
					}

					// ── Text fields (top → down) ───────────────────────────
					List<String[]> fieldList = new ArrayList<>();
					if (showBrandName)
						fieldList.add(new String[] { "Brand: ", ld.brandName });
					if (showProductName)
						fieldList.add(new String[] { "Product: ", ld.productName });
					if (showVariantDetails)
						fieldList.add(new String[] { "Variant: ", ld.variantDetails });
					if (showNetQuantity)
						fieldList.add(new String[] { "Net Qty: ", ld.netQuantity });
					if (showMfgDate)
						fieldList.add(new String[] { "MFG: ", ld.mfgDate });
					if (showExpDate)
						fieldList.add(new String[] { "EXP: ", ld.expDate });
					if (showBatchNo)
						fieldList.add(new String[] { "Batch: ", ld.batchNo });
					if (showMrp)
						fieldList.add(new String[] { "MRP: ", ld.mrp });
					if (showFssaiCode && ld.fssaiCode != null && !ld.fssaiCode.isEmpty())
						fieldList.add(new String[] { "FSSAI Lic. No.: ", ld.fssaiCode });

					// Number of fields per row — driven by config; default is 1 (single
					// column)
					int columnsPerRow = (config != null && config.getColumnsPerRow() != null
							&& config.getColumnsPerRow() > 1) ? config.getColumnsPerRow() : 1;

					if (columnsPerRow > 1) {
						// ── Multi-column layout (N fields per row) ─────────────
						float colW = LW / (float) columnsPerRow;
						float colValW = colW - PAD * 2.5f; // usable value width per
															// column

						for (int i = 0; i < fieldList.size(); i += columnsPerRow) {
							for (int col = 0; col < columnsPerRow && (i + col) < fieldList.size(); col++) {
								String[] f = fieldList.get(i + col);
								float colX = lx + PAD + col * colW;
								float lblW = bold.getStringWidth(f[0]) / 1000f * FSZBOLD;
								cs.beginText();
								cs.setFont(bold, FSZBOLD);
								cs.newLineAtOffset(colX, ty);
								cs.showText(f[0]);
								cs.endText();
								String val = truncatePdf(f[1], regular, FSZNORM, colValW - lblW);
								cs.beginText();
								cs.setFont(regular, FSZNORM);
								cs.newLineAtOffset(colX + lblW, ty);
								cs.showText(val);
								cs.endText();
							}
							ty -= LINEH;
						}
					}
					else {
						// ── Single field per row ───────────────────────────────
						float maxValW = LW - PAD * 2;
						for (String[] f : fieldList) {
							float lblW = bold.getStringWidth(f[0]) / 1000f * FSZBOLD;
							cs.beginText();
							cs.setFont(bold, FSZBOLD);
							cs.newLineAtOffset(tx, ty);
							cs.showText(f[0]);
							cs.endText();
							String val = truncatePdf(f[1], regular, FSZNORM, maxValW - lblW);
							cs.beginText();
							cs.setFont(regular, FSZNORM);
							cs.newLineAtOffset(tx + lblW, ty);
							cs.showText(val);
							cs.endText();
							ty -= LINEH;
						}
					}

					// ── Barcode section ────────────────────────────────────
					if (showBarcode) {
						float sepY = ly + PAD + BAR_TXT + 4 + BAR_H + 6;
						cs.setLineWidth(0.3f);
						cs.moveTo(lx + PAD, sepY);
						cs.lineTo(lx + LW - PAD, sepY);
						cs.stroke();

						float barcodeX = lx + PAD;
						float barcodeW = LW - PAD * 2;
						float barcodeImgY = ly + PAD + BAR_TXT + 4;
						try {
							BitMatrix bm = new Code128Writer().encode(ld.barcodeValue, BarcodeFormat.CODE_128,
									(int) (barcodeW * 2), (int) (BAR_H * 2));
							BufferedImage bi = MatrixToImageWriter.toBufferedImage(bm);
							PDImageXObject img = LosslessFactory.createFromImage(doc, bi);
							cs.drawImage(img, barcodeX, barcodeImgY, barcodeW, BAR_H);
						}
						catch (Exception ex) {
							logger.warn("Barcode generation failed for {}: {}", ld.barcodeValue, ex.getMessage());
						}

						float bvW = regular.getStringWidth(ld.barcodeValue) / 1000f * BAR_TXT;
						float bvX = barcodeX + (barcodeW - bvW) / 2f;
						cs.beginText();
						cs.setFont(regular, BAR_TXT);
						cs.newLineAtOffset(bvX, ly + PAD);
						cs.showText(ld.barcodeValue);
						cs.endText();
					}
				}
			}

			doc.save(outputPath);
		}
	}

	/** Truncate text so it fits within maxWidth pt at the given font size. */
	private String truncatePdf(String text, PDFont font, float size, float maxWidth) {
		if (text == null)
			return "";
		try {
			if (font.getStringWidth(text) / 1000f * size <= maxWidth)
				return text;
			while (text.length() > 1) {
				text = text.substring(0, text.length() - 1);
				if (font.getStringWidth(text + "..") / 1000f * size <= maxWidth)
					return text + "..";
			}
		}
		catch (Exception ignored) {
		}
		return text;
	}

	/** Strip characters outside Latin-1 (PDFBox Type1 font limitation). */
	private String sanitize(String s) {
		if (s == null)
			return "";
		StringBuilder sb = new StringBuilder(s.length());
		for (char c : s.toCharArray())
			sb.append(c < 256 ? c : '?');
		return sb.toString();
	}

	// ─── Label Config MASTER CRUD ─────────────────────────────────────────────

	@Override
	@Transactional
	public LabelConfigResponseDTO createLabelConfig(LabelConfigCreateDTO dto) {
		if (dto.getConfigName() == null || dto.getConfigName().trim().isEmpty()) {
			throw new IllegalArgumentException("configName is mandatory");
		}
		if (dto.getLabelWidthInches() == null || dto.getLabelWidthInches() <= 0) {
			throw new IllegalArgumentException("labelWidthInches must be a positive value");
		}
		if (dto.getLabelHeightInches() == null || dto.getLabelHeightInches() <= 0) {
			throw new IllegalArgumentException("labelHeightInches must be a positive value");
		}
		if (labelConfigRepository.findByConfigNameIgnoreCase(dto.getConfigName().trim()).isPresent()) {
			throw new IllegalArgumentException(
					"A label configuration with name '" + dto.getConfigName().trim() + "' already exists");
		}

		// If this one should be default, clear any existing default first
		if (Boolean.TRUE.equals(dto.getIsDefault())) {
			clearExistingDefault();
		}

		LabelConfigEO eo = LabelConfigEO.builder()
			.configName(dto.getConfigName().trim())
			.description(dto.getDescription())
			.labelWidthInches(dto.getLabelWidthInches())
			.labelHeightInches(dto.getLabelHeightInches())
			.showLogo(dto.getShowLogo() != null ? dto.getShowLogo() : false)
			.logoPath(normalizePath(dto.getLogoPath()))
			.showBrandName(dto.getShowBrandName() != null ? dto.getShowBrandName() : true)
			.showProductName(dto.getShowProductName() != null ? dto.getShowProductName() : true)
			.showVariantDetails(dto.getShowVariantDetails() != null ? dto.getShowVariantDetails() : true)
			.showNetQuantity(dto.getShowNetQuantity() != null ? dto.getShowNetQuantity() : true)
			.showMfgDate(dto.getShowMfgDate() != null ? dto.getShowMfgDate() : true)
			.showExpDate(dto.getShowExpDate() != null ? dto.getShowExpDate() : true)
			.showBatchNo(dto.getShowBatchNo() != null ? dto.getShowBatchNo() : true)
			.showMrp(dto.getShowMrp() != null ? dto.getShowMrp() : true)
			.showBarcode(dto.getShowBarcode() != null ? dto.getShowBarcode() : true)
			.fssaiCode(dto.getFssaiCode() != null ? dto.getFssaiCode().trim() : null)
			.showFssaiCode(dto.getShowFssaiCode() != null ? dto.getShowFssaiCode() : true)
			.isDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false)
			.columnsPerRow(dto.getColumnsPerRow() != null && dto.getColumnsPerRow() > 0 ? dto.getColumnsPerRow() : 1)
			.status(dto.getStatus() != null && !dto.getStatus().trim().isEmpty() ? dto.getStatus().trim().toUpperCase()
					: "ACTIVE")
			.createdBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "SYSTEM")
			.build();

		eo = labelConfigRepository.save(eo);
		logger.info("Label config created: id={}, name={}", eo.getId(), eo.getConfigName());
		return toLabelConfigDTO(eo);
	}

	@Override
	@Transactional
	public LabelConfigResponseDTO updateLabelConfig(Long id, LabelConfigCreateDTO dto) {
		LabelConfigEO eo = labelConfigRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Label configuration not found for ID: " + id));

		if (dto.getConfigName() != null && !dto.getConfigName().trim().isEmpty()) {
			// Check name uniqueness only if changed
			if (!dto.getConfigName().trim().equalsIgnoreCase(eo.getConfigName())) {
				boolean nameExists = labelConfigRepository.findByConfigNameIgnoreCase(dto.getConfigName().trim())
					.isPresent();
				if (nameExists) {
					throw new IllegalArgumentException(
							"A label configuration with name '" + dto.getConfigName().trim() + "' already exists");
				}
			}
			eo.setConfigName(dto.getConfigName().trim());
		}
		if (dto.getDescription() != null)
			eo.setDescription(dto.getDescription());
		if (dto.getLabelWidthInches() != null && dto.getLabelWidthInches() > 0)
			eo.setLabelWidthInches(dto.getLabelWidthInches());
		if (dto.getLabelHeightInches() != null && dto.getLabelHeightInches() > 0)
			eo.setLabelHeightInches(dto.getLabelHeightInches());
		if (dto.getShowLogo() != null)
			eo.setShowLogo(dto.getShowLogo());
		if (dto.getLogoPath() != null)
			eo.setLogoPath(normalizePath(dto.getLogoPath()));
		if (dto.getShowBrandName() != null)
			eo.setShowBrandName(dto.getShowBrandName());
		if (dto.getShowProductName() != null)
			eo.setShowProductName(dto.getShowProductName());
		if (dto.getShowVariantDetails() != null)
			eo.setShowVariantDetails(dto.getShowVariantDetails());
		if (dto.getShowNetQuantity() != null)
			eo.setShowNetQuantity(dto.getShowNetQuantity());
		if (dto.getShowMfgDate() != null)
			eo.setShowMfgDate(dto.getShowMfgDate());
		if (dto.getShowExpDate() != null)
			eo.setShowExpDate(dto.getShowExpDate());
		if (dto.getShowBatchNo() != null)
			eo.setShowBatchNo(dto.getShowBatchNo());
		if (dto.getShowMrp() != null)
			eo.setShowMrp(dto.getShowMrp());
		if (dto.getShowBarcode() != null)
			eo.setShowBarcode(dto.getShowBarcode());
		if (dto.getFssaiCode() != null)
			eo.setFssaiCode(dto.getFssaiCode().trim());
		if (dto.getShowFssaiCode() != null)
			eo.setShowFssaiCode(dto.getShowFssaiCode());
		if (dto.getColumnsPerRow() != null && dto.getColumnsPerRow() > 0)
			eo.setColumnsPerRow(dto.getColumnsPerRow());
		if (dto.getStatus() != null)
			eo.setStatus(dto.getStatus().trim().toUpperCase());
		if (dto.getUpdatedBy() != null)
			eo.setUpdatedBy(dto.getUpdatedBy());

		// Handle default flag change
		if (Boolean.TRUE.equals(dto.getIsDefault()) && !Boolean.TRUE.equals(eo.getIsDefault())) {
			clearExistingDefault();
			eo.setIsDefault(true);
		}
		else if (Boolean.FALSE.equals(dto.getIsDefault())) {
			eo.setIsDefault(false);
		}

		eo = labelConfigRepository.save(eo);
		logger.info("Label config updated: id={}, name={}", eo.getId(), eo.getConfigName());
		return toLabelConfigDTO(eo);
	}

	@Override
	@Transactional
	public ResponseDTO deleteLabelConfig(Long id) {
		ResponseDTO response = new ResponseDTO();
		LabelConfigEO eo = labelConfigRepository.findById(id).orElse(null);
		if (eo == null) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Label configuration not found for ID: " + id);
			return response;
		}
		eo.setStatus("INACTIVE");
		if (Boolean.TRUE.equals(eo.getIsDefault())) {
			eo.setIsDefault(false);
		}
		labelConfigRepository.save(eo);
		logger.info("Label config deactivated: id={}, name={}", id, eo.getConfigName());
		response.setResponseStatus(Constants.SUCCESS_STATUS);
		response.setResponseMessage("Label configuration '" + eo.getConfigName() + "' deactivated successfully");
		return response;
	}

	@Override
	public LabelConfigResponseDTO getLabelConfigById(Long id) {
		return labelConfigRepository.findById(id).map(this::toLabelConfigDTO).orElse(null);
	}

	@Override
	public List<LabelConfigResponseDTO> getAllLabelConfigs(String status) {
		List<LabelConfigEO> list = (status != null && !status.trim().isEmpty())
				? labelConfigRepository.findByStatusOrderByConfigNameAsc(status.trim().toUpperCase())
				: labelConfigRepository.findAllByOrderByConfigNameAsc();
		return list.stream().map(this::toLabelConfigDTO).collect(Collectors.toList());
	}

	@Override
	@Transactional
	public ResponseDTO setDefaultLabelConfig(Long id) {
		ResponseDTO response = new ResponseDTO();
		LabelConfigEO eo = labelConfigRepository.findById(id).orElse(null);
		if (eo == null) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Label configuration not found for ID: " + id);
			return response;
		}
		if (!"ACTIVE".equalsIgnoreCase(eo.getStatus())) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Cannot set an INACTIVE configuration as default. Activate it first.");
			return response;
		}
		clearExistingDefault();
		eo.setIsDefault(true);
		labelConfigRepository.save(eo);
		logger.info("Label config set as default: id={}, name={}", id, eo.getConfigName());
		response.setResponseStatus(Constants.SUCCESS_STATUS);
		response.setResponseMessage("Label configuration '" + eo.getConfigName() + "' is now the system default");
		return response;
	}

	/** Clears the isDefault flag from any currently-default config. */
	private void clearExistingDefault() {
		labelConfigRepository.findByIsDefaultTrueAndStatus("ACTIVE").ifPresent(existing -> {
			existing.setIsDefault(false);
			labelConfigRepository.save(existing);
		});
	}

	/** Maps a LabelConfigEO to its response DTO. */
	private LabelConfigResponseDTO toLabelConfigDTO(LabelConfigEO eo) {
		return LabelConfigResponseDTO.builder()
			.id(eo.getId())
			.configName(eo.getConfigName())
			.description(eo.getDescription())
			.labelWidthInches(eo.getLabelWidthInches())
			.labelHeightInches(eo.getLabelHeightInches())
			.showLogo(eo.getShowLogo())
			.logoPath(eo.getLogoPath())
			.showBrandName(eo.getShowBrandName())
			.showProductName(eo.getShowProductName())
			.showVariantDetails(eo.getShowVariantDetails())
			.showNetQuantity(eo.getShowNetQuantity())
			.showMfgDate(eo.getShowMfgDate())
			.showExpDate(eo.getShowExpDate())
			.showBatchNo(eo.getShowBatchNo())
			.showMrp(eo.getShowMrp())
			.showBarcode(eo.getShowBarcode())
			.fssaiCode(eo.getFssaiCode())
			.showFssaiCode(eo.getShowFssaiCode())
			.columnsPerRow(eo.getColumnsPerRow())
			.isDefault(eo.getIsDefault())
			.status(eo.getStatus())
			.createdAt(eo.getCreatedAt())
			.createdBy(eo.getCreatedBy())
			.updatedAt(eo.getUpdatedAt())
			.updatedBy(eo.getUpdatedBy())
			.build();
	}

	// ─── PDF Resize for Thermal Label ────────────────────────────────────────

	/**
	 * Downloads a PDF from {@code request.getPdfUrl()}, scales every page to 4" × 6" (288
	 * × 432 pt) — the standard thermal-label size — and writes the result to the public
	 * labels folder.
	 */
	@Override
	public PdfResizeResponseDTO resizePdfForThermalLabel(PdfResizeRequestDTO request) {

		PdfResizeResponseDTO response = new PdfResizeResponseDTO();
		try {
			// ── Validate ──────────────────────────────────────────────────────
			if (request.getPdfUrl() == null || request.getPdfUrl().trim().isEmpty()) {
				response.setStatus(Constants.FAILURE_STATUS);
				response.setMessage("pdfUrl is required");
				return response;
			}

			// ── Download ──────────────────────────────────────────────────────
			byte[] pdfBytes = downloadUrlBytes(request.getPdfUrl().trim());

			// ── Resize & save ─────────────────────────────────────────────────
			String dir = labelPdfDir.endsWith("/") || labelPdfDir.endsWith("\\") ? labelPdfDir : labelPdfDir + "/";
			new File(dir).mkdirs();
			String filename = "resized_" + System.currentTimeMillis() + ".pdf";
			String filePath = dir + filename;

			int pageCount = scalePdfToThermalLabel(pdfBytes, filePath);

			String pdfUrl = "/products/labels/download/" + filename;
			response.setStatus(Constants.SUCCESS_STATUS);
			response.setMessage("PDF resized successfully to 4\"\u00d76\" thermal label format");
			response.setPdfUrl(pdfUrl);
			response.setPageCount(pageCount);

		}
		catch (Exception e) {
			logger.error("Error resizing PDF for thermal label: {}", e.getMessage(), e);
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage("Failed to resize PDF: " + e.getMessage());
		}
		return response;
	}

	/**
	 * Downloads bytes from any HTTP/HTTPS URL with a sensible timeout.
	 */
	private byte[] downloadUrlBytes(String urlStr) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(15_000);
		conn.setReadTimeout(60_000);
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (thermal-label-resizer)");
		conn.setInstanceFollowRedirects(true);
		try (InputStream is = conn.getInputStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			byte[] buf = new byte[8192];
			int read;
			while ((read = is.read(buf)) != -1)
				bos.write(buf, 0, read);
			return bos.toByteArray();
		}
	}

	/**
	 * Rendering DPI for the source-page rasterisation step. 300 DPI gives text that is
	 * crisp at 4"×6" thermal printer resolution (up from the old 203 DPI which produced
	 * blurry fonts).
	 */
	private static final float THERMAL_DPI = 300f;

	/**
	 * Reads a PDF from {@code pdfBytes}, renders every page to a raster image at
	 * {@value #THERMAL_DPI} DPI (native thermal-printer resolution), scales the image to
	 * FILL a 4" × 6" (288 × 432 pt) target page using scale-to-fill mode (largest
	 * possible scale that fills the full label area, centred, aspect ratio preserved —
	 * any overflow is clipped at the page boundary) and saves the result to
	 * {@code outputPath}.
	 *
	 * <p>
	 * Raster rendering ensures fonts are drawn at full resolution regardless of the
	 * source PDF dimensions, making text clear and readable even when the source is
	 * larger than the thermal label (e.g. A4 → 4"×6").
	 * </p>
	 * @return the number of pages in the output PDF
	 */

	private int scalePdfToThermalLabel(byte[] pdfBytes, String outputPath) throws Exception {
		final float TARGET_W = 288f; // 4 inches × 72 pt/inch
		final float TARGET_H = 432f; // 6 inches × 72 pt/inch

		try (PDDocument srcDoc = PDDocument.load(pdfBytes); PDDocument dstDoc = new PDDocument()) {

			// ── 0. Remove unwanted text ("newvsys" in Shipped-By section) ─────
			whiteoutText(srcDoc, "newvsys");

			PDFRenderer renderer = new PDFRenderer(srcDoc);
			renderer.setSubsamplingAllowed(false); // force full-quality render (no pixel
													// skipping)
			int numPages = srcDoc.getNumberOfPages();

			for (int i = 0; i < numPages; i++) {

				// ── 1. Render source page to a raster image at thermal DPI ────────
				BufferedImage raw = renderer.renderImageWithDPI(i, THERMAL_DPI, ImageType.RGB);

				// ── 2. Compute scale-to-FILL factor using source page dimensions ──
				// (preserves aspect ratio; content fills the larger target axis;
				// the smaller axis may overflow slightly — clipped by PDF boundary)
				PDPage srcPage = srcDoc.getPage(i);
				float srcW = srcPage.getMediaBox().getWidth(); // points
				float srcH = srcPage.getMediaBox().getHeight(); // points

				float scaleX = TARGET_W / srcW;
				float scaleH = TARGET_H / srcH;
				float scale = Math.max(scaleX, scaleH); // fill, not fit

				float drawW = srcW * scale;
				float drawH = srcH * scale;
				float offX = (TARGET_W - drawW) / 2f; // negative → clips edge
				float offY = (TARGET_H - drawH) / 2f;

				// ── 3. If the image needs upscaling, use bicubic resampling ───────
				// (downscaling keeps the raw render; upscaling resamples to avoid
				// pixelation when source is smaller than the target)
				BufferedImage finalImg;
				if (scale > 1.05f) {
					int newW = Math.round(raw.getWidth() * scale);
					int newH = Math.round(raw.getHeight() * scale);
					finalImg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
					Graphics2D g2 = finalImg.createGraphics();
					g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
					g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.drawImage(raw, 0, 0, newW, newH, null);
					g2.dispose();
					// once resampled, draw at full target size
					drawW = TARGET_W + Math.abs(offX) * 2;
					drawH = TARGET_H + Math.abs(offY) * 2;
					offX = (TARGET_W - drawW) / 2f;
					offY = (TARGET_H - drawH) / 2f;
				}
				else {
					finalImg = raw;
				}

				// ── 4. Create target 4"×6" page and embed the image ───────────────
				PDRectangle targetRect = new PDRectangle(TARGET_W, TARGET_H);
				PDPage dstPage = new PDPage(targetRect);
				dstDoc.addPage(dstPage);

				PDImageXObject img = LosslessFactory.createFromImage(dstDoc, finalImg);

				try (PDPageContentStream cs = new PDPageContentStream(dstDoc, dstPage)) {
					// White background (ensures thermal printers don't print transparency
					// as black)
					cs.setNonStrokingColor(1f, 1f, 1f);
					cs.addRect(0, 0, TARGET_W, TARGET_H);
					cs.fill();
					// Draw the rendered label image (fill mode — centred, fills the
					// label)
					cs.drawImage(img, offX, offY, drawW, drawH);
				}
			}

			dstDoc.save(outputPath);
			return numPages;
		}
	}

	// ─── Text masking helpers ─────────────────────────────────────────────────

	/**
	 * Scans every page of {@code doc} for all runs containing {@code textToRemove}
	 * (case-insensitive) and paints an opaque white rectangle over each match directly in
	 * the source document's content streams.
	 *
	 * <p>
	 * This modifies {@code doc} in-place before it is rasterised, so the masked text
	 * never appears in the output image.
	 * </p>
	 */
	private void whiteoutText(PDDocument doc, String textToRemove) {
		String targetLower = textToRemove.toLowerCase(java.util.Locale.ROOT);
		try {
			for (int pageIdx = 0; pageIdx < doc.getNumberOfPages(); pageIdx++) {
				PDPage page = doc.getPage(pageIdx);
				float pageHeight = page.getMediaBox().getHeight();

				// Collect bounding boxes of all matching text runs on this page
				PositionCollector collector = new PositionCollector(targetLower, pageHeight);
				collector.setStartPage(pageIdx + 1);
				collector.setEndPage(pageIdx + 1);
				collector.getText(doc);

				List<float[]> boxes = collector.getBoxes();
				if (boxes.isEmpty())
					continue;

				// Append a white-fill pass on top of every match
				try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND,
						true, true)) {
					cs.setNonStrokingColor(1f, 1f, 1f);
					for (float[] b : boxes) {
						cs.addRect(b[0], b[1], b[2], b[3]);
					}
					cs.fill();
				}
				logger.info("whiteoutText: masked {} box(es) of '{}' on page {}", boxes.size(), textToRemove,
						pageIdx + 1);
			}
		}
		catch (Exception e) {
			logger.warn("whiteoutText: could not mask '{}': {}", textToRemove, e.getMessage());
		}
	}

	/**
	 * PDFTextStripper subclass that collects the PDF-coordinate bounding boxes of every
	 * text run containing the given target string.
	 *
	 * <p>
	 * PDFBox returns text positions with the Y axis <em>flipped</em> (origin at the
	 * top-left of the page, Y increases downward — screen coordinates). We convert to
	 * standard PDF coordinates (origin bottom-left, Y up) so the boxes can be used
	 * directly with {@link PDPageContentStream#addRect}.
	 * </p>
	 */
	private static class PositionCollector extends PDFTextStripper {

		private final String target;

		private final float pageHeight;

		private final List<float[]> boxes = new ArrayList<>();

		PositionCollector(String targetLower, float pageHeight) throws java.io.IOException {
			this.target = targetLower;
			this.pageHeight = pageHeight;
			setSortByPosition(true);
		}

		@Override
		protected void writeString(String text, List<TextPosition> positions) throws java.io.IOException {
			super.writeString(text, positions);
			if (positions.isEmpty())
				return;

			String lower = text.toLowerCase(java.util.Locale.ROOT);
			int from = 0;
			while (true) {
				int idx = lower.indexOf(target, from);
				if (idx < 0)
					break;

				int endIdx = Math.min(idx + target.length(), positions.size());
				int startIdx = Math.min(idx, positions.size() - 1);

				TextPosition first = positions.get(startIdx);
				TextPosition last = positions.get(endIdx - 1);

				// ── Screen-space coordinates (origin top-left, Y down) ───────
				// getX() = left edge of the character box
				// getY() = top edge of the character box (Y increases downward)
				// getWidth() = character advance width
				// getHeight() = total character cell height
				float screenX1 = first.getX();
				float screenX2 = last.getX() + last.getWidth();
				float screenY = first.getY(); // top of char in screen space
				float charH = first.getHeight();

				// ── Convert to PDF coordinates (origin bottom-left, Y up) ────
				// pdfY = pageHeight - screenY - charH (bottom of the char cell)
				float padding = charH * 0.4f; // generous padding for safety
				float pdfX = screenX1 - padding;
				float pdfY = pageHeight - screenY - charH - padding;
				float pdfW = (screenX2 - screenX1) + padding * 2f;
				float pdfH = charH * 1.8f + padding * 2f; // 1.8× height covers ascenders
															// + descenders

				boxes.add(new float[] { pdfX, pdfY, pdfW, pdfH });
				from = idx + target.length();
			}
		}

		List<float[]> getBoxes() {
			return boxes;
		}

	}

	// ── Utility helpers ──────────────────────────────────────────────────────

	/**
	 * Normalizes a filesystem path received from client input or configuration:
	 * <ol>
	 * <li>Replaces Windows-style backslashes ({@code \}) with forward slashes so the path
	 * works correctly on Linux / Docker containers.</li>
	 * <li>Prepends a leading {@code /} if the path does not already start with one,
	 * ensuring the path is treated as absolute (e.g. {@code public/logo.png} becomes
	 * {@code /public/logo.png}).</li>
	 * </ol>
	 * @param path the raw path string (may be null or blank)
	 * @return normalized absolute path string, or {@code null} if input is null/blank
	 */
	private String normalizePath(String path) {
		if (path == null || path.trim().isEmpty()) {
			return null;
		}
		String normalized = path.trim().replace('\\', '/');
		if (!normalized.startsWith("/")) {
			normalized = "/" + normalized;
		}
		return normalized;
	}

}
