package com.example.learnverse.activity.nlp;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class QueryParser {

//    - Natural language parsing using regex patterns
//    - Extracts structured data: distance, price, mode, time preferences
//    - Cleans query text for TF-IDF processing
//    - Handles various input formats gracefully


    // Regex patterns for extracting structured data
    private static final Pattern DISTANCE_PATTERN = Pattern.compile("(?:within|under|less than|max)\\s+(\\d+)\\s*(?:km|kilometer|kilometres)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_PATTERN = Pattern.compile("(?:under|below|less than|max|budget|within)\\s+(\\d+)\\s*(?:inr|rupees?|rs)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MODE_PATTERN = Pattern.compile("\\b(online|offline|hybrid)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DAYS_PATTERN = Pattern.compile("\\b(weekend|weekday|monday|tuesday|wednesday|thursday|friday|saturday|sunday|evening|morning)s?\\b", Pattern.CASE_INSENSITIVE);

    @Data
    public static class ParsedQuery {
        private String queryText;
        private Double distanceKm;
        private Integer priceMax;
        private String mode;
        private String timePreference;
        private String originalText;
    }

    public ParsedQuery parseQuery(String naturalQuery) {
        log.info("Parsing natural query: {}", naturalQuery);

        ParsedQuery parsed = new ParsedQuery();
        parsed.setOriginalText(naturalQuery);

        // Extract distance
        Matcher distanceMatcher = DISTANCE_PATTERN.matcher(naturalQuery);
        if (distanceMatcher.find()) {
            parsed.setDistanceKm(Double.valueOf(distanceMatcher.group(1)));
        }

        // Extract price
        Matcher priceMatcher = PRICE_PATTERN.matcher(naturalQuery);
        if (priceMatcher.find()) {
            parsed.setPriceMax(Integer.valueOf(priceMatcher.group(1)));
        }

        // Extract mode
        Matcher modeMatcher = MODE_PATTERN.matcher(naturalQuery);
        if (modeMatcher.find()) {
            parsed.setMode(modeMatcher.group(1).toLowerCase());
        }

        // Extract time preference
        Matcher daysMatcher = DAYS_PATTERN.matcher(naturalQuery);
        if (daysMatcher.find()) {
            parsed.setTimePreference(daysMatcher.group(1).toLowerCase());
        }

        // Extract query text (remove structured parts)
        String cleanQuery = naturalQuery
                .replaceAll(DISTANCE_PATTERN.pattern(), "")
                .replaceAll(PRICE_PATTERN.pattern(), "")
                .replaceAll(MODE_PATTERN.pattern(), "")
                .replaceAll(DAYS_PATTERN.pattern(), "")
                .replaceAll("\\b(?:find|me|classes|that|are|located|and|made|for|someone|who|has|to|suggest|something|will|make|within|range)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();

        parsed.setQueryText(cleanQuery);

        log.info("Parsed query - Text: '{}', Distance: {}km, Price: ₹{}, Mode: {}, Time: {}",
                parsed.getQueryText(), parsed.getDistanceKm(), parsed.getPriceMax(), parsed.getMode(), parsed.getTimePreference());

        return parsed;
    }
}
