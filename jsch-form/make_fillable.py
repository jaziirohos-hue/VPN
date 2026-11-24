from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor, white
from reportlab.lib.units import mm
NAVY,GREEN,RED,ALT,LINE=HexColor("#13204a"),HexColor("#1f7a3e"),HexColor("#8a1020"),HexColor("#f2f6f3"),HexColor("#9aa5b8")
W,H=A4; M=12*mm
c=canvas.Canvas("JSCH-Medicine-Price-Form.pdf",pagesize=A4)
c.setTitle("JSCH Medicine Price Form"); c.setAuthor("Jazeera Specialist Community Hospital")
def header():
    c.drawImage("emblem.png",M+18*mm,H-M-26*mm,26*mm,26*mm,mask="auto")
    x=M+50*mm
    c.setFillColor(NAVY); c.setFont("Helvetica-Bold",28); c.drawString(x,H-M-11*mm,"JAZEERA")
    c.setFillColor(GREEN); c.setFont("Helvetica-Bold",11.5); c.drawString(x,H-M-17*mm,"SPECIALIST COMMUNITY HOSPITAL")
    c.setFillColor(RED); c.setFont("Helvetica-Bold",10); c.drawString(x,H-M-22.5*mm,"(JSCH)   |   Tel: +252 61 259 0697")
    c.setStrokeColor(GREEN); c.setLineWidth(3.5); c.line(M,H-M-30*mm,W-M,H-M-30*mm)
header()
f=c.acroForm
y=H-M-40*mm
c.setFillColor(NAVY); c.setFont("Helvetica-Bold",14); c.drawString(M,y,"MEDICINE PRICE LIST")
c.setFont("Helvetica",9.5); c.drawRightString(W-M-42*mm,y,"Date:")
f.textfield(name="date",x=W-M-40*mm,y=y-2.5*mm,width=40*mm,height=7*mm,borderColor=LINE,fillColor=white,textColor=NAVY,fontSize=10,borderWidth=0.6)
# columns
cols=[("No.",12*mm),("MEDICINE NAME",W-2*M-12*mm-34*mm-32*mm-22*mm),("UNIT",34*mm),("PRICE ($)",32*mm),("SAVE",22*mm)]
import json; geo=[]
top=y-6*mm; hh=8*mm; rh=(top-hh-(M+34*mm))/30
c.setFillColor(NAVY); c.rect(M,top-hh,W-2*M,hh,fill=1,stroke=0)
c.setFillColor(white); c.setFont("Helvetica-Bold",9.5)
xs=[M]
for name,w in cols: xs.append(xs[-1]+w)
for i,(name,w) in enumerate(cols):
    c.drawString(xs[i]+2.2*mm,top-hh+2.8*mm,name)
for r in range(30):
    yy=top-hh-(r+1)*rh
    if r%2: c.setFillColor(ALT); c.rect(M,yy,W-2*M,rh,fill=1,stroke=0)
    c.setStrokeColor(LINE); c.setLineWidth(0.5); c.rect(M,yy,W-2*M,rh,fill=0,stroke=1)
    for xv in xs[1:-1]: c.line(xv,yy,xv,yy+rh)
    c.setFillColor(HexColor("#555555")); c.setFont("Helvetica",9); c.drawCentredString(M+6*mm,yy+2.3*mm,str(r+1))
    for k,key in [(1,"name"),(2,"unit"),(3,"price")]:
        f.textfield(name=f"{key}_{r+1}",x=xs[k]+0.8*mm,y=yy+0.6*mm,width=cols[k][1]-1.6*mm,height=rh-1.2*mm,
            borderWidth=0,fillColor=None,textColor=NAVY,fontSize=10,fontName="Helvetica",
            tooltip=f"{cols[k][0].title()} {r+1}")
    geo.append([xs[4]+1.5*mm,yy+0.8*mm,cols[4][1]-3*mm,rh-1.6*mm])
end=top-hh-30*rh
# footer
fy=M+24*mm
c.setFillColor(NAVY); c.setFont("Helvetica",9.5)
c.drawString(M,fy,"Prepared by:"); f.textfield(name="prepared_by",x=M+22*mm,y=fy-2.5*mm,width=55*mm,height=7*mm,borderWidth=0.6,borderColor=LINE,fillColor=white,textColor=NAVY,fontSize=10)
c.drawString(M+84*mm,fy,"Signature:"); c.setStrokeColor(NAVY); c.line(M+102*mm,fy-2*mm,M+150*mm,fy-2*mm)
c.setStrokeColor(GREEN); c.setLineWidth(3.5); c.line(M,M+16*mm,W-M,M+16*mm)
c.drawImage("emblem.png",M,M+1*mm,13*mm,13*mm,mask="auto")
c.setFillColor(NAVY); c.setFont("Helvetica-Bold",11); c.drawString(M+15*mm,M+8.5*mm,"JAZEERA SPECIALIST COMMUNITY HOSPITAL (JSCH)")
c.setFillColor(GREEN); c.setFont("Helvetica-Bold",9.5); c.drawString(M+15*mm,M+3.5*mm,"Tel: +252 61 259 0697")
c.setFillColor(HexColor("#666666")); c.setFont("Helvetica",7.5); c.drawRightString(W-M,M+3.5*mm,"Pharmacy Department")
c.save()
json.dump(geo,open("save_geo.json","w"))
