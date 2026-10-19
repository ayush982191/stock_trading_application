package com.trading.portfolioservice.service;
import com.trading.portfolioservice.entity.Holding;
import com.trading.portfolioservice.repository.HoldingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioEventConsumer {
    private final HoldingRepository holdingRepository;

    @KafkaListener(topics = "order.executed")
    public void consumeOrderExecuted(
            @Payload Map<String ,Object> payload
            ){
        try{
            String userId =  (String) payload.get("userId");
            String symbol = (String) payload.get("symbol");
            String type = (String) payload.get("type");
            int quantity = Integer.parseInt(
                    payload.get("quantity").toString()
            );
            BigDecimal price = new BigDecimal(
                    payload.get("price").toString()
            );
            log.info("updating portfolio : user{} {}*{} at {}",
            userId,symbol,quantity,price
            );
            if ("BUY".equals(type)){
                addHolding(userId,symbol,quantity,price);

            }else if("SELL".equals(type)) {
                removeHolding(userId,symbol,quantity,price);
            }


        } catch (Exception e) {
            log.error("Error updating portfolio {}",e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private void removeHolding(String userId, String symbol, int quantity, BigDecimal price) {

    }

    /**
     * add or update after buy order.
     * also calulates new average
     * @param userId
     * @param symbol
     * @param quantity
     * @param price
     */
    private void addHolding(String userId, String symbol, int quantity, BigDecimal price) {
        Holding holding = holdingRepository.
                findByUserIdAndSymbol(userId,symbol)
                .orElse(null);
        if(holding == null){
            // first time buying stock
            holding = new Holding();
            holding.setUserId(userId);
            holding.setSymbol(symbol);
            holding.setCompanyName(symbol);
            holding.setQuantity(quantity);
            holding.setAverageBuyPrice(price);
            holding.setTotalInvested(price.multiply(BigDecimal.valueOf(quantity)));


        }else{
            // already holding this stock
            int newQuantity = holding.getQuantity() + quantity;
            BigDecimal newInvested = holding.getTotalInvested().add(price.multiply(BigDecimal.valueOf(quantity)));
            BigDecimal newAveragePrice = newInvested
                    .divide(BigDecimal.valueOf(newQuantity),2, RoundingMode.HALF_UP);
            holding.setQuantity(newQuantity);
            holding.setTotalInvested(newInvested);
            holding.setAverageBuyPrice(newAveragePrice);



        }
        holdingRepository.save(holding);
        log.info("Holding added");

    }
    private void removeHolding(String userId,String symbol,
                               int quantity
                               ){
        Holding holding = holdingRepository.
                findByUserIdAndSymbol(userId,symbol)
                .orElseThrow(()->new RuntimeException("Holding not found"));
        if (holding.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient share to sell");
        }
        int newQuantity = holding.getQuantity() - quantity;
        if(newQuantity == 0){
            holdingRepository.delete(holding);
            log.info("Holding removed");
        }else{
            BigDecimal newInvested = holding.getAverageBuyPrice()
                    .multiply(BigDecimal.valueOf(newQuantity));
            holding.setQuantity(newQuantity);
            holding.setTotalInvested(newInvested);
            holdingRepository.save(holding);
            log.info("holding removed");
        }

    }


}
