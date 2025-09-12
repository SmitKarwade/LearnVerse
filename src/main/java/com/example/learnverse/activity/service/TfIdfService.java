package com.example.learnverse.activity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@EnableCaching
public class TfIdfService {

//    - Text preprocessing (lowercase, remove punctuation, normalize)
//    - TF-IDF vectorization (converts text to numerical vectors)
//    - Cosine similarity calculation (measures text relevance)
//    - Vocabulary management (tracks all unique words)


    private Map<String, Integer> termToIndex = new HashMap<>();
    private Map<String, Integer> documentFrequencies = new HashMap<>();
    private int totalDocuments = 0;

    @Cacheable(value = "activityVectors", key = "#p0", condition = "#p0 != null")
    public double[] vectorizeText(String text) {
        List<String> tokens = preprocess(text);
        Map<String, Integer> termCounts = new HashMap<>();

        for (String token : tokens) {
            termCounts.put(token, termCounts.getOrDefault(token, 0) + 1);
        }

        double[] vector = new double[termToIndex.size()];

        for (Map.Entry<String, Integer> entry : termCounts.entrySet()) {
            String term = entry.getKey();
            if (!termToIndex.containsKey(term)) {
                continue;
            }

            int idx = termToIndex.get(term);
            int tf = entry.getValue();
            int df = documentFrequencies.getOrDefault(term, 1);

            double idf = Math.log(1.0 + (double) totalDocuments / df);
            double tfIdf = tf * idf;

            vector[idx] = tfIdf;
        }

        return vector;
    }

    @Cacheable(value = "tfidfIndex", key = "#p0.hashCode()", condition = "#p0 != null && !#p0.isEmpty()")
    public void buildIndex(List<String> documents) {
        log.info("Building TF-IDF index for {} documents", documents.size());

        termToIndex.clear();
        documentFrequencies.clear();
        totalDocuments = documents.size();

        int termIndex = 0;

        for (String doc : documents) {
            List<String> tokens = preprocess(doc);
            Set<String> uniqueTokens = new HashSet<>(tokens);

            for (String token : uniqueTokens) {
                documentFrequencies.put(token, documentFrequencies.getOrDefault(token, 0) + 1);
                if (!termToIndex.containsKey(token)) {
                    termToIndex.put(token, termIndex++);
                }
            }
        }

        log.info("TF-IDF index built with {} unique terms", termToIndex.size());
    }

    public static double cosineSimilarity(double[] vec1, double[] vec2) {
        if (vec1.length != vec2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            normA += vec1[i] * vec1[i];
            normB += vec2[i] * vec2[i];
        }

        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    private List<String> preprocess(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Lowercase and remove special characters
        text = text.toLowerCase();
        text = text.replaceAll("[^a-zA-Z0-9\\s]", " ");
        text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

        // Split and filter tokens
        List<String> tokens = Arrays.stream(text.split("\\s+"))
                .filter(token -> token.length() > 2)
                .collect(Collectors.toList());

        return tokens;
    }
}

