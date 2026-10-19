package com.trading.orderservice.dto;

import com.trading.orderservice.entity.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private String userId;
    @NotBlank(message = "stock symbol is required")
    private String symbol;
    @NotBlank(message = "order type is required BUY or SELL")
    private OrderType type;
    @Min(value = 1, message = "Quantity must be at least 1 ")
    private Integer quantity;



}
