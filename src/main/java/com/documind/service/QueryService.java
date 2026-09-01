package com.documind.service;

import com.documind.dto.QueryResponse;

public interface QueryService {

    QueryResponse query(
            Long userId,
            String question
    );
}