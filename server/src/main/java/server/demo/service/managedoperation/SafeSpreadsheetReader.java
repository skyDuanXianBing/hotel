package server.demo.service.managedoperation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import server.demo.exception.ManagedOperationValidationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class SafeSpreadsheetReader {
    // Two files share the application's 15MB multipart request cap.
    static final long MAX_FILE_BYTES = 7L * 1024 * 1024;
    static final int MAX_ROWS = 20_000;
    static final int MAX_COLUMNS = 100;
    static final int MAX_CELL_LENGTH = 2_000;
    static final int MAX_SHEETS = 10;

    public Table read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ManagedOperationValidationException("上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ManagedOperationValidationException("单个上传文件不能超过 7MB");
        }
        String extension = extension(file.getOriginalFilename());
        try {
            byte[] bytes = file.getBytes();
            validateSignature(extension, bytes);
            return switch (extension) {
                case "csv" -> readCsv(bytes);
                case "xls", "xlsx" -> readExcel(bytes);
                default -> throw new ManagedOperationValidationException("仅支持 CSV、XLS、XLSX 文件");
            };
        } catch (ManagedOperationValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ManagedOperationValidationException("表格解析失败: " + safeMessage(ex), ex);
        }
    }

    private Table readCsv(byte[] bytes) throws IOException {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException ex) {
            throw new ManagedOperationValidationException("CSV 必须使用严格 UTF-8 编码");
        }
        if (!text.isEmpty() && text.charAt(0) == '\ufeff') {
            text = text.substring(1);
        }
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(false)
                .setTrim(false)
                .get();
        List<List<String>> rows = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(new StringReader(text), format)) {
            for (CSVRecord record : parser) {
                if (record.getRecordNumber() > MAX_ROWS) {
                    throw new ManagedOperationValidationException("表格行数超过 " + MAX_ROWS);
                }
                if (record.size() > MAX_COLUMNS) {
                    throw new ManagedOperationValidationException("表格列数超过 " + MAX_COLUMNS);
                }
                List<String> row = new ArrayList<>(record.size());
                for (String value : record) {
                    row.add(safeCell(value));
                }
                rows.add(row);
            }
        }
        return validateTable(rows);
    }

    private Table readExcel(byte[] bytes) throws Exception {
        ZipSecureFile.setMinInflateRatio(0.01d);
        ZipSecureFile.setMaxEntrySize(50L * 1024 * 1024);
        ZipSecureFile.setMaxTextSize(20L * 1024 * 1024);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() > MAX_SHEETS) {
                throw new ManagedOperationValidationException("Excel sheet 数超过 " + MAX_SHEETS);
            }
            Sheet selected = null;
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (hasContent(sheet)) {
                    selected = sheet;
                    break;
                }
            }
            if (selected == null) {
                throw new ManagedOperationValidationException("Excel 中没有可读取的数据");
            }
            if (selected.getLastRowNum() + 1 > MAX_ROWS) {
                throw new ManagedOperationValidationException("表格行数超过 " + MAX_ROWS);
            }
            List<List<String>> rows = new ArrayList<>();
            for (int rowIndex = selected.getFirstRowNum(); rowIndex <= selected.getLastRowNum(); rowIndex++) {
                Row row = selected.getRow(rowIndex);
                int lastCell = row == null ? 0 : Math.max(0, row.getLastCellNum());
                if (lastCell > MAX_COLUMNS) {
                    throw new ManagedOperationValidationException("表格列数超过 " + MAX_COLUMNS);
                }
                List<String> values = new ArrayList<>(lastCell);
                for (int column = 0; column < lastCell; column++) {
                    values.add(safeCell(cellValue(row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));
                }
                rows.add(values);
            }
            return validateTable(rows);
        }
    }

    private static String cellValue(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                // Preserve Excel's numeric text without constructing financial values from a binary double.
                // Amount parsing later uses BigDecimal(String).
                yield NumberToTextConverter.toText(cell.getNumericCellValue());
            }
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case ERROR -> throw new ManagedOperationValidationException("Excel 包含错误单元格");
            default -> "";
        };
    }

    private static boolean hasContent(Sheet sheet) {
        if (sheet == null) return false;
        int scanned = 0;
        for (Row row : sheet) {
            if (++scanned > MAX_ROWS) break;
            for (Cell cell : row) {
                if (cell != null && cell.getCellType() != CellType.BLANK) return true;
            }
        }
        return false;
    }

    private static Table validateTable(List<List<String>> rows) {
        while (!rows.isEmpty() && rows.get(rows.size() - 1).stream().allMatch(String::isBlank)) {
            rows.remove(rows.size() - 1);
        }
        if (rows.size() < 2 || rows.get(0).stream().allMatch(String::isBlank)) {
            throw new ManagedOperationValidationException("表格没有表头或数据行");
        }
        return new Table(List.copyOf(rows.get(0)), List.copyOf(rows.subList(1, rows.size())));
    }

    private static String safeCell(String value) {
        String result = value == null ? "" : value.strip();
        if (result.length() > MAX_CELL_LENGTH) {
            throw new ManagedOperationValidationException("单元格文字超过 " + MAX_CELL_LENGTH + " 字符");
        }
        return result;
    }

    private static String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static void validateSignature(String extension, byte[] bytes) {
        boolean ole = bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8),
                new byte[]{(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0, (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1});
        boolean zip = bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K'
                && (bytes[2] == 3 || bytes[2] == 5 || bytes[2] == 7)
                && (bytes[3] == 4 || bytes[3] == 6 || bytes[3] == 8);
        switch (extension) {
            case "xls" -> {
                if (!ole) throw new ManagedOperationValidationException("XLS 扩展名与文件内容不一致");
            }
            case "xlsx" -> {
                if (!zip) throw new ManagedOperationValidationException("XLSX 扩展名与文件内容不一致");
            }
            case "csv" -> {
                if (ole || zip || containsNul(bytes)) {
                    throw new ManagedOperationValidationException("CSV 扩展名与文件内容不一致");
                }
            }
            default -> throw new ManagedOperationValidationException("仅支持 CSV、XLS、XLSX 文件");
        }
    }

    private static boolean containsNul(byte[] bytes) {
        for (byte b : bytes) if (b == 0) return true;
        return false;
    }

    private static String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) return ex.getClass().getSimpleName();
        return message.length() > 200 ? message.substring(0, 200) : message;
    }

    static String normalizeHeader(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .replaceAll("[\\s_\\-]+", "").toLowerCase(Locale.ROOT);
    }

    public record Table(List<String> headers, List<List<String>> rows) {}
}
