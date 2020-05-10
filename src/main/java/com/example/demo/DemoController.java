package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
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

    final Logger logger = LoggerFactory.getLogger(DemoController.class);

    private Map checkAuth(String user, String token, String method, String[] pathAsArray) {
        Map<String, Object> inputValue = new HashMap<>();
        inputValue.put("user", user);
        inputValue.put("token", token);
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
        String user = getUserName(request.getUserPrincipal());
        String token = getTokenValue(request.getUserPrincipal());
        String path = request.getRequestURI().substring(1); // remove the leading "/" from the path prior to splitting
        String[] pathAsArray = path.split("/");

        Map<String, Object> response = checkAuth(user, token, request.getMethod(), pathAsArray);
        Map<String, Object> result = (Map<String, Object>) response.get("result");

        // return success/error message per the "allowed" value
        return (Boolean.TRUE.equals(result.get("allowed"))) ?
                String.format("Success: user %s is authorized", user) :
                String.format("Error: user %s is not authorized to %s url /%s", user, request.getMethod(), path);
    }

    @RequestMapping("/employees/**")
    public ArrayList employees(HttpServletRequest request) throws IOException {
        File file = new ClassPathResource("employees.json").getFile();
        Map<String, Object> map = objectMapper.readValue(file, new TypeReference<>() {
        });
        ArrayList employees = (ArrayList) map.get("employees");

        String user = getUserName(request.getUserPrincipal());
        String token = getTokenValue(request.getUserPrincipal());
        String path = request.getRequestURI().substring(1); // remove the leading "/" from the path prior to splitting
        String[] pathAsArray = path.split("/");

        Map<String, Object> response = filterData(user, token, request.getMethod(), pathAsArray, employees);
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        return (ArrayList) result.get("data");
    }

    private Map filterData(String user, String token, String method, String[] pathAsArray, ArrayList data) {
        Map<String, Object> inputValue = new HashMap<>();
        inputValue.put("user", user);
        inputValue.put("token", token);
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

    private String getUserName(Principal principal) {
        String username = principal.getName();
        if (principal instanceof JwtAuthenticationToken) {
            username = (String) ((JwtAuthenticationToken) principal).getTokenAttributes().get("user");
        }
        return username;
    }

    private String getTokenValue(Principal principal) {
        String tokenValue = null;
        if (principal instanceof JwtAuthenticationToken) {
            tokenValue = ((JwtAuthenticationToken) principal).getToken().getTokenValue();
        }
        return tokenValue;
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
