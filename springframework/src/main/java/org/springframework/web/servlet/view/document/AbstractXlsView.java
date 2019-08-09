package org.springframework.web.servlet.view.document;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public abstract class AbstractXlsView extends AbstractView {

    public AbstractXlsView() {
        setContentType("application/vnd.ms-excel");
    }

    @Override
    protected boolean generatesDownloadContent() {
        return true;
    }

    @Override
    protected final void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Create a fresh workbook instance for this render step.
        Workbook workbook = createWorkbook(model, request);
        // Delegate to application-provided document code.
        buildExcelDocument(model, workbook, request, response);
        // Set the content type.
        response.setContentType(getContentType());
        // Flush byte array to servlet output stream.
        renderWorkbook(workbook, response);
    }

    protected Workbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
        return new HSSFWorkbook();
    }

    protected void renderWorkbook(Workbook workbook, HttpServletResponse response) throws IOException {
        ServletOutputStream out = response.getOutputStream();
        workbook.write(out);
        workbook.close();
    }

    protected abstract void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception;

}
