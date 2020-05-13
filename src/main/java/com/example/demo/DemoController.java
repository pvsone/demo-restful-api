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

    @Value("${opa.mainPath}")
    private String opaMainPath;

    @Value("${opa.filterPath}")
    private String opaFilterPath;

    @Value("${opa.tokenPath}")
    private String opaTokenPath;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    final Logger logger = LoggerFactory.getLogger(DemoController.class);

    @RequestMapping("/**")
    public Map main(HttpServletRequest request) {
        Map<String, Object> response = queryOpa(opaMainPath, request, null);
        return (Map<String, Object>) response.get("result");
    }

    @RequestMapping("/employees/**")
    public Map filter(HttpServletRequest request) throws IOException {
        File file = new ClassPathResource("employees.json").getFile();
        Map<String, Object> map = objectMapper.readValue(file, new TypeReference<>() {});
        ArrayList employees = (ArrayList) map.get("employees");

        Map<String, Object> response = queryOpa(opaFilterPath, request, employees);
        return (Map<String, Object>) response.get("result");
    }

    @RequestMapping("/token/**")
    public Map token(HttpServletRequest request) {
        Map<String, Object> response = queryOpa(opaTokenPath, request, null);
        return (Map<String, Object>) response.get("result");
    }

    private Map queryOpa(String opaQueryPath, HttpServletRequest request, ArrayList data) {
        String path = request.getRequestURI().substring(1); // remove the leading "/" from the path prior to splitting
        String[] pathAsArray = path.split("/");

        Map<String, Object> inputValue = new HashMap<>();
        inputValue.put("user", getUserName(request.getUserPrincipal()));
        inputValue.put("jwt", getTokenValue(request.getUserPrincipal()));
        inputValue.put("path", pathAsArray);
        inputValue.put("method", request.getMethod());
        inputValue.put("data", data);
        Map<String, Object> input = Collections.singletonMap("input", inputValue);

        logger.info("Query with input...");
        logger.info(dumpJson(input));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                opaUrl + opaQueryPath, input, Map.class);

        if (response.getStatusCodeValue() >= 300) {
            logger.error("Error in query, got status " + response.getStatusCodeValue());
            return Collections.EMPTY_MAP;
        }

        Map<String, Object> body = response.getBody();
        logger.info("Query response:");
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
