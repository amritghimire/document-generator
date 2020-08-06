package functions;

import org.docx4j.wml.P;
import org.docx4j.wml.Text;
import org.docx4j.wml.ContentAccessor;
import com.google.cloud.functions.HttpRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import java.io.*;
import java.util.*;
import javax.xml.bind.JAXBElement;

/**
 * Class to handle word document.
 */
public class WordDocument {
    private final HttpRequest.HttpPart file;
    private final JsonNode variables;
    private final HashMap<String, String> processedTemplates;
    private WordprocessingMLPackage template;

    /**
     * Constructor for word document.
     *
     * @param file - Template file for word.
     * @param variables - Variables list to replace for word file.
     * @param processedTemplates - The hash map for processed template with html.
     * @throws IOException Caused when handling file.
     * @throws Docx4JException Caused when processing the file.
     */
    public WordDocument(HttpRequest.HttpPart file, JsonNode variables, HashMap<String, String> processedTemplates) throws IOException, Docx4JException {

        this.file = file;
        this.variables = variables;
        this.processedTemplates = processedTemplates;

        initializeWordProcessing();
    }

    /**
     * Get all element of specific class in a tree structure.
     * @param obj Parent object to search
     * @param toSearch Class to find.
     * @return List of found nodes.
     */
    private static List<Object> getAllElementFromObject(Object obj, Class<?> toSearch) {
        List<Object> result = new ArrayList<>();
        if (obj instanceof JAXBElement) obj = ((JAXBElement<?>) obj).getValue();

        if (obj.getClass().equals(toSearch))
            result.add(obj);
        else if (obj instanceof ContentAccessor) {
            List<?> children = ((ContentAccessor) obj).getContent();
            for (Object child : children) {
                result.addAll(getAllElementFromObject(child, toSearch));
            }

        }
        return result;
    }

    /**
     * Initialize the word processing.
     *
     * @throws IOException Caused when handling file.
     * @throws Docx4JException Caused when processing the file.
     */
    private void initializeWordProcessing() throws IOException, Docx4JException {
        template = WordprocessingMLPackage.load(file.getInputStream());
    }

    /**
     * Process the template file by replacing the placeholder and partials.
     * @throws Docx4JException Caused when handling the document.
     */
    public void processWordTemplate() throws Docx4JException {
        replacePlaceholder();
        for (String partial : processedTemplates.keySet()) {
            replacePartial(partial);
        }
    }

    /**
     * Replace the single partial in the document.
     *
     * @param partialText - Name of partial to replace.
     * @throws Docx4JException Caused when handling the document.
     */
    private void replacePartial(String partialText) throws Docx4JException {
        // 1. get the paragraph
        List<Object> paragraphs = getAllElementFromObject(template.getMainDocumentPart(), P.class);
        String placeholder = "${" + partialText + "}";

        P toReplace = null;
        for (Object p : paragraphs) {
            List<Object> texts = getAllElementFromObject(p, Text.class);
            for (Object t : texts) {
                Text content = (Text) t;
                if (content.getValue().equals(placeholder)) {
                    toReplace = (P) p;
                    break;
                }
            }
        }
        if (toReplace == null) {
            return;
        }
        XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(template);
        List<Object> converted = XHTMLImporter.convert(processedTemplates.getOrDefault(partialText, ""), null);
        ContentAccessor parent = (ContentAccessor) toReplace.getParent();
        int i = parent.getContent().indexOf(toReplace);
        parent.getContent().addAll(i, converted);
        parent.getContent().remove(toReplace);
    }

    /**
     * Replaces the placeholder texts from variable lists.
     */
    private void replacePlaceholder() {
        List<Object> texts = getAllElementFromObject(template.getMainDocumentPart(), Text.class);

        for (Object text : texts) {
            Text textElement = (Text) text;
            replaceText(textElement);
        }
    }

    /**
     * Process a text element to find and replace the text variable.
     * @param textElement - Text Element of word document.
     */
    private void replaceText(Text textElement) {
        String text = textElement.getValue();
        Iterator<Map.Entry<String, JsonNode>> fields = variables.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String variable = field.getKey();
            String findText = "${" + variable + "}";
            String replaceText = field.getValue().asText(variable);
            if (text != null && text.contains(findText)) {
                text = text.replace(findText, replaceText);
            }
        }
        textElement.setValue(text);
    }

    /**
     * Save the provided file.
     * @return Document file.
     * @throws Docx4JException caused when saving document.
     */
    public File save() throws Docx4JException, IOException {
        File wordFile = File.createTempFile("WordOutput", ".docx");
        template.save(wordFile);
        return wordFile;
    }

}
