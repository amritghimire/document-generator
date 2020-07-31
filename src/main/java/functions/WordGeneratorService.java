package functions;

import com.google.cloud.functions.HttpRequest;
import org.docx4j.openpackaging.exceptions.Docx4JException;

import java.io.File;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service to generate the word file.
 */
public class WordGeneratorService {
    /**
     * Generate the word file.
     * @param payload - Payload for partials and variables.
     * @param file - Template file
     * @param partials - Partials files.
     * @return Word Document
     * @throws IOException caused when reading and writing file.
     * @throws Docx4JException caused when handling document file.
     */
    public File generateWord(WordFormationPayload payload, HttpRequest.HttpPart file, HashMap<String, InputStream> partials) throws IOException, Docx4JException {
        TemplatePartials templatePartials = new TemplatePartials(payload.getParts(), payload.getPartsVariables(), partials);
        HashMap<String, String> processedTemplates = templatePartials.processTemplates();
        WordDocument wordDocument = new WordDocument(file, payload.getVariables(), processedTemplates);
        wordDocument.processWordTemplate();
        return wordDocument.save();
    }
}
