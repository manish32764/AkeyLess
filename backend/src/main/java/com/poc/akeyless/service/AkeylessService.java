package com.poc.akeyless.service;

import com.poc.akeyless.config.AkeylessProperties;
import com.poc.akeyless.model.SecretResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that wraps the Akeyless REST API.
 * Docs: https://docs.akeyless.io/reference
 *
 * Authentication: api_key (Access ID + Access Key)
 * A fresh token is obtained before each operation (stateless / POC design).
 */
@Service
public class AkeylessService {

    private static final Logger log = LoggerFactory.getLogger(AkeylessService.class);

    private final AkeylessProperties props;
    private final RestTemplate restTemplate;

    public AkeylessService(AkeylessProperties props) {
        this.props = props;
        this.restTemplate = new RestTemplate();
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    public SecretResponse createSecret(String name, String value, String description) {
        try {
            String token = authenticate();

            Map<String, Object> body = new HashMap<>();
            body.put("token", token);
            body.put("name", name);
            body.put("value", value);
            if (description != null && !description.isBlank()) {
                body.put("description", description);
            }

            post("/create-secret", body);
            return success("Secret '" + name + "' created successfully.");
        } catch (HttpClientErrorException e) {
            String msg = extractAkeylessError(e);
            // If it already exists, try to update instead
            if (msg.contains("already exists") || e.getStatusCode().value() == 409) {
                return updateSecret(name, value);
            }
            log.error("Akeyless create-secret error: {}", msg);
            return failure("Failed to create secret: " + msg);
        } catch (Exception e) {
            log.error("Unexpected error creating secret", e);
            return failure("Unexpected error: " + e.getMessage());
        }
    }

    public SecretResponse updateSecret(String name, String value) {
        try {
            String token = authenticate();

            Map<String, Object> body = new HashMap<>();
            body.put("token", token);
            body.put("name", name);
            body.put("value", value);

            post("/set-secret-val", body);
            return success("Secret '" + name + "' updated successfully.");
        } catch (HttpClientErrorException e) {
            String msg = extractAkeylessError(e);
            log.error("Akeyless set-secret-val error: {}", msg);
            return failure("Failed to update secret: " + msg);
        } catch (Exception e) {
            log.error("Unexpected error updating secret", e);
            return failure("Unexpected error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public SecretResponse getSecret(String name) {
        try {
            String token = authenticate();

            Map<String, Object> body = new HashMap<>();
            body.put("token", token);
            body.put("names", List.of(name));

            ResponseEntity<Map> response = postForEntity("/get-secret-value", body, Map.class);
            Map<String, String> values = (Map<String, String>) response.getBody();

            SecretResponse resp = new SecretResponse(true, "Secret retrieved.");
            resp.setName(name);
            resp.setValue(values != null ? values.get(name) : null);
            return resp;
        } catch (HttpClientErrorException e) {
            String msg = extractAkeylessError(e);
            log.error("Akeyless get-secret-value error: {}", msg);
            return failure("Failed to retrieve secret: " + msg);
        } catch (Exception e) {
            log.error("Unexpected error fetching secret", e);
            return failure("Unexpected error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> listSecrets(String pathPrefix) {
        String token = authenticate();

        Map<String, Object> body = new HashMap<>();
        body.put("token", token);
        body.put("path", pathPrefix == null ? "/" : pathPrefix);
        body.put("type", "static-secret");

        ResponseEntity<Map> response = postForEntity("/list-items", body, Map.class);
        Map<String, Object> result = response.getBody();
        if (result == null) return List.of();

        Object items = result.get("items");
        if (items instanceof List<?> list) {
            return list.stream()
                    .filter(i -> i instanceof Map)
                    .map(i -> (String) ((Map<?, ?>) i).get("item_name"))
                    .filter(n -> n != null)
                    .toList();
        }
        return List.of();
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String authenticate() {
        Map<String, Object> authBody = new HashMap<>();
        authBody.put("access-type", "api_key");
        authBody.put("access-id", props.getAccessId());
        authBody.put("access-key", props.getAccessKey());

        ResponseEntity<Map> response = postForEntity("/auth", authBody, Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("token")) {
            throw new IllegalStateException("Akeyless auth response missing token");
        }
        return (String) body.get("token");
    }

    private void post(String path, Map<String, Object> body) {
        postForEntity(path, body, Void.class);
    }

    private <T> ResponseEntity<T> postForEntity(String path, Map<String, Object> body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(props.getApiUrl() + path, request, responseType);
    }

    private String extractAkeylessError(HttpClientErrorException e) {
        try {
            String raw = e.getResponseBodyAsString();
            // Akeyless error body is typically {"message":"..."}
            if (raw.contains("\"message\"")) {
                int start = raw.indexOf("\"message\"") + 11;
                int end = raw.indexOf("\"", start + 1);
                return raw.substring(start + 1, end);
            }
            return raw;
        } catch (Exception ex) {
            return e.getMessage();
        }
    }

    private SecretResponse success(String message) {
        return new SecretResponse(true, message);
    }

    private SecretResponse failure(String message) {
        return new SecretResponse(false, message);
    }
}
