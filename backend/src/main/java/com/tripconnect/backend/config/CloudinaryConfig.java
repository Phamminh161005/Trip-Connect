package com.tripconnect.backend.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(@Value("${cloudinary.url}") String cloudinaryUrl) {
        if (cloudinaryUrl == null || !cloudinaryUrl.startsWith("cloudinary://")) {
            throw new IllegalStateException("CLOUDINARY_URL chưa được cấu hình đúng (dạng cloudinary://key:secret@cloud_name)");
        }
        Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
        cloudinary.config.secure = true; // luôn sinh link https
        return cloudinary;
    }
}
