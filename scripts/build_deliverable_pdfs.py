from __future__ import annotations

from pathlib import Path
import re

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import (
    SimpleDocTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
    PageBreak,
    Preformatted,
    Image as RLImage,
)
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont


ROOT = Path(__file__).resolve().parents[1]
DELIVERABLES = ROOT / "deliverables"
FONT_PATH = Path(r"C:\Windows\Fonts\simhei.ttf")
FONT_NAME = "SimHei"


def register_fonts() -> None:
    if not FONT_PATH.exists():
        raise FileNotFoundError(f"Chinese font not found: {FONT_PATH}")
    pdfmetrics.registerFont(TTFont(FONT_NAME, str(FONT_PATH)))


def make_styles():
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle(
            "TitleCN",
            parent=base["Title"],
            fontName=FONT_NAME,
            fontSize=22,
            leading=30,
            alignment=TA_CENTER,
            textColor=colors.HexColor("#12355B"),
            spaceAfter=18,
        ),
        "h1": ParagraphStyle(
            "Heading1CN",
            parent=base["Heading1"],
            fontName=FONT_NAME,
            fontSize=15,
            leading=21,
            textColor=colors.HexColor("#1F4E79"),
            spaceBefore=12,
            spaceAfter=7,
        ),
        "h2": ParagraphStyle(
            "Heading2CN",
            parent=base["Heading2"],
            fontName=FONT_NAME,
            fontSize=12.5,
            leading=18,
            textColor=colors.HexColor("#2F6F73"),
            spaceBefore=9,
            spaceAfter=5,
        ),
        "body": ParagraphStyle(
            "BodyCN",
            parent=base["BodyText"],
            fontName=FONT_NAME,
            fontSize=10.2,
            leading=16,
            alignment=TA_LEFT,
            firstLineIndent=0,
            spaceAfter=5,
        ),
        "bullet": ParagraphStyle(
            "BulletCN",
            parent=base["BodyText"],
            fontName=FONT_NAME,
            fontSize=10.0,
            leading=15,
            leftIndent=14,
            firstLineIndent=-10,
            spaceAfter=3,
        ),
        "code": ParagraphStyle(
            "CodeCN",
            parent=base["Code"],
            fontName=FONT_NAME,
            fontSize=8.2,
            leading=11,
            leftIndent=6,
            rightIndent=6,
            textColor=colors.HexColor("#222222"),
            backColor=colors.HexColor("#F4F6F8"),
            spaceBefore=4,
            spaceAfter=7,
        ),
        "small": ParagraphStyle(
            "SmallCN",
            parent=base["BodyText"],
            fontName=FONT_NAME,
            fontSize=8.6,
            leading=12,
        ),
        "caption": ParagraphStyle(
            "CaptionCN",
            parent=base["BodyText"],
            fontName=FONT_NAME,
            fontSize=8.6,
            leading=12,
            alignment=TA_CENTER,
            textColor=colors.HexColor("#5B6670"),
            spaceAfter=8,
        ),
    }


def escape(text: str) -> str:
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    )


def inline_markdown(text: str) -> str:
    text = escape(text)
    text = re.sub(r"`([^`]+)`", r"<font color='#5B2C6F'>\1</font>", text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", text)
    return text


def is_table_line(line: str) -> bool:
    return line.strip().startswith("|") and line.strip().endswith("|")


def is_separator(line: str) -> bool:
    stripped = line.strip().strip("|").strip()
    return bool(stripped) and all(set(cell.strip()) <= {"-", ":"} for cell in stripped.split("|"))


def split_table_row(line: str) -> list[str]:
    return [cell.strip() for cell in line.strip().strip("|").split("|")]


def table_col_widths(rows: list[list[str]], usable_width: float) -> list[float]:
    col_count = max(len(row) for row in rows)
    max_lens = [1] * col_count
    for row in rows:
        for index in range(col_count):
            value = row[index] if index < len(row) else ""
            max_lens[index] = max(max_lens[index], min(len(value), 28))
    total = sum(max_lens)
    return [usable_width * value / total for value in max_lens]


def build_table(lines: list[str], styles, usable_width: float) -> Table:
    rows = [split_table_row(line) for line in lines if not is_separator(line)]
    col_count = max(len(row) for row in rows)
    data = []
    for row_index, row in enumerate(rows):
        padded = row + [""] * (col_count - len(row))
        style = styles["small"] if col_count >= 4 else styles["body"]
        data.append([Paragraph(inline_markdown(cell), style) for cell in padded])
    table = Table(data, colWidths=table_col_widths(rows, usable_width), repeatRows=1)
    table.setStyle(
        TableStyle(
            [
                ("FONTNAME", (0, 0), (-1, -1), FONT_NAME),
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#E8EEF5")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.HexColor("#12355B")),
                ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#C8D0D8")),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("LEFTPADDING", (0, 0), (-1, -1), 5),
                ("RIGHTPADDING", (0, 0), (-1, -1), 5),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ]
        )
    )
    return table


def build_image(line: str, styles, usable_width: float):
    match = re.match(r"!\[([^\]]*)\]\(([^)]+)\)", line.strip())
    if not match:
        return []
    alt = match.group(1).strip()
    image_path = Path(match.group(2).strip())
    if not image_path.is_absolute():
        image_path = (DELIVERABLES / image_path).resolve()
        if not image_path.exists():
            image_path = (ROOT / match.group(2).strip()).resolve()
    if not image_path.exists():
        return [Paragraph(inline_markdown(f"[图片缺失：{match.group(2)}]"), styles["body"])]
    img = RLImage(str(image_path))
    max_width = usable_width
    max_height = 12.5 * cm
    scale = min(max_width / img.imageWidth, max_height / img.imageHeight, 1.0)
    img.drawWidth = img.imageWidth * scale
    img.drawHeight = img.imageHeight * scale
    result = [img, Spacer(1, 3)]
    if alt:
        result.append(Paragraph(inline_markdown(alt), styles["caption"]))
    return result


def parse_markdown(path: Path, styles, usable_width: float):
    lines = path.read_text(encoding="utf-8").splitlines()
    story = []
    in_code = False
    code_lines: list[str] = []
    table_lines: list[str] = []
    first_heading = True

    def flush_code():
        nonlocal code_lines
        if code_lines:
            story.append(Preformatted("\n".join(code_lines), styles["code"]))
            code_lines = []

    def flush_table():
        nonlocal table_lines
        if table_lines:
            story.append(build_table(table_lines, styles, usable_width))
            story.append(Spacer(1, 6))
            table_lines = []

    for raw in lines:
        line = raw.rstrip()
        if line.strip().startswith("```"):
            if in_code:
                flush_code()
                in_code = False
            else:
                flush_table()
                in_code = True
            continue

        if in_code:
            code_lines.append(line)
            continue

        if is_table_line(line):
            table_lines.append(line)
            continue
        flush_table()

        stripped = line.strip()
        if not stripped:
            story.append(Spacer(1, 4))
            continue
        if stripped.startswith("!["):
            story.extend(build_image(stripped, styles, usable_width))
            continue
        if stripped.startswith(">"):
            stripped = stripped.lstrip(">").strip()
            story.append(Paragraph(inline_markdown(stripped), styles["body"]))
            continue
        if stripped.startswith("# "):
            if not first_heading:
                story.append(PageBreak())
            story.append(Paragraph(inline_markdown(stripped[2:]), styles["title"]))
            first_heading = False
            continue
        if stripped.startswith("## "):
            story.append(Paragraph(inline_markdown(stripped[3:]), styles["h1"]))
            continue
        if stripped.startswith("### "):
            story.append(Paragraph(inline_markdown(stripped[4:]), styles["h2"]))
            continue
        if re.match(r"^\d+\.\s+", stripped):
            story.append(Paragraph(inline_markdown(stripped), styles["bullet"]))
            continue
        if stripped.startswith("- "):
            story.append(Paragraph("· " + inline_markdown(stripped[2:]), styles["bullet"]))
            continue
        story.append(Paragraph(inline_markdown(stripped), styles["body"]))

    flush_table()
    flush_code()
    return story


def add_page_number(canvas, doc):
    canvas.saveState()
    canvas.setFont(FONT_NAME, 8)
    canvas.setFillColor(colors.HexColor("#666666"))
    canvas.drawRightString(A4[0] - 1.6 * cm, 1.1 * cm, f"第 {doc.page} 页")
    canvas.restoreState()


def build_pdf(markdown_name: str, pdf_name: str) -> None:
    styles = make_styles()
    pdf_path = DELIVERABLES / pdf_name
    doc = SimpleDocTemplate(
        str(pdf_path),
        pagesize=A4,
        leftMargin=1.75 * cm,
        rightMargin=1.75 * cm,
        topMargin=1.6 * cm,
        bottomMargin=1.7 * cm,
        title=pdf_name,
    )
    story = parse_markdown(DELIVERABLES / markdown_name, styles, doc.width)
    doc.build(story, onFirstPage=add_page_number, onLaterPages=add_page_number)


def main() -> None:
    register_fonts()
    build_pdf("design_document.md", "design_document.pdf")
    build_pdf("experiment_report.md", "experiment_report.pdf")


if __name__ == "__main__":
    main()
