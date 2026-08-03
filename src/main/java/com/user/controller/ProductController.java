package com.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.dto.*;
import com.user.service.ProductService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/products")
public class ProductController {

	private static final Logger logger = LogManager.getLogger(ProductController.class);

	@Value("${label.pdf.dir:/public/labels/}")
	private String labelPdfDir;

	@Autowired
	private ProductService productService;

	// Categories APIs
	@PostMapping(value = "/categories", consumes = { "multipart/form-data" })
	public ResponseEntity<CategoryDTO> createCategory(@RequestPart("category") String categoryJson,
			@RequestPart(value = "image", required = false) MultipartFile imageFile) {
		CategoryDTO createCategory = new CategoryDTO();
		try {
			ObjectMapper mapper = new ObjectMapper();
			CategoryCreateDTO categoryCreateDTO = mapper.readValue(categoryJson, CategoryCreateDTO.class);
			createCategory = productService.createProductCat(categoryCreateDTO, imageFile);
			logger.info("Category created successfully: {}", createCategory);
			return ResponseEntity.ok(createCategory);
		}
		catch (JsonProcessingException e) {
			logger.error("JSON parsing error while creating category: {}", e.getMessage());
			return ResponseEntity.ok(createCategory);
		}
		catch (Exception e) {
			logger.error("Error creating category: {}", e.getMessage());
			return ResponseEntity.ok(createCategory);
		}
	}

	@PutMapping(value = "/categories/{category_id}", consumes = { "multipart/form-data" })
	public ResponseEntity<CategoryDTO> updateCategory(@PathVariable("category_id") String categoryId,
			@RequestPart("category") String categoryJson,
			@RequestPart(value = "image", required = false) MultipartFile imageFile) {
		CategoryDTO updatedCategory = new CategoryDTO();
		try {
			ObjectMapper mapper = new ObjectMapper();
			CategoryCreateDTO categoryCreateDTO = mapper.readValue(categoryJson, CategoryCreateDTO.class);
			updatedCategory = productService.updateProductCat(categoryId, categoryCreateDTO, imageFile);
			logger.info("Category updated successfully: {}", updatedCategory);
			return ResponseEntity.ok(updatedCategory);
		}
		catch (JsonProcessingException e) {
			logger.error("JSON parsing error while updating category: {}", e.getMessage());
			return ResponseEntity.ok(updatedCategory);

		}
		catch (Exception e) {
			logger.error("Error updating category: {}", e.getMessage());
			return ResponseEntity.ok(updatedCategory);
		}
	}

	@DeleteMapping("/categories/{category_id}")
	public ResponseEntity<Void> deleteCategories(@PathVariable("category_id") String categoryId) {
		productService.deleteCategoriesById(categoryId);
		logger.info("Category deleted successfully, ID: {}", categoryId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/categories")
	public ResponseEntity<List<CategoryDTO>> getAllCategories() {
		List<CategoryDTO> categories = new ArrayList<>();
		try {
			categories = productService.getAllCategories();
			logger.info("Fetched all categories, count: {}", categories.size());
		}
		catch (Exception e) {
			logger.error("Error fetching all categories", e);
		}
		return ResponseEntity.ok()
			.cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
			.body(categories);
	}

	@GetMapping("/categories/{category_id}")
	public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable("category_id") String categoryId) {
		try {
			CategoryDTO category = productService.getCategoryById(categoryId);
			logger.info("Fetched category by ID: {}, category: {}", categoryId, category);
			return ResponseEntity.ok(category);
		}
		catch (Exception e) {
			logger.error("Error fetching category by ID: {}", categoryId, e);
			return ResponseEntity.ok(null);
		}
	}

	// Product APIs
	@PostMapping(value = "/product", consumes = { "application/json" })
	public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductCreateDTO productCreateDTO) {
		ProductDTO createProduct = null;
		try {
			createProduct = productService.createProduct(productCreateDTO);
			logger.info("Product created successfully: {}", createProduct);
			return ResponseEntity.ok(createProduct);
		}
		catch (Exception e) {
			logger.error("Error creating product: {}", e.getMessage(), e);
			return ResponseEntity.status(500).body(createProduct);
		}
	}

	@GetMapping("/product")
	public ResponseEntity<List<ProductDTO>> getAllProducts() {
		List<ProductDTO> products = new ArrayList<>();
		try {
			logger.info("Received getAllProducts request");
			products = productService.getAllProducts();
			logger.info("Fetched all products, count: {}", products.size());
		}
		catch (Exception e) {
			logger.error("Error fetching all products: {}", e.getMessage(), e);
		}
		return ResponseEntity.ok(products);
	}

	@GetMapping("/product/{product_id}")
	public ResponseEntity<ProductDTO> getProductById(@PathVariable("product_id") Long productId) {
		try {
			logger.info("Received getProductById request for productId={}", productId);
			ProductDTO product = productService.getProductById(productId);
			if (product == null) {
				logger.warn("Product not found for productId={}", productId);
				return ResponseEntity.notFound().build();
			}
			logger.info("Fetched product details for productId={}", productId);
			return ResponseEntity.ok(product);
		}
		catch (Exception e) {
			logger.error("Error fetching product details for productId={}: {}", productId, e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	@PutMapping("/{product_id}")
	public ResponseEntity<ProductDTO> updateProduct(@PathVariable("product_id") Integer productId,
			@RequestBody ProductCreateDTO productUpdateDTO) {
		ProductDTO product = productService.updateProduct(productId, productUpdateDTO);
		logger.info("Product updated successfully: {}", product);
		return ResponseEntity.ok(product);
	}

	@DeleteMapping("/{product_id}")
	public ResponseEntity<Void> deleteProduct(@PathVariable("product_id") Integer productId) {
		productService.deleteProductById(productId);
		logger.info("Product deleted successfully, ID: {}", productId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping(value = "/productsVariant", consumes = { "multipart/form-data" })
	public ResponseEntity<ProdVarDTO> productsVariant(@RequestPart("productVariant") String productJson,
			@RequestParam(value = "images", required = false) List<MultipartFile> images,
			@RequestParam(value = "video", required = false) MultipartFile video,
			@RequestParam(value = "mainImageIndex", required = false, defaultValue = "0") int mainImageIndex) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			ProdVarCreateDTO prodVarCreateDTO = mapper.readValue(productJson, ProdVarCreateDTO.class);
			ProdVarDTO createProduct = productService.createProductVariant(prodVarCreateDTO, images, video,
					mainImageIndex);
			logger.info("Product variant created successfully: {}", createProduct);
			return ResponseEntity.ok(createProduct);
		}
		catch (JsonProcessingException e) {
			logger.error("JSON parsing error while creating product variant: {}", e.getMessage());
			throw new RuntimeException("Invalid JSON for product", e);
		}
		catch (Exception e) {
			logger.error("Error creating product variant: {}", e.getMessage());
			throw e;
		}
	}

	@PutMapping(value = "/productsVariant", consumes = { "multipart/form-data" })
	public ResponseEntity<ProdVarDTO> updateProductsVariant(@RequestPart("productVariant") String productJson,
			@RequestParam(value = "images", required = false) List<MultipartFile> images,
			@RequestParam(value = "video", required = false) MultipartFile video,
			@RequestParam(value = "mainImageIndex", required = false, defaultValue = "0") int mainImageIndex) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			ProdVarUpdateDTO prodVarUpdateDTO = mapper.readValue(productJson, ProdVarUpdateDTO.class);
			ProdVarDTO updatedProductVariant = productService.updateProductVariant(prodVarUpdateDTO, images, video,
					mainImageIndex);
			logger.info("Product variant updated successfully: {}", updatedProductVariant);
			return ResponseEntity.ok(updatedProductVariant);
		}
		catch (JsonProcessingException e) {
			logger.error("JSON parsing error while updating product variant: {}", e.getMessage());
			throw new RuntimeException("Invalid JSON for product variant", e);
		}
		catch (Exception e) {
			logger.error("Error updating product variant: {}", e.getMessage());
			throw e;
		}
	}

	@DeleteMapping("/productsVariant/{variant_id}")
	public ResponseEntity<Void> deleteProductsVariant(@PathVariable("variant_id") String variantId) {
		productService.deleteProductVariantById(variantId);
		logger.info("Product variant deleted successfully, ID: {}", variantId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/productsVariant/{product_id}")
	public ResponseEntity<List<ProdVarDTO>> getVariantsByProductId(@PathVariable("product_id") Long productId) {
		List<ProdVarDTO> variants = new ArrayList<>();
		try {
			logger.info("Received getVariantsByProductId request for productId={}", productId);
			variants = productService.getVariantsByProductId(productId);
			logger.info("Fetched {} variants for productId={}", variants.size(), productId);
		}
		catch (Exception e) {
			logger.error("Error fetching variants for productId={}: {}", productId, e.getMessage(), e);
		}
		return ResponseEntity.ok(variants);
	}

	@GetMapping("/search")
	public ResponseEntity<List<ProductDTO>> searchProduct(@RequestParam(required = false) String query,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) List<Long> categoryId, @RequestParam(required = false) Boolean inStock,
			@RequestParam(required = false) Integer minPrice, @RequestParam(required = false) Integer price,
			@RequestParam(required = false, defaultValue = "defaultSort") String sort,
			@RequestParam(required = false, defaultValue = "1") int page,
			@RequestParam(required = false, defaultValue = "5") int limit,
			@RequestParam(required = false, name = "_rsc") String rsc) {
		// Accept both ?query= and ?search= — front-end sends ?search=, legacy clients use ?query=
		String effectiveQuery = (query != null && !query.isBlank()) ? query
				: (search != null && !search.isBlank()) ? search : null;
		logger.debug(
				"Invoked searchProduct with effectiveQuery: {}, categoryId: {}, inStock: {}, minPrice: {}, price: {}, sort: {}, page: {}, limit: {}, rsc: {}",
				effectiveQuery, categoryId, inStock, minPrice, price, sort, page, limit, rsc);
		List<ProductDTO> products = new ArrayList<>();
		try {
			products = productService.searchProduct(effectiveQuery, categoryId, inStock, minPrice, price, sort, page,
					rsc, limit);
			logger.info("Fetched products, count: {}", products.size());
		}
		catch (Exception e) {
			logger.error("Error fetching products. Error: {}", e.getMessage(), e);
		}
		return ResponseEntity.ok(products);
	}

	@GetMapping("/productSlug/{productSlug}")
	public ResponseEntity<ProductDTO> getProductBySlug(@PathVariable String productSlug) {

		try {
			ProductDTO product = productService.findBySlug(productSlug);
			logger.info("Fetched product by slug: {}, product: {}", productSlug, product);
			return ResponseEntity.ok(product);
		}
		catch (Exception e) {
			logger.error("Error fetching product by slug: {}. Error: {}", productSlug, e.getMessage(), e);
			return ResponseEntity.ok(null);
		}
	}

	@GetMapping("/productImage/{productId}")
	public ResponseEntity<List<ProductImageDTO>> getImagesByProductId(@PathVariable String productId) {
		List<ProductImageDTO> images = productService.findProductImageByProductId(productId);
		logger.info("Fetched images by product ID: {}, count: {}", productId, images.size());
		return ResponseEntity.ok(images);
	}

	@DeleteMapping("/productImage/{image_id}")
	public ResponseEntity<ResponseDTO> deleteProductImage(@PathVariable("image_id") Long imageId) {
		ResponseDTO response = productService.deleteProductImage(imageId);
		logger.info("Delete product image imageId={}: {}", imageId, response);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/productsVariant/{variant_id}/video")
	public ResponseEntity<ResponseDTO> deleteProductVideo(@PathVariable("variant_id") Long variantId) {
		ResponseDTO response = productService.deleteProductVideo(variantId);
		logger.info("Delete video for variantId={}: {}", variantId, response);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/productImage/{image_id}/main")
	public ResponseEntity<ResponseDTO> setMainImage(@PathVariable("image_id") Long imageId) {
		ResponseDTO response = productService.setMainImage(imageId);
		logger.info("Set main image for imageId={}: {}", imageId, response);
		return ResponseEntity.ok(response);
	}

	// API for product details
	@PostMapping("/productAttributes")
	public ResponseEntity<ProductAttributeDTO> createProductAttribute(
			@RequestBody ProductAttributeCreateDTO productAttributeCreateDTO) {
		ProductAttributeDTO createdAttribute = productService.createProductAttribute(productAttributeCreateDTO);
		logger.info("Product attribute created successfully: {}", createdAttribute);
		return ResponseEntity.ok(createdAttribute);
	}

	@PutMapping("/productAttributes/{attributeId}")
	public ResponseEntity<ProductAttributeDTO> updateProductAttribute(@PathVariable Long attributeId,
			@RequestBody ProductAttributeCreateDTO productAttributeCreateDTO) {
		ProductAttributeDTO updatedAttribute = productService.updateProductAttribute(attributeId,
				productAttributeCreateDTO);
		logger.info("Product attribute updated successfully: {}", updatedAttribute);
		return ResponseEntity.ok(updatedAttribute);
	}

	@DeleteMapping("/productAttributes/{attributeId}")
	public ResponseEntity<Void> deleteProductAttribute(@PathVariable Long attributeId) {
		productService.deleteProductAttribute(attributeId);
		logger.info("Product attribute deleted successfully, ID: {}", attributeId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/categories/{category_id}/products")
	public ResponseEntity<List<ProductDTO>> getActiveProductsByCategoryId(
			@PathVariable("category_id") Integer categoryId) {
		List<ProductDTO> products = new ArrayList<>();
		try {
			logger.info("Received getActiveProductsByCategoryId request for categoryId={}", categoryId);
			products = productService.getProductsByCategoryId(categoryId);
		}
		catch (Exception e) {
			logger.error("Error fetching active products for categoryId={}", categoryId, e);
		}
		return ResponseEntity.ok(products);
	}

	// ─── Unit of Measure APIs ────────────────────────────────────────────────────

	@PostMapping("/uom")
	public ResponseEntity<ResponseDTO> createUom(@RequestBody UomCreateDTO uomCreateDTO) {
		ResponseDTO response = productService.createUom(uomCreateDTO);
		logger.info("Create UOM response: {}", response);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/uom/{uom_id}")
	public ResponseEntity<ResponseDTO> updateUom(@PathVariable("uom_id") Long uomId,
			@RequestBody UomUpdateDTO uomUpdateDTO) {
		ResponseDTO response = productService.updateUom(uomId, uomUpdateDTO);
		logger.info("Update UOM {} response: {}", uomId, response);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/uom/{uom_id}")
	public ResponseEntity<ResponseDTO> deleteUom(@PathVariable("uom_id") Long uomId) {
		ResponseDTO response = productService.deleteUom(uomId);
		logger.info("Delete UOM {} response: {}", uomId, response);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/uom/{uom_id}")
	public ResponseEntity<UomResponseDTO> getUomById(@PathVariable("uom_id") Long uomId) {
		UomResponseDTO uom = productService.getUomById(uomId);
		if (uom == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(uom);
	}

	@GetMapping("/uom")
	public ResponseEntity<List<UomResponseDTO>> getAllUoms() {
		List<UomResponseDTO> uoms = new ArrayList<>();
		try {
			uoms = productService.getAllUoms();
			logger.info("Fetched all UOMs, count: {}", uoms.size());
		}
		catch (Exception e) {
			logger.error("Error fetching all UOMs: {}", e.getMessage(), e);
		}
		return ResponseEntity.ok(uoms);
	}

	// ─── Label Printing API ───────────────────────────────────────────────────

	/**
	 * POST /api/products/labels/print Generates a PDF with product labels for the given
	 * batch/barcodes.
	 */
	@PostMapping(value = "/labels/print", consumes = { "application/json" })
	public ResponseEntity<LabelPrintResponseDTO> printLabels(@RequestBody LabelPrintRequestDTO request) {
		LabelPrintResponseDTO response = productService.generateLabelPdf(request);
		logger.info("Label print request processed: labelCount={}, pdfUrl={}", response.getLabelCount(),
				response.getPdfUrl());
		return ResponseEntity.ok(response);
	}

	/**
	 * GET /api/products/labels/jobs Lists label print job history, newest first. Optional
	 * filters: batchNo, barcode (single value). Paginated.
	 */
	@GetMapping("/labels/jobs")
	public ResponseEntity<List<LabelPrintJobDTO>> getLabelJobs(
			@RequestParam(value = "batchNo", required = false) String batchNo,
			@RequestParam(value = "barcode", required = false) String barcode,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "limit", defaultValue = "20") int limit) {
		List<LabelPrintJobDTO> jobs = productService.getLabelPrintJobs(batchNo, barcode, page, limit);
		logger.info("Fetched {} label print jobs (batchNo={}, barcode={}, page={}, limit={})", jobs.size(), batchNo,
				barcode, page, limit);
		return ResponseEntity.ok(jobs);
	}

	/**
	 * GET /api/products/labels/jobs/{jobId} Returns a single label print job by its ID
	 * (includes pdfUrl for re-download).
	 */
	@GetMapping("/labels/jobs/{jobId}")
	public ResponseEntity<?> getLabelJobById(@PathVariable("jobId") Long jobId) {
		LabelPrintJobDTO job = productService.getLabelPrintJobById(jobId);
		if (job == null) {
			return ResponseEntity.status(404).body(Map.of("error", "Label print job not found for ID: " + jobId));
		}
		return ResponseEntity.ok(job);
	}

	/**
	 * POST /api/products/labels/jobs/{jobId}/reprint Regenerates the PDF for an existing
	 * job using its stored parameters and saves a new job record.
	 */
	@PostMapping("/labels/jobs/{jobId}/reprint")
	public ResponseEntity<LabelPrintResponseDTO> reprintLabelJob(@PathVariable("jobId") Long jobId) {
		LabelPrintResponseDTO response = productService.reprintLabelJob(jobId);
		logger.info("Reprint job={}: status={}, pdfUrl={}", jobId, response.getStatus(), response.getPdfUrl());
		return ResponseEntity.ok(response);
	}

	/**
	 * POST /api/products/labels/resize-pdf
	 *
	 * Downloads an external PDF from the supplied URL, scales every page to fit a 4" × 6"
	 * (288 × 432 pt) thermal label, stores the resized PDF in the public folder, and
	 * returns the new PDF URL.
	 *
	 * Request body (JSON): { "pdfUrl":
	 * "https://sr-core-cdn.shiprocket.in/label/s/.../xxx.pdf" }
	 */
	@PostMapping(value = "/labels/resize-pdf", consumes = { "application/json" })
	public ResponseEntity<PdfResizeResponseDTO> resizePdfForThermalLabel(@RequestBody PdfResizeRequestDTO request) {
		PdfResizeResponseDTO response = productService.resizePdfForThermalLabel(request);
		logger.info("PDF resize: sourceUrl={}, status={}, newUrl={}, pages={}", request.getPdfUrl(),
				response.getStatus(), response.getPdfUrl(), response.getPageCount());
		return ResponseEntity.ok(response);
	}

	/**
	 * GET /api/products/labels/download/{filename} Streams a generated label PDF from the
	 * /public/labels/ directory.
	 */
	@GetMapping("/labels/download/{filename:.+}")
	public ResponseEntity<Resource> downloadLabelPdf(@PathVariable String filename) {
		String dir = labelPdfDir.endsWith("/") || labelPdfDir.endsWith("\\") ? labelPdfDir : labelPdfDir + "/";
		File file = new File(dir + filename);
		if (!file.exists() || !file.isFile()) {
			logger.warn("Label PDF not found: {}", file.getAbsolutePath());
			return ResponseEntity.notFound().build();
		}
		Resource resource = new FileSystemResource(file);
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_PDF)
			.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
			.body(resource);
	}

	// ─── Label Configuration MASTER APIs ──────────────────────────────────────

	/**
	 * POST /api/products/labels/config Creates a new label configuration master record.
	 */
	@PostMapping(value = "/labels/config", consumes = { "application/json" })
	public ResponseEntity<?> createLabelConfig(@RequestBody LabelConfigCreateDTO dto) {
		try {
			LabelConfigResponseDTO response = productService.createLabelConfig(dto);
			logger.info("Label config created: id={}, name={}", response.getId(), response.getConfigName());
			return ResponseEntity.ok(response);
		}
		catch (IllegalArgumentException e) {
			logger.warn("Create label config validation error: {}", e.getMessage());
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * PUT /api/products/labels/config/{configId} Updates an existing label configuration.
	 */
	@PutMapping(value = "/labels/config/{configId}", consumes = { "application/json" })
	public ResponseEntity<?> updateLabelConfig(@PathVariable("configId") Long configId,
			@RequestBody LabelConfigCreateDTO dto) {
		try {
			LabelConfigResponseDTO response = productService.updateLabelConfig(configId, dto);
			logger.info("Label config updated: id={}, name={}", configId, response.getConfigName());
			return ResponseEntity.ok(response);
		}
		catch (IllegalArgumentException e) {
			logger.warn("Update label config validation error: {}", e.getMessage());
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * DELETE /api/products/labels/config/{configId} Soft-deletes a label configuration
	 * (status → INACTIVE).
	 */
	@DeleteMapping("/labels/config/{configId}")
	public ResponseEntity<ResponseDTO> deleteLabelConfig(@PathVariable("configId") Long configId) {
		ResponseDTO response = productService.deleteLabelConfig(configId);
		logger.info("Delete label config {}: {}", configId, response.getResponseStatus());
		return ResponseEntity.ok(response);
	}

	/**
	 * GET /api/products/labels/config/{configId} Returns a single label configuration by
	 * ID.
	 */
	@GetMapping("/labels/config/{configId}")
	public ResponseEntity<?> getLabelConfigById(@PathVariable("configId") Long configId) {
		LabelConfigResponseDTO config = productService.getLabelConfigById(configId);
		if (config == null) {
			return ResponseEntity.status(404)
				.body(Map.of("error", "Label configuration not found for ID: " + configId));
		}
		return ResponseEntity.ok(config);
	}

	/**
	 * GET /api/products/labels/config Lists all label configurations. Optional query
	 * param: status=ACTIVE (to return only active ones)
	 */
	@GetMapping("/labels/config")
	public ResponseEntity<List<LabelConfigResponseDTO>> getAllLabelConfigs(
			@RequestParam(value = "status", required = false) String status) {
		List<LabelConfigResponseDTO> configs = productService.getAllLabelConfigs(status);
		logger.info("Fetched {} label configs (status={})", configs.size(), status);
		return ResponseEntity.ok(configs);
	}

	/**
	 * PUT /api/products/labels/config/{configId}/set-default Marks the given
	 * configuration as the system default. Any previously-default config is automatically
	 * demoted.
	 */
	@PutMapping("/labels/config/{configId}/set-default")
	public ResponseEntity<ResponseDTO> setDefaultLabelConfig(@PathVariable("configId") Long configId) {
		ResponseDTO response = productService.setDefaultLabelConfig(configId);
		logger.info("Set default label config {}: {}", configId, response.getResponseStatus());
		return ResponseEntity.ok(response);
	}

}