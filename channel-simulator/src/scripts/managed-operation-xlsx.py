#!/usr/bin/env python3
"""Write a minimal XLSX workbook using only the Python standard library.

The workbook intentionally leaves numeric date cells on the General style. This
matches Booking exports whose dates arrive as Excel serial values rather than as
date-formatted cells.
"""

from __future__ import annotations

import html
import json
import re
import sys
import zipfile
from pathlib import Path
from typing import Any


NUMBER_PATTERN = re.compile(r"^-?(?:0|[1-9]\d*)(?:\.\d+)?$")


def fail(message: str) -> "NoReturn":
    raise SystemExit(message)


def column_name(index: int) -> str:
    if index < 1:
        fail("column index must be positive")
    result = ""
    current = index
    while current > 0:
        current, remainder = divmod(current - 1, 26)
        result = chr(65 + remainder) + result
    return result


def xml_text(value: Any) -> str:
    text = str(value)
    for character in text:
        codepoint = ord(character)
        if codepoint < 0x20 and character not in "\t\n\r":
            fail("workbook contains an invalid XML control character")
    return html.escape(text, quote=False)


def worksheet_xml(model: dict[str, Any]) -> str:
    rows = model.get("rows")
    if not isinstance(rows, list) or not rows:
        fail("workbook model must contain at least one row")
    if len(rows) > 20000:
        fail("workbook model exceeds the row limit")

    max_columns = 0
    xml_rows: list[str] = []
    for row_index, raw_row in enumerate(rows, start=1):
        if not isinstance(raw_row, list):
            fail(f"row {row_index} is not a list")
        if len(raw_row) > 100:
            fail(f"row {row_index} exceeds the column limit")
        max_columns = max(max_columns, len(raw_row))
        cells: list[str] = []
        for column_index, cell in enumerate(raw_row, start=1):
            if not isinstance(cell, dict):
                fail(f"cell {row_index},{column_index} is not an object")
            cell_type = cell.get("type")
            value = str(cell.get("value", ""))
            reference = f"{column_name(column_index)}{row_index}"
            if cell_type == "number":
                if not NUMBER_PATTERN.fullmatch(value):
                    fail(f"cell {reference} is not a valid decimal number")
                cells.append(f'<c r="{reference}"><v>{value}</v></c>')
            elif cell_type == "string":
                cells.append(
                    f'<c r="{reference}" t="inlineStr"><is><t xml:space="preserve">'
                    f"{xml_text(value)}</t></is></c>"
                )
            else:
                fail(f"cell {reference} has unsupported type: {cell_type}")
        xml_rows.append(f'<row r="{row_index}">{"".join(cells)}</row>')

    dimension = f"A1:{column_name(max_columns)}{len(rows)}"
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">'
        f'<dimension ref="{dimension}"/>'
        '<sheetViews><sheetView workbookViewId="0"/></sheetViews>'
        '<sheetFormatPr defaultRowHeight="15"/>'
        f'<sheetData>{"".join(xml_rows)}</sheetData>'
        '</worksheet>'
    )


def zip_entry(archive: zipfile.ZipFile, name: str, text: str) -> None:
    info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o600 << 16
    archive.writestr(info, text.encode("utf-8"))


def write_workbook(output_path: Path, model: dict[str, Any]) -> None:
    sheet_name = str(model.get("sheetName") or "Booking")
    if not sheet_name or len(sheet_name) > 31 or any(char in sheet_name for char in "[]:*?/\\"):
        fail("invalid Excel sheet name")
    output_path.parent.mkdir(parents=True, exist_ok=True)

    content_types = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
    package_relationships = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
    workbook = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <bookViews><workbookView activeTab="0"/></bookViews>
  <sheets><sheet name="{html.escape(sheet_name, quote=True)}" sheetId="1" r:id="rId1"/></sheets>
  <calcPr calcId="0" fullCalcOnLoad="0"/>
</workbook>"""
    workbook_relationships = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="0"/>
  <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
  <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""

    with zipfile.ZipFile(output_path, mode="w") as archive:
        zip_entry(archive, "[Content_Types].xml", content_types)
        zip_entry(archive, "_rels/.rels", package_relationships)
        zip_entry(archive, "xl/workbook.xml", workbook)
        zip_entry(archive, "xl/_rels/workbook.xml.rels", workbook_relationships)
        zip_entry(archive, "xl/styles.xml", styles)
        zip_entry(archive, "xl/worksheets/sheet1.xml", worksheet_xml(model))


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: managed-operation-xlsx.py OUTPUT.xlsx")
    try:
        model = json.load(sys.stdin)
    except (json.JSONDecodeError, UnicodeError) as error:
        fail(f"invalid workbook JSON: {error}")
    if not isinstance(model, dict):
        fail("workbook JSON root must be an object")
    write_workbook(Path(sys.argv[1]), model)


if __name__ == "__main__":
    main()
