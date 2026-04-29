package com.subscriptiontracker.dto;

import java.util.List;

public class AiRecommendationResponse {
    private List<String> suggestions;

    public AiRecommendationResponse() {}

    public AiRecommendationResponse(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
