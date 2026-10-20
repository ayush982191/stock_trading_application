package com.trading.marketdataservice.config;

import com.trading.marketdataservice.entity.Stock;
import com.trading.marketdataservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@Order(2)
public class DataInitializer implements CommandLineRunner {
    private final StockRepository stockRepository;
    @Override
    public void run(String ...args) throws Exception{
        if (stockRepository.count()>0){
            log.info("Stocks already initialized-skipping");
            return;
        }
        log.info("init stock data");
        List<Stock> stocks = new ArrayList<>();

        // Indian Stocks (NSE / INR)
        stocks.add(new Stock("RELIANCE", "Reliance Industries Ltd", new BigDecimal("2950.00"), "NSE", "INR", true));
        stocks.add(new Stock("TCS", "Tata Consultancy Services Ltd", new BigDecimal("4120.00"), "NSE", "INR", true));
        stocks.add(new Stock("HDFCBANK", "HDFC Bank Ltd", new BigDecimal("1680.00"), "NSE", "INR", true));
        stocks.add(new Stock("INFY", "Infosys Ltd", new BigDecimal("1860.00"), "NSE", "INR", true));
        stocks.add(new Stock("TATAMOTORS", "Tata Motors Ltd", new BigDecimal("965.00"), "NSE", "INR", true));

        // US Stocks (NASDAQ / USD)
        stocks.add(new Stock("AAPL", "Apple Inc.", new BigDecimal("230.00"), "NASDAQ", "USD", true));
        stocks.add(new Stock("MSFT", "Microsoft Corporation", new BigDecimal("425.00"), "NASDAQ", "USD", true));
        stocks.add(new Stock("GOOGL", "Alphabet Inc.", new BigDecimal("175.00"), "NASDAQ", "USD", true));
        stocks.add(new Stock("AMZN", "Amazon.com Inc.", new BigDecimal("190.00"), "NASDAQ", "USD", true));
        stocks.add(new Stock("TSLA", "Tesla Inc.", new BigDecimal("220.00"), "NASDAQ", "USD", true));

        stockRepository.saveAll(stocks);
        log.info("Successfully initialized {} stocks", stocks.size());
    }


}
