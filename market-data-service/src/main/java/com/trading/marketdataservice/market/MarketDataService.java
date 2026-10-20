package com.trading.marketdataservice.market;
import com.trading.marketdataservice.dto.StockPriceDTO;
import com.trading.marketdataservice.entity.Stock;
import com.trading.marketdataservice.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
@RequiredArgsConstructor
@Order(2)
public class MarketDataService implements CommandLineRunner {
    private final StockRepository stockRepository;
    private final Random random = new Random();
    private final RedisTemplate<String,Object> redisTemplate;
    private static final String PRICE_KEY_PREFIX = "stock_price:";
    private final SimpMessagingTemplate messagingTemplate;
    private static final String PRICE_UPDATED_TOPIC = "stock.price.updated";
    private final KafkaTemplate<String ,Object> kafkaTemplate;

    // concurrent hashmap for thread safety.
    private final Map<String, StockPriceDTO> stockPriceDTOMap =
            new ConcurrentHashMap<>();

    /**
     * initialize stock price.
     * post- construct : runs when context is ready.
     */
//    @PostConstruct
    @Override
    public void run(String ...args){
        log.info("Loading stock data.....");
        List<Stock> stocks = stockRepository.findByActiveTrue();
        if(stocks.isEmpty()){
            log.warn("No active stocks found ");
            return;
        }
        stocks.forEach(stock -> {
            StockPriceDTO stockPriceDTO = new StockPriceDTO();
            stockPriceDTO.setSymbol(stock.getSymbol());
            stockPriceDTO.setCompanyName(stock.getCompanyName());
            stockPriceDTO.setPrice(stock.getInitialPrice());
            stockPriceDTO.setOpen(stock.getInitialPrice());
            stockPriceDTO.setHigh(stock.getInitialPrice());
            stockPriceDTO.setLow(stock.getInitialPrice());
            stockPriceDTO.setChange(BigDecimal.ZERO);
            stockPriceDTO.setChangePercent(BigDecimal.ZERO);
            stockPriceDTO.setVolume(0L);
            stockPriceDTO.setTimestamp(LocalDateTime.now());

            stockPriceDTOMap.put(stock.getSymbol(),stockPriceDTO);
            log.info("Loaded stock {} at {} ",stock.getSymbol(),stock.getInitialPrice());
        });
        log.info("Initialize {} stocks ",stockPriceDTOMap.size());
    }

    /**
     * scheduled price update every 2 second.
     */
    @Scheduled(fixedRateString = "${market.price-update-interval:2000}")
    public void updatePrice() {
        if (stockPriceDTOMap.isEmpty()) {
            log.warn("No stock loaded, skipping price update");
            return;
        }
        stockPriceDTOMap.forEach((symbol, dto) -> {
            BigDecimal oldPrice = dto.getPrice();

            // Random fluctuation in range [-0.25%, +0.25%]
            double changePercentFactor = (random.nextDouble() - 0.5) * 0.5;

            BigDecimal priceDelta = oldPrice.multiply(
                    BigDecimal.valueOf(changePercentFactor / 100)
            );

            // Ensure price never drops below 0.01
            BigDecimal newPrice = oldPrice.add(priceDelta)
                    .max(BigDecimal.valueOf(0.01))
                    .setScale(2, RoundingMode.HALF_UP);

            // Calculate change from Open price
            BigDecimal openPrice = dto.getOpen() != null ? dto.getOpen() : newPrice;
            BigDecimal netChange = newPrice.subtract(openPrice).setScale(2, RoundingMode.HALF_UP);

            BigDecimal netChangePercent = BigDecimal.ZERO;
            if (openPrice.compareTo(BigDecimal.ZERO) > 0) {
                netChangePercent = netChange.divide(openPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            // Update all fields of DTO
            dto.setPrice(newPrice);
            dto.setHigh(dto.getHigh() != null ? dto.getHigh().max(newPrice) : newPrice);
            dto.setLow(dto.getLow() != null ? dto.getLow().min(newPrice) : newPrice);
            dto.setChange(netChange);
            dto.setChangePercent(netChangePercent);
            dto.setVolume(dto.getVolume() != null ? dto.getVolume() + (random.nextInt(50) + 1) : 1L);
            dto.setTimestamp(LocalDateTime.now());

            // cache in redis
            redisTemplate.opsForValue().set(
                    PRICE_KEY_PREFIX + symbol,
                    newPrice
            );
            // broadcast via websocket -> all clients.
            messagingTemplate.convertAndSend("/topic/prices" + symbol,newPrice);
            // publish to kafka
            Map<String ,Object> priceUpdatedEvent = new HashMap<>();
            priceUpdatedEvent.put("symbol",symbol);
            priceUpdatedEvent.put("price",newPrice);
            priceUpdatedEvent.put("timestamp",LocalDateTime.now().toString());

            kafkaTemplate.send(PRICE_UPDATED_TOPIC,symbol,priceUpdatedEvent);

        });
        log.debug("prices updated for {} stocks",stockPriceDTOMap.size());
    }

    /**
     * get current price from redis.
     * @return
     */
    public StockPriceDTO getPrice(String symbol){
        Object cached = redisTemplate.opsForValue()
                .get(PRICE_KEY_PREFIX+symbol);

        if(cached != null){
            return (StockPriceDTO) cached;
        }
        // fallback to memory.
        StockPriceDTO priceDTO = stockPriceDTOMap.get(
                symbol.toUpperCase()
        );
        if(priceDTO == null){
            throw new RuntimeException("Stock not found {} "+symbol);
        }
        return priceDTO;

    }

    /**
     * get all active stock prices.
     * @return
     */
    public List<StockPriceDTO> getAllPrices() {
        return stockPriceDTOMap.values().stream().toList();
    }

    public List<Stock> getAllStocks(){
        return stockRepository.findByActiveTrue();
    }


}
