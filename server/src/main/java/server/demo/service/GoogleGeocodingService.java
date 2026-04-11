package server.demo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import server.demo.util.PriceLabsCountryUtil;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class GoogleGeocodingService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleGeocodingService.class);

    @Value("${geocoding.provider:google}")
    private String provider;

    @Value("${geocoding.google.base-url:https://maps.googleapis.com/maps/api/geocode/json}")
    private String baseUrl;

    @Value("${geocoding.google.api-key:}")
    private String apiKey;

    @Value("${geocoding.google.language:zh-CN}")
    private String language;

    @Value("${geocoding.google.region:}")
    private String configuredRegion;

    @Autowired
    private RestTemplate priceLabsRestTemplate;

    public Optional<double[]> geocodeCoordinates(String city, String state, String address, String countryAlpha3) {
        if (!"google".equalsIgnoreCase(trimToNull(provider))) {
            return Optional.empty();
        }

        String key = trimToNull(apiKey);
        if (key == null) {
            logger.warn("[PriceLabsGeo] Google API key is missing (geocoding.google.api-key)");
            return Optional.empty();
        }

        String query = buildQuery(city, state, address, countryAlpha3);
        if (query == null) {
            return Optional.empty();
        }

        String countryAlpha2 = PriceLabsCountryUtil.normalizeToAlpha2(countryAlpha3);
        String region = trimToNull(configuredRegion);
        if (region == null && countryAlpha2 != null) {
            region = countryAlpha2.toLowerCase(Locale.ROOT);
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("address", query)
                .queryParam("key", key);

        String languageValue = trimToNull(language);
        if (languageValue != null) {
            uriBuilder.queryParam("language", languageValue);
        }
        if (region != null) {
            uriBuilder.queryParam("region", region);
        }
        if (countryAlpha2 != null) {
            uriBuilder.queryParam("components", "country:" + countryAlpha2.toLowerCase(Locale.ROOT));
        }

        URI uri = uriBuilder.build().encode().toUri();

        try {
            ResponseEntity<GoogleGeocodeResponse> resp = priceLabsRestTemplate.getForEntity(uri, GoogleGeocodeResponse.class);
            GoogleGeocodeResponse body = resp.getBody();
            if (body == null) {
                logger.warn("[PriceLabsGeo] Empty geocoding response for query={}", query);
                return Optional.empty();
            }
            if (!"OK".equalsIgnoreCase(body.status)) {
                if (!"ZERO_RESULTS".equalsIgnoreCase(body.status)) {
                    logger.warn("[PriceLabsGeo] Geocoding failed. status={}, error={}, query={}",
                            body.status,
                            body.errorMessage,
                            query);
                }
                return Optional.empty();
            }
            if (body.results == null || body.results.isEmpty() || body.results.get(0).geometry == null || body.results.get(0).geometry.location == null) {
                return Optional.empty();
            }

            Double lat = body.results.get(0).geometry.location.lat;
            Double lng = body.results.get(0).geometry.location.lng;
            if (lat == null || lng == null) {
                return Optional.empty();
            }
            return Optional.of(new double[]{lat, lng});
        } catch (Exception ex) {
            logger.warn("[PriceLabsGeo] Geocoding request failed for query={}: {}", query, ex.getMessage());
            return Optional.empty();
        }
    }

    private String buildQuery(String city, String state, String address, String countryAlpha3) {
        List<String> parts = new ArrayList<>();
        addIfNotBlank(parts, address);
        addIfNotBlank(parts, state);
        addIfNotBlank(parts, city);
        addIfNotBlank(parts, countryAlpha3);

        if (parts.isEmpty()) {
            return null;
        }
        return String.join(", ", parts);
    }

    private void addIfNotBlank(List<String> parts, String value) {
        String normalized = trimToNull(value);
        if (normalized != null) {
            parts.add(normalized);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GoogleGeocodeResponse {
        public String status;

        @JsonProperty("error_message")
        public String errorMessage;

        public List<Result> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Result {
        public Geometry geometry;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Geometry {
        public Location location;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Location {
        public Double lat;
        public Double lng;
    }
}
