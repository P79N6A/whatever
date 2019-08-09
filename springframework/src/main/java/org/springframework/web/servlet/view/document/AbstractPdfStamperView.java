package org.springframework.web.servlet.view.document;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public abstract class AbstractPdfStamperView extends AbstractUrlBasedView {

    public AbstractPdfStamperView() {
        setContentType("application/pdf");
    }

    @Override
    protected boolean generatesDownloadContent() {
        return true;
    }

    @Override
    protected final void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // IE workaround: write into byte array first.
        ByteArrayOutputStream baos = createTemporaryOutputStream();
        PdfReader reader = readPdfResource();
        PdfStamper stamper = new PdfStamper(reader, baos);
        mergePdfDocument(model, stamper, request, response);
        stamper.close();
        // Flush to HTTP response.
        writeToResponse(response, baos);
    }

    protected PdfReader readPdfResource() throws IOException {
        String url = getUrl();
        Assert.state(url != null, "'url' not set");
        return new PdfReader(obtainApplicationContext().getResource(url).getInputStream());
    }

    protected abstract void mergePdfDocument(Map<String, Object> model, PdfStamper stamper, HttpServletRequest request, HttpServletResponse response) throws Exception;

}
