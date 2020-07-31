package functions;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.docx4j.openpackaging.exceptions.Docx4JException;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.net.HttpURLConnection;

/**
 * Main entry point for Google Cloud function.
 */
public class DocsGenerator implements HttpFunction {

    private final WordGeneratorService wordGeneratorService;

    /**
     * Initialize the service class.
     */
    public DocsGenerator() {
        wordGeneratorService = new WordGeneratorService();
    }

    /**
     * Main service function to serve request and response.
     *
     * @param request - Request from http
     * @param response - Response to return
     * @throws IOException when handling request stream.
     */
    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException{
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setStatusCode(HttpURLConnection.HTTP_BAD_METHOD);
            return;
        }

        HashMap<String, InputStream> uploads = new HashMap<>();
        Map<String, HttpRequest.HttpPart> parts = request.getParts();
        HttpRequest.HttpPart payload = null;
        HttpRequest.HttpPart file = null;
        for (Map.Entry<String, HttpRequest.HttpPart> next : parts.entrySet()) {
            String key = next.getKey();
            HttpRequest.HttpPart value = next.getValue();
            if (key.equalsIgnoreCase("payload")) {
                payload = value;
            } else if (key.equalsIgnoreCase("file")) {
                file = value;
            } else {
                String filename = value.getFileName().orElse(key);
                uploads.put(filename, value.getInputStream());
            }
        }
        if(payload == null || file == null){
            response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        WordFormationPayload wordFormationPayload = mapper.readValue(payload.getInputStream(), WordFormationPayload.class);
        try {
            File word = wordGeneratorService.generateWord(wordFormationPayload, file, uploads);
            FileInputStream fileInputStream = new FileInputStream(word);
            OutputStream outputStream = response.getOutputStream();
            fileInputStream.transferTo(outputStream);
        } catch (Docx4JException e) {
            e.printStackTrace();
            response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
        }
    }
}
