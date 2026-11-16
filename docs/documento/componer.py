"""Arma el PDF final: caratula original + cuerpo generado, con encabezado y pie en cada pagina.
Uso: python3 componer.py <pdf original (caratula)> <cuerpo.pdf> <salida.pdf>"""
import io
import sys

from pypdf import PdfReader, PdfWriter
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas

original, cuerpo, salida = sys.argv[1:4]
pdfmetrics.registerFont(TTFont("Carlito", "/usr/share/fonts/truetype/crosextra/Carlito-Regular.ttf"))

AZUL = (0.18, 0.37, 0.65)
MARGEN = 54  # 1,9 cm

portada = PdfReader(original).pages[0]
paginas = PdfReader(cuerpo).pages
total = len(paginas) + 1

writer = PdfWriter()
writer.add_page(portada)
for numero, pagina in enumerate(paginas, start=2):
    ancho, alto = float(pagina.mediabox.width), float(pagina.mediabox.height)
    capa = io.BytesIO()
    c = canvas.Canvas(capa, pagesize=(ancho, alto))
    c.setFont("Carlito", 10.5)
    # Encabezado
    c.setFillColorRGB(0, 0, 0)
    c.drawString(MARGEN + 14, alto - 42, "Equipo N03")
    c.drawRightString(ancho - MARGEN - 14, alto - 42, "Aplicaciones Interactivas")
    c.setStrokeColorRGB(*AZUL)
    c.setLineWidth(2)
    c.line(MARGEN + 14, alto - 50, ancho - MARGEN, alto - 50)
    # Pie
    c.line(MARGEN + 14, 46, ancho - MARGEN, 46)
    c.drawString(MARGEN + 14, 30, "Curso: ")
    c.setFillColorRGB(*AZUL)
    c.drawString(MARGEN + 14 + c.stringWidth("Curso: ", "Carlito", 10.5), 30, "2do Cuatrimestre – Martes, Noche")
    c.setFillColorRGB(0, 0, 0)
    c.drawRightString(ancho - MARGEN - 14, 30, f"Página {numero} de {total}")
    c.save()
    capa.seek(0)
    pagina.merge_page(PdfReader(capa).pages[0])
    writer.add_page(pagina)

writer.add_metadata({"/Title": "TPO DeliveryGo - Grupo 3 - Módulo de Pagos (actualizado)"})
with open(salida, "wb") as f:
    writer.write(f)
print(f"{salida}: {total} paginas")
