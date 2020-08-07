package functions;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.VelocityContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;


/**
 * Class to parse the template.
 */
public class TemplatePartials {
    private HashMap<String, InputStream> uploads;
    private VelocityContext dataModel;
    private JsonNode parts;
    private HashMap<String, String> partials;

    /**
     * Constructor to initialize the velocity.
     */
    public TemplatePartials() {
        Velocity.init();
        partials = new HashMap<>();
        uploads = new HashMap<>();
    }

    /**
     * Constructor with parts and other data from requests.
     *
     * @param parts          - Partials in document.
     * @param partsVariables - Data model for partials.
     * @param uploads        - Uploaded partial templates.
     */
    public TemplatePartials(JsonNode parts, JsonNode partsVariables, HashMap<String, InputStream> uploads) {
        this();
        this.parts = parts;
        this.uploads = uploads;
        dataModel = JsonNodeToHashMap(partsVariables);
    }

    /**
     * Getter for Partials.
     *
     * @return Partials
     */
    public HashMap<String, String> getPartials() {
        return partials;
    }

    /**
     * Convert the JSON Node to hash map for data model.
     * @param node - Partial JSON node
     * @return Velocity context from data model.
     */
    private VelocityContext JsonNodeToHashMap(JsonNode node) {
        VelocityContext root = new VelocityContext();

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode value = field.getValue();
            switch (value.getNodeType()) {
                case NUMBER:
                    root.put(key, value.asDouble());
                    break;
                case BOOLEAN:
                    root.put(key, value.asBoolean());
                    break;
                case OBJECT:
                    root.put(key, JsonNodeToHashMap(node));
                    break;
                case STRING:
                    root.put(key, value.asText());
                    break;
                case ARRAY:
                    ArrayNode arrayNode = (ArrayNode) value;
                    ArrayList<VelocityContext> listOfNodes = new ArrayList<>();
                    for (JsonNode arrayElement : arrayNode) {
                        listOfNodes.add(JsonNodeToHashMap(arrayElement));
                    }
                    root.put(key, listOfNodes.iterator());
                    break;
            }
        }
        return root;
    }

    /**
     * Process the provided template using velocity.
     *
     * @return Hash map for template name and html.
     * @throws IOException Input output exception while reading template.
     */
    public HashMap<String, String> processTemplates() throws IOException {
        Iterator<String> partsIterator = parts.fieldNames();
        while (partsIterator.hasNext()) {
            String singlePart = partsIterator.next();
            String templateString;
            String partialValue = parts.get(singlePart).asText("");
            if (partialValue.startsWith("file:")) {
                InputStream inputStream = uploads.get(partialValue.substring(5));
                templateString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            } else {
                templateString = partialValue;
            }
            StringWriter stringWriter = new StringWriter();
            Velocity.evaluate(dataModel, stringWriter, singlePart, templateString);
            String wrappedTemplate = StringEscapeUtils.unescapeHtml4("<div>" + stringWriter.toString() + "</div>");
            wrappedTemplate = wrappedTemplate.replaceAll("&", "&amp;");
            partials.put(singlePart, wrappedTemplate);
        }
        return getPartials();
    }

    /**
     * Setter for Partials.
     *
     * @param partials Partials
     */
    public void setPartials(HashMap<String, String> partials) {
        this.partials = partials;
    }


}
