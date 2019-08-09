package org.springframework.web.servlet.view.document;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public abstract class AbstractXlsxView extends AbstractXlsView {

    public AbstractXlsxView() {
        setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Override
    protected Workbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
        return new XSSFWorkbook();
    }

}
