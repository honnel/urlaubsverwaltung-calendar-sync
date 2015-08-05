package integration.gather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;


/**
 * @author  Aljona Murygina - murygina@synyx.de
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbsenceResponse {


    public static final String DATE_FORMAT = "yyyy-MM-dd";

    private String from;

    private String to;

    private BigDecimal dayLength;

    private PersonResponse person;
}
