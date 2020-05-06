package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objectMapper;
    }
}

@RestController
class DemoController {

    @Value("${opa.url}")
    private String opaUrl;

    @Value("${opa.policyPath}")
    private String opaPolicyPath;

    @Value("${opa.filterPath}")
    private String opaFilterPath;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    Logger logger = LoggerFactory.getLogger(DemoController.class);

    private Map checkAuth(String user, String method, String[] pathAsArray) {
        Map<String, Object> inputValue = new HashMap<>();
        inputValue.put("user", user);
        inputValue.put("path", pathAsArray);
        inputValue.put("method", method);
        Map<String, Object> input = Collections.singletonMap("input", inputValue);

        logger.info("Checking auth...");
        logger.info(dumpJson(input));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                opaUrl + opaPolicyPath, input, Map.class);

        if (response.getStatusCodeValue() >= 300) {
            logger.error("Error checking auth, got status " + response.getStatusCodeValue());
            return Collections.EMPTY_MAP;
        }

        Map<String, Object> body = response.getBody();
        logger.info("Auth response:");
        logger.info(dumpJson(body));
        return body;
    }

    @RequestMapping("/**")
    public String root(HttpServletRequest request) {
        String user = request.getUserPrincipal().getName();
        String path = request.getRequestURI().substring(1); // remove the leading "/" from the path prior to splitting
        String[] pathAsArray = path.split("/");

        Map<String, Object> response = checkAuth(user, request.getMethod(), pathAsArray);
        Map<String, Object> result = (Map<String, Object>) response.get("result");

        // return success/error message per the "allowed" value
        return (Boolean.TRUE.equals((Boolean) result.get("allowed"))) ?
            String.format("Success: user %s is authorized", user) :
            String.format("Error: user %s is not authorized to %s url /%s", user, request.getMethod(), path);
    }

    @RequestMapping("/employees/**")
    public ArrayList employees (HttpServletRequest request) throws IOException {
        File file = new ClassPathResource("employees.json").getFile();
        Map<String, Object> map = objectMapper.readValue(file, new TypeReference<Map<String,Object>>(){});
        ArrayList employees = (ArrayList) map.get("employees");

        String user = request.getUserPrincipal().getName();
        String path = request.getRequestURI().substring(1); // remove the leading "/" from the path prior to splitting
        String[] pathAsArray = path.split("/");

        Map<String, Object> response = filterData(user, request.getMethod(), pathAsArray, employees);
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        return (ArrayList) result.get("data");
    }

    private Map filterData(String user, String method, String[] pathAsArray, ArrayList data) {
        Map<String, Object> inputValue = new HashMap<>();
        inputValue.put("user", user);
        inputValue.put("path", pathAsArray);
        inputValue.put("method", method);
        inputValue.put("data", data);
        Map<String, Object> input = Collections.singletonMap("input", inputValue);

        logger.info("Filtering data...");
        logger.info(dumpJson(input));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                opaUrl + opaFilterPath, input, Map.class);

        if (response.getStatusCodeValue() >= 300) {
            logger.error("Error filtering data, got status " + response.getStatusCodeValue());
            return Collections.EMPTY_MAP;
        }

        Map<String, Object> body = response.getBody();
        logger.info("Filtered response:");
        logger.info(dumpJson(body));
        return body;
    }

    private String dumpJson(Object object) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }
}
