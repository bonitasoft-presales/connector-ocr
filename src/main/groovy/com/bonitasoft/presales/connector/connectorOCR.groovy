package com.bonitasoft.presales.connector

import groovy.util.logging.Slf4j
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import org.bonitasoft.engine.api.ProcessAPI
import org.bonitasoft.engine.bpm.document.Document
import org.bonitasoft.engine.bpm.document.DocumentNotFoundException
import org.bonitasoft.engine.connector.AbstractConnector
import org.bonitasoft.engine.connector.ConnectorException
import org.bonitasoft.engine.connector.ConnectorValidationException

@Slf4j
class connectorOCR extends AbstractConnector {

    def static final INPUT_ATTACHMENT = "attachment"
    def static final INPUT_LANGUAGE = "language"
    def static final OUTPUT_OCR_RESULT = "ocrOutput"

    /**
     * Perform validation on the inputs defined on the connector definition (src/main/resources/connector-ocr.def)
     * You should:
     * - validate that mandatory inputs are presents
     * - validate that the content of the inputs is coherent with your use case (e.g: validate that a date is / isn't in the past ...)
     */
    @Override
    void validateInputParameters() throws ConnectorValidationException {
        checkMandatoryStringInput(INPUT_ATTACHMENT)
        checkMandatoryStringInput(INPUT_LANGUAGE)
    }
    
    def checkMandatoryStringInput(inputName) throws ConnectorValidationException {
        def value = getInputParameter(inputName)
        if (value in String) {
            if (!value) {
                throw new ConnectorValidationException(this, "Mandatory parameter '$inputName' is missing.")
            }
        } else {
            throw new ConnectorValidationException(this, "'$inputName' parameter must be a String")
        }
    }

    private static Tesseract getTesseract() {

        Tesseract instance = new Tesseract()
        instance.setDatapath("/bin/trainedData")
        instance.setLanguage(INPUT_LANGUAGE)
    }

    private Document getDocument(Object attachment, ProcessAPI processAPI)
            throws ConnectorException, DocumentNotFoundException {
        if (attachment instanceof String && !((String) attachment).trim().isEmpty()) {
            String docName = (String) attachment;
            long processInstanceId = getExecutionContext().getProcessInstanceId();
            return processAPI.getLastDocument(processInstanceId, docName);
        } else if (attachment instanceof Document) {
            return (Document) attachment;
        } else {
            throw new ConnectorException(
                    "Attachments must be document names or org.bonitasoft.engine.bpm.document.Document");
        }
    }

    /**
     * Core method:
     * - Execute all the business logic of your connector using the inputs (connect to an external service, compute some values ...).
     * - Set the output of the connector execution. If outputs are not set, connector fails.
     */
    @Override
    void executeBusinessLogic() throws ConnectorException, TesseractException {
        def attachment = getInputParameter(INPUT_ATTACHMENT)
        log.info "$INPUT_ATTACHMENT : $attachment"
        Tesseract tesseract = getTesseract()
        //Get document from case
        try {
            ProcessAPI processAPI = getAPIAccessor().getProcessAPI()
            Document document = getDocument(attachment,processAPI)
            def file = document as File
            def result = tesseract.doOCR(file)
            setOutputParameter(OUTPUT_OCR_RESULT,result)

        } catch (DocumentNotFoundException | IOException e) {
            throw new ConnectorException(e);
        }
    }
    
    /**
     * [Optional] Open a connection to remote server
     */
    @Override
    void connect() throws ConnectorException{}

    /**
     * [Optional] Close connection to remote server
     */
    @Override
    void disconnect() throws ConnectorException{}
}
