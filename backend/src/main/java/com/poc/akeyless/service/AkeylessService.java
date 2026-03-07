package com.poc.akeyless.service;

import com.poc.akeyless.config.AkeylessProperties;
import com.poc.akeyless.model.SecretResponse;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Akeyless Secret Management (POC).
 * Handles Auth Token caching and automatic pathing for 'manishpoc'.
 */
@Service
public class AkeylessService {

    private static final Logger log = LoggerFactory.getLogger(AkeylessService.class);

    private final AkeylessProperties props;
    private final RestTemplate restTemplate;

    // Simple Token Cache
    private String cachedToken;
    private LocalDateTime tokenExpiry;

    public AkeylessService(AkeylessProperties props) {
        this.props = props;
        this.restTemplate = new RestTemplate();
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    public SecretResponse createSecret(String name, String value, String description) {
        String fullPath = sanitizePath(name);
        try {
            String token = getOrRefreshToken();
            Map<String, Object> body = new HashMap<>();
            body.put("token", token);
            body.put("name", fullPath);
            body.put("value", value);
            if (description != null && !description.isBlank()) {
                body.put("description", description);
            }

            post("/create-secret", body);
            return success("Secret '" + fullPath + "' created successfully.");
        } catch (HttpClientErrorException e) {
            String msg = extractAkeylessError(e);
            if (msg.contains("already exists") || e.getStatusCode().value() == 409) {
                return updateSecret(name, value);
            }
            return failure("Failed to create secret: " + msg);
        } catch (Exception e) {
            return failure("Unexpected error: " + e.getMessage());
        }
    }

    public SecretResponse updateSecret(String name, String value) {
        String fullPath = sanitizePath(name);
        try {
            String token = getOrRefreshToken();
            Map<String, Object> body = new HashMap<>();
            body.put("token", token);
            body.put("name", fullPath);
            body.put("value", value);

            post("/set-secret-val", body);
            return success("Secret '" + fullPath + "' updated successfully.");
        } catch (Exception e) {
            return failure("Failed to update: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public SecretResponse getSecret(String name) {
        String fullPath = sanitizePath(name);
        try {
            String token = getOrRefreshToken();
            Map<String, Object> body = new HashMap<>();
            body.put("token", token);
            body.put("names", List.of(fullPath));

            ResponseEntity<Map> response = postForEntity("/get-secret-value", body, Map.class);
            Map<String, String> values = (Map<String, String>) response.getBody();

            String value = (values != null) ? values.get(fullPath) : null;
            SecretResponse resp = success("Secret retrieved.");
            resp.setName(fullPath);
            resp.setValue(value);
            return resp;
        } catch (Exception e) {
            return failure("Failed to retrieve secret: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getAllValues() {
        List<String> names = listSecrets();
        if (names.isEmpty()) return Map.of();

        String token = getOrRefreshToken();
        Map<String, Object> body = new HashMap<>();
        body.put("token", token);
        body.put("names", names);

        ResponseEntity<Map> response = postForEntity("/get-secret-value", body, Map.class);
        Map<?, ?> raw = response.getBody();
        if (raw == null) return Map.of();

        Map<String, String> result = new HashMap<>();
        raw.forEach((k, v) -> result.put(String.valueOf(k), v != null ? String.valueOf(v) : ""));
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<String> listSecrets() {
        try {
            String token = getOrRefreshToken();
            Map<String, Object> body = new HashMap<>();
            body.put("token", token);
            // Remove trailing slash and add recursive flag
            body.put("path", "/manishpoc");
            body.put("recursive", true);
            // Optional: Filter by type if necessary, but keep it broad to debug first
            // body.put("type", "static-secret");

            ResponseEntity<Map> response = postForEntity("/list-items", body, Map.class);
            Map<String, Object> result = response.getBody();

            if (result == null || !result.containsKey("items")) {
                log.warn("No items found in Akeyless response for path /manishpoc");
                return List.of();
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
            return items.stream()
                .map(i -> (String) i.get("item_name")) // Ensure 'item_name' exists in the logs
                .filter(Objects::nonNull)
                .toList();
        } catch (Exception e) {
            log.error("Error listing secrets", e);
            return List.of();
        }
    }

    // ---------------------------------------------------------------
    // Internal Helpers
    // ---------------------------------------------------------------

    private String sanitizePath(String name) {
        // Automatically ensures the secret is stored in /manishpoc/
        if (name.startsWith("/manishpoc/")) return name;
        if (name.startsWith("manishpoc/")) return "/" + name;
        return "/manishpoc/" + (name.startsWith("/") ? name.substring(1) : name);
    }

    private synchronized String getOrRefreshToken() {
        if (cachedToken != null && tokenExpiry != null && LocalDateTime.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        log.info("Fetching new Akeyless Auth Token...");
        Map<String, Object> authBody = new HashMap<>();
        authBody.put("access-type", "api_key");
        authBody.put("access-id", props.getAccessId());
        authBody.put("access-key", props.getAccessKey());

        ResponseEntity<Map> response = postForEntity("/auth", authBody, Map.class);
        Map<String, Object> body = response.getBody();
        
        if (body == null || !body.containsKey("token")) {
            throw new RuntimeException("Auth failed: No token in response");
        }

        this.cachedToken = (String) body.get("token");
        this.tokenExpiry = LocalDateTime.now().plusMinutes(50); // Akeyless tokens usually last 60m
        return cachedToken;
    }

    private <T> ResponseEntity<T> postForEntity(String path, Map<String, Object> body, Class<T> respType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(props.getApiUrl() + path, new HttpEntity<>(body, headers), respType);
    }

    private void post(String path, Map<String, Object> body) {
        postForEntity(path, body, Void.class);
    }

    private String extractAkeylessError(HttpClientErrorException e) {
        String raw = e.getResponseBodyAsString();
        return raw.contains("\"message\"") ? raw.split("\"message\":\"")[1].split("\"")[0] : e.getMessage();
    }

    private SecretResponse success(String msg) { return new SecretResponse(true, msg); }
    private SecretResponse failure(String msg) { return new SecretResponse(false, msg); }
}