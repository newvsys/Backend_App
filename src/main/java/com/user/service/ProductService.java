package com.user.service;

import com.user.dto.LabelPrintJobDTO;
import com.user.dto.LabelPrintRequestDTO;
import com.user.dto.LabelPrintResponseDTO;
import com.user.dto.LabelConfigCreateDTO;
import com.user.dto.LabelConfigResponseDTO;
import com.user.dto.PdfResizeRequestDTO;
import com.user.dto.PdfResizeResponseDTO;
import com.user.dto.CategoryCreateDTO;
import com.user.dto.CategoryDTO;
import com.user.dto.ProdVarCreateDTO;
import com.user.dto.ProdVarDTO;
import com.user.dto.ProdVarUpdateDTO;
import com.user.dto.ProductAttributeCreateDTO;
import com.user.dto.ProductAttributeDTO;
import com.user.dto.ProductCreateDTO;
import com.user.dto.ProductDTO;
import com.user.dto.ProductImageDTO;
import com.user.dto.ResponseDTO;
import com.user.dto.UomCreateDTO;
import com.user.dto.UomResponseDTO;
import com.user.dto.UomUpdateDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

	CategoryDTO createProductCat(CategoryCreateDTO categoryCreateDTO, MultipartFile imageFile);

	CategoryDTO updateProductCat(String categoryId, CategoryCreateDTO productCategory, MultipartFile imageFile);

	void deleteCategoriesById(String categoryId);

	List<CategoryDTO> getAllCategories();

	CategoryDTO getCategoryById(String categoryId);

	ProductDTO createProduct(ProductCreateDTO productCreateDTO);

	List<ProductDTO> getAllProducts();

	ProductDTO getProductById(Long productId);

	ProductDTO updateProduct(Integer productId, ProductCreateDTO productUpdateDTO);

	void deleteProductById(Integer productId);

	ProdVarDTO createProductVariant(ProdVarCreateDTO productCreateDTO, List<MultipartFile> images, MultipartFile video,
			int mainImageIndex);

	List<ProdVarDTO> getVariantsByProductId(Long productId);

	ProdVarDTO updateProductVariant(ProdVarUpdateDTO prodVarUpdateDTO, List<MultipartFile> images, MultipartFile video,
			int mainImageIndex);

	void deleteProductVariantById(String variantId);

	List<ProductDTO> searchProduct(String query, List<Long> categoryIds, Boolean inStock, Integer minPrice,
			Integer price, String sort, int page, String rsc, int limit);

	ProductDTO findBySlug(String productSlug);

	List<ProductImageDTO> findProductImageByProductId(String productId);

	ResponseDTO deleteProductImage(Long imageId);

	ResponseDTO deleteProductVideo(Long variantId);

	ResponseDTO setMainImage(Long imageId);

	ProductAttributeDTO createProductAttribute(ProductAttributeCreateDTO productAttributeCreateDTO);

	ProductAttributeDTO updateProductAttribute(Long attributeId, ProductAttributeCreateDTO productAttributeCreateDTO);

	void deleteProductAttribute(Long attributeId);

	List<ProductDTO> getProductsByCategoryId(Integer categoryId);

	// Unit of Measure APIs
	ResponseDTO createUom(UomCreateDTO uomCreateDTO);

	ResponseDTO updateUom(Long uomId, UomUpdateDTO uomUpdateDTO);

	ResponseDTO deleteUom(Long uomId);

	UomResponseDTO getUomById(Long uomId);

	List<UomResponseDTO> getAllUoms();

	LabelPrintResponseDTO generateLabelPdf(LabelPrintRequestDTO request);

	// ─── Label Job History & Reprint ────────────────────────────────────────

	/**
	 * Returns paginated list of all label print jobs. Optionally filtered by batchNo OR a
	 * single barcode value.
	 */
	List<LabelPrintJobDTO> getLabelPrintJobs(String batchNo, String barcode, int page, int limit);

	/** Fetch a single label print job by its ID. */
	LabelPrintJobDTO getLabelPrintJobById(Long jobId);

	/**
	 * Reprint an existing job: regenerates the PDF using the stored brandName +
	 * batchNo/barcodes, saves a new job record, and returns the new PDF URL.
	 */
	LabelPrintResponseDTO reprintLabelJob(Long jobId);

	/**
	 * Downloads a PDF from the given URL, scales every page to fit a 4" × 6" (288 × 432
	 * pt) thermal label, saves it to the public folder, and returns the new PDF URL.
	 */
	PdfResizeResponseDTO resizePdfForThermalLabel(PdfResizeRequestDTO request);

	// ─── Label Config MASTER APIs ───────────────────────────────────────────

	/** Create a new label configuration. */
	LabelConfigResponseDTO createLabelConfig(LabelConfigCreateDTO dto);

	/** Update an existing label configuration by ID. */
	LabelConfigResponseDTO updateLabelConfig(Long id, LabelConfigCreateDTO dto);

	/** Soft-delete a label configuration (sets status = INACTIVE). */
	ResponseDTO deleteLabelConfig(Long id);

	/** Fetch a single label configuration by ID. */
	LabelConfigResponseDTO getLabelConfigById(Long id);

	/**
	 * Fetch all label configurations. Pass "ACTIVE" to get only active ones; pass null
	 * for all.
	 */
	List<LabelConfigResponseDTO> getAllLabelConfigs(String status);

	/**
	 * Marks the given config as the system default (isDefault=true) and clears the flag
	 * from any other config.
	 */
	ResponseDTO setDefaultLabelConfig(Long id);

}
