package vn.poly.bagistore.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Service
public class CloudinaryService {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    private final RestTemplate rest = new RestTemplate();

    public String upload(MultipartFile file) throws Exception {
        if (cloudName == null || cloudName.isBlank() || apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException("Cloudinary not configured on server");
        }

        String uploadUrl = String.format("https://api.cloudinary.com/v1_1/%s/image/upload", cloudName);

        long timestamp = Instant.now().getEpochSecond();
        String toSign = "timestamp=" + timestamp;
        String signature = sha1Hex(toSign + apiSecret);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });
        body.add("api_key", apiKey);
        body.add("timestamp", String.valueOf(timestamp));
        body.add("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

    ResponseEntity<Map> respEntity = rest.postForEntity(new URI(uploadUrl), requestEntity, Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> resp = respEntity.getBody();
        if (resp == null) throw new RuntimeException("Empty response from Cloudinary");
        Object secureObj = resp.get("secure_url");
        if (secureObj == null) secureObj = resp.get("url");
        if (secureObj == null) throw new RuntimeException("Cloudinary response missing secure_url: " + resp);
        return String.valueOf(secureObj);
    }

    private static String sha1Hex(String input) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
