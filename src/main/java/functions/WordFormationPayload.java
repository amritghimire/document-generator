package functions;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Word formation payload from requests.
 */
public class WordFormationPayload {
    private JsonNode variables;
    private JsonNode parts;
    private JsonNode partsVariables;

    /**
     * Getter for Partials.
     *
     * @return Partials
     */
    public JsonNode getParts() {
        return parts;
    }

    /**
     * Getter for partials data models.
     *
     * @return partials data models
     */
    public JsonNode getPartsVariables() {
        return partsVariables;
    }

    /**
     * Getter for variables.
     *
     * @return variables
     */
    public JsonNode getVariables() {
        return variables;
    }

    /**
     * Setter for partials.
     *
     * @param parts partials
     */
    public void setParts(JsonNode parts) {
        this.parts = parts;
    }

    /**
     * Setter for Partials data models.
     *
     * @param partsVariables Partials data models
     */
    public void setPartsVariables(JsonNode partsVariables) {
        this.partsVariables = partsVariables;
    }

    /**
     * Setter for variables to replace.
     *
     * @param variables variables to replace
     */
    public void setVariables(JsonNode variables) {
        this.variables = variables;
    }
}
