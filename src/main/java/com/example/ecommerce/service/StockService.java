package com.example.ecommerce.service;

public interface StockService {

    void adjust(Long productId, int delta);
}
