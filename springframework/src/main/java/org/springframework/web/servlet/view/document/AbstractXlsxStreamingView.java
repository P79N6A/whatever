package org.springframework.web.servlet.view.document;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public abstract class AbstractXlsxStreamingView extends AbstractXlsxView {

    @Override
    protected SXSSFWorkbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
        return new SXSSFWorkbook();
    }

    @Override
    protected void renderWorkbook(Workbook workbook, HttpServletResponse response) throws IOException {
        super.renderWorkbook(workbook, response);
        // Dispose of temporary files in case of streaming variant...
        ((SXSSFWorkbook) workbook).dispose();
    }

}
