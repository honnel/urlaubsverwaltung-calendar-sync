package integration.gather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;


/**
 * @author  Aljona Murygina - murygina@synyx.de
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PersonResponse {

    private String ldapName;

    private String email;

    private String firstName;

    private String lastName;
}
