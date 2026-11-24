from pypdf import PdfWriter
from pypdf.generic import (DictionaryObject,NameObject,NumberObject,TextStringObject,ArrayObject,
    FloatObject,StreamObject,NullObject)
from reportlab.lib.units import mm
w=PdfWriter(clone_from="JSCH-Medicine-Price-Form.pdf")
page=w.pages[0]
font=DictionaryObject({NameObject("/Type"):NameObject("/Font"),NameObject("/Subtype"):NameObject("/Type1"),NameObject("/BaseFont"):NameObject("/Helvetica-Bold")})
fref=w._add_object(font)
def ap(wd,ht,rgb,label):
    s=StreamObject()
    tw=len(label)*0.6*11
    s._data=(f"q {rgb} rg 0 0 {wd:.2f} {ht:.2f} re f Q BT /HB 11 Tf 1 1 1 rg {(wd-tw)/2:.2f} {(ht-8)/2:.2f} Td ({label}) Tj ET").encode()
    s.update({NameObject("/Type"):NameObject("/XObject"),NameObject("/Subtype"):NameObject("/Form"),
      NameObject("/BBox"):ArrayObject([FloatObject(0),FloatObject(0),FloatObject(wd),FloatObject(ht)]),
      NameObject("/Resources"):DictionaryObject({NameObject("/Font"):DictionaryObject({NameObject("/HB"):fref})})})
    return w._add_object(s)
W,H=page.mediabox.width,page.mediabox.height
M=12*mm; bw,bh=30*mm,9*mm; y=float(M+21.5*mm)
btns=[("save_button","SAVE","0.122 0.478 0.243",'app.execMenuItem("SaveAs");',float(W)-M-2*bw-4*mm),
      ("print_button","PRINT","0.075 0.125 0.290",'this.print({bUI:true});',float(W)-M-bw)]
fields=w._root_object["/AcroForm"]["/Fields"]
annots=page["/Annots"]
for name,label,rgb,js,x in btns:
    a=DictionaryObject({
      NameObject("/Type"):NameObject("/Annot"),NameObject("/Subtype"):NameObject("/Widget"),
      NameObject("/FT"):NameObject("/Btn"),NameObject("/Ff"):NumberObject(65536),
      NameObject("/T"):TextStringObject(name),NameObject("/TU"):TextStringObject(label),
      NameObject("/F"):NumberObject(0),  # not printed
      NameObject("/Rect"):ArrayObject([FloatObject(x),FloatObject(y),FloatObject(x+bw),FloatObject(y+bh)]),
      NameObject("/MK"):DictionaryObject({NameObject("/BG"):ArrayObject([FloatObject(float(v)) for v in rgb.split()]),NameObject("/CA"):TextStringObject(label)}),
      NameObject("/DA"):TextStringObject("/Helv 11 Tf 1 1 1 rg"),
      NameObject("/AP"):DictionaryObject({NameObject("/N"):ap(bw,bh,rgb,label)}),
      NameObject("/A"):DictionaryObject({NameObject("/S"):NameObject("/JavaScript"),NameObject("/JS"):TextStringObject(js)}),
      NameObject("/P"):page.indirect_reference})
    ref=w._add_object(a); annots.append(ref); fields.append(ref)
w.write("JSCH-Medicine-Price-Form.pdf")
