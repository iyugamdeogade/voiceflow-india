package com.voiceflow.dto;

import java.util.List;

public record HistoryPageResponse(List<HistoryItemResponse> items, int page, int size, long totalItems) {}
