package integration.gather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.context.annotation.Configuration;

import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;


/**
 * Daniel Hammann - <hammann@synyx.de>.
 */
@Configuration
public class AbsenceDataService {

    public List<AbsenceResponse> getAbsenceResponsesFromUrlaubsverwaltungRestApi()
        throws com.fasterxml.jackson.core.JsonProcessingException {

        RestTemplate restTemplate = new RestTemplate();

        ObjectNode jsonResponse = restTemplate.getForObject(
                "http://localhost:8080/api/vacations?from=2015-01-01&to=2015-12-31", ObjectNode.class);

        JsonNode vacationsNode = jsonResponse.get("response").get("vacations");

        ObjectMapper objectMapper = new ObjectMapper();
        AbsenceResponse[] absenceResponses = objectMapper.treeToValue(vacationsNode, AbsenceResponse[].class);

        return Arrays.asList(absenceResponses);
    }
}
