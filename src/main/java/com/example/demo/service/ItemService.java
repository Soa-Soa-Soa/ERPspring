package com.example.demo.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.Item;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ItemService {

    private final RestTemplate restTemplate;

    public ItemService (RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }

    public List<Item> get(String sid) throws Exception{
        String url = "http://erpnext.localhost:8000/api/method/frappe.desk.reportview.get";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Cookie", "sid="+sid); 

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("doctype", "Item");
        requestBody.put("filters", List.of(  ));
        requestBody.put("fields", List.of("item_name"));
        requestBody.put("order_by", "creation desc");
        requestBody.put("limit_page_length", 100);
        requestBody.put("start", 0);

        HttpEntity<Map<String, Object>> arg = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, arg, String.class);

        List<Item> deviss = new ArrayList<>();
        try {
            Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
            Object message = responseBody.get("message");

            if (message instanceof Map) {
                Map<String, Object> messageMap = (Map<String, Object>) message;
                List<List<Object>> values = (List<List<Object>>) messageMap.get("values");

                deviss = values.stream().map(value -> {
                    Item item = new Item();
                    item.setItem_name((String) value.get(0));
                    return item;
                }).toList();
            }

            
        }catch(Exception e){
            throw e;
        }
        return deviss;
    }
}
