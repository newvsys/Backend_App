package com.user.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Central service for uploading and deleting media assets on Cloudinary.
 * <p>
 * Folder scheme:
 * <ul>
 *   <li>{@code kuchimittai/categories} – category images</li>
 *   <li>{@code kuchimittai/products}   – product images &amp; videos</li>
 *   <li>{@code kuchimittai/labels}     – generated label PDFs (resource_type=raw)</li>
 *   <li>{@code kuchimittai/returns}    – return-order images</li>
 * </ul>
 */
@Service
public class CloudinaryMediaService {

	private static final Logger logger = LogManager.getLogger(CloudinaryMediaService.class);

	@Autowired
	private Cloudinary cloudinary;

	/**
	 * Upload an image {@link MultipartFile} to Cloudinary.
	 *
	 * @param file   the image file to upload
	 * @param folder target Cloudinary folder (e.g. {@code "kuchimittai/products"})
	 * @return map containing {@code "secure_url"} and {@code "public_id"}
	 */
	public Map<String, String> uploadImage(MultipartFile file, String folder) throws IOException {
		Map<?, ?> result = cloudinary.uploader()
			.upload(file.getBytes(), ObjectUtils.asMap("folder", folder, "resource_type", "image"));
		return Map.of("secure_url", (String) result.get("secure_url"), "public_id",
				(String) result.get("public_id"));
	}

	/**
	 * Upload a video {@link MultipartFile} to Cloudinary.
	 *
	 * @param file   the video file to upload
	 * @param folder target Cloudinary folder
	 * @return map containing {@code "secure_url"} and {@code "public_id"}
	 */
	public Map<String, String> uploadVideo(MultipartFile file, String folder) throws IOException {
		Map<?, ?> result = cloudinary.uploader()
			.upload(file.getBytes(), ObjectUtils.asMap("folder", folder, "resource_type", "video"));
		return Map.of("secure_url", (String) result.get("secure_url"), "public_id",
				(String) result.get("public_id"));
	}

	/**
	 * Upload raw bytes (e.g. a generated PDF) to Cloudinary.
	 *
	 * @param bytes        file bytes
	 * @param folder       target Cloudinary folder
	 * @param resourceType {@code "raw"} for PDFs, {@code "image"} for images,
	 *                     {@code "video"} for videos
	 * @return map containing {@code "secure_url"} and {@code "public_id"}
	 */
	public Map<String, String> uploadBytes(byte[] bytes, String folder, String resourceType) throws IOException {
		Map<?, ?> result = cloudinary.uploader()
			.upload(bytes, ObjectUtils.asMap("folder", folder, "resource_type", resourceType));
		return Map.of("secure_url", (String) result.get("secure_url"), "public_id",
				(String) result.get("public_id"));
	}

	/**
	 * Delete a Cloudinary asset by its {@code public_id}. Best-effort — logs a warning on
	 * failure but does not throw.
	 *
	 * @param publicId     Cloudinary public_id of the asset
	 * @param resourceType {@code "image"}, {@code "video"}, or {@code "raw"}
	 */
	public void delete(String publicId, String resourceType) {
		if (publicId == null || publicId.isBlank()) {
			return;
		}
		try {
			cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
			logger.info("Deleted Cloudinary resource: publicId={}, type={}", publicId, resourceType);
		}
		catch (Exception e) {
			logger.warn("Failed to delete Cloudinary resource publicId={}: {}", publicId, e.getMessage());
		}
	}

}


