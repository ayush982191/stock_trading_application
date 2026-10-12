package com.trading.marketdataservice.controller;
import com.trading.marketdataservice.dto.StockPriceDTO;
import com.trading.marketdataservice.entity.Stock;
import com.trading.marketdataservice.market.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;


@RestController
@RequestMapping("/api/v1/market")
@Slf4j
@RequiredArgsConstructor
public class MarketDataController {
    private final MarketDataService marketDataService;

    /**
     * get all stock with current prices.
     */
    @GetMapping("/stocks")
    private ResponseEntity<List<StockPriceDTO>> getAllStocks(){
        return ResponseEntity.ok(marketDataService.getAllPrices());
    }

    /**
     * get current price for specific stock
     */
    @GetMapping("/stocks/{symbol}")
    private ResponseEntity<StockPriceDTO> getCurrentStockPrice(
            @PathVariable String symbol
    ){
        return ResponseEntity.ok(marketDataService.getPrice(symbol.toUpperCase()));
    }

    /**
     * get all stocks
     * show which stock are available for trading.
     */
    @GetMapping("/stocks/list")
    public ResponseEntity<List<Stock>> getStockList(){
        return ResponseEntity.ok(marketDataService.getAllStocks());
    }






}
