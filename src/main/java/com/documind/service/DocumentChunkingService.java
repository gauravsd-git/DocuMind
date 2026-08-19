package com.documind.service;

import java.util.List;

public interface DocumentChunkingService {

    List<String> chunk(String text);
}