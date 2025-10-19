package com.example.learnverse.community.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public Map<String, String> uploadFile(MultipartFile file, String folder) {
        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto",
                    "quality", "auto:good",
                    "fetch_format", "auto",
                    "transformation", new Transformation()
                            .width(800).height(600).crop("limit")
                            .quality("auto:good")
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            Map<String, String> result = new HashMap<>();
            result.put("url", uploadResult.get("secure_url").toString());
            result.put("publicId", uploadResult.get("public_id").toString());
            result.put("format", uploadResult.get("format").toString());

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to Cloudinary: " + e.getMessage());
        }
    }

    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }
}