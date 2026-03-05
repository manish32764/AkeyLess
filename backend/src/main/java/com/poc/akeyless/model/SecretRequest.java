package com.poc.akeyless.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SecretRequest {

    /**
     * Akeyless secret path, e.g. /my-app/db-password
     * Must start with /
     */
    @NotBlank(message = "Secret name/path is required")
    @Pattern(regexp = "^/.*", message = "Secret name must start with /")
    private String name;

    @NotBlank(message = "Secret value is required")
    private String value;

    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
