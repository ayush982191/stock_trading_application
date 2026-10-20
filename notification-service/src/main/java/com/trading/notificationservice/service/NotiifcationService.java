package com.trading.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotiifcationService {

    @KafkaListener(topics = "order.executed")
    public void consumeOrderExecuted(
            @Payload Map<String ,Object> payload
            ){
        try{
            String userId = (String) payload.get("userId");
            String type = (String) payload.get("type");
            String symbol = (String) payload.get("symbol");
//            String quantity = (String) payload.get("quantity");
            Object quantity = payload.get("quantity");
            Object price = payload.get("price");
            Object totalAmount = payload.get("totalAmount");
            sendNotification(userId,
                    type.equals("BUY") ? "ORDER EXECUTED- BUY"
                            : "ORDER EXECUTED- SELL",
                    String.format("%s %s shared %s at %s Total %s",type,
                            quantity, symbol, price,totalAmount
                            )
                    );



        } catch (Exception e) {
            log.error("Error : ",e.getMessage());
            throw new RuntimeException(e);
        }



    }

    @KafkaListener(topics = "order.failed")
    public void consumeOrderFailed(
            @Payload Map<String ,Object> payload
     ){
        try{
            String userId = (String) payload.get("userId");
            String symbol = (String) payload.get("symbol");
            String reason = (String) payload.get("reason");
            sendNotification(userId,"Order Failed",String.format("your order for %s failed reason is %s",symbol,reason ));

        } catch (Exception e) {
            log.error("Error ",e.getMessage());
            throw new RuntimeException(e);
        }
    }


    @KafkaListener(topics = "user.register")
    public void consumeUserRegister(
            @Payload Map<String ,Object> payload
    ){
        try{
            String userId = (String) payload.get("userId");
            String firstName = (String) payload.get("firstName");
            Object walletBalance = payload.get("walletBalance");
            sendNotification(userId,"Welcome to stock trading plateform",String.format("Welcome %s %s added to your wallet",userId,walletBalance));

        } catch (Exception e) {
            log.error("Error ",e.getMessage());
            throw new RuntimeException(e);
        }
    }




    private void sendNotification(String userId,String title,String message){
        // gmail application type notification
        log.info("");
        log.info("Notification sent");
        log.info("to user {} ",userId);
        log.info("title {}",title);
        log.info("message {}",message);
        log.info("");

    }




}
