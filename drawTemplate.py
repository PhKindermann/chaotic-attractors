# code by Olszewski et al.
import math


def goToCoord(x,y):
    return "M "+str(int(x))+" "+str(int(y))


def bezier(x,y):
    return "c 0 "+str(int(y/2))+" "+str(int(x))+" "+str(int(y)/2)+" "+str(int(x))+" "+str(int(y))


def goVertical(height):
    return "v "+str(int(height))


def goHorizontal(breadth):
    return "h "+str(int(breadth))


def finishPath():
    return "z"


def leftPermut(x, y, drawing, color="white", scale=1):
    p = drawing.path(style="fill:"+color)
    p.push(goToCoord(x,y))
    p.push(bezier(100 * scale, 100 * scale))
    p.push(goVertical(1))
    p.push(goHorizontal(40 * scale))
    p.push(goVertical(-1))
    p.push(bezier(-100 * scale, -100 * scale))
    p.push(finishPath())
    drawing.add(p)
    p = drawing.path(style="fill:"+color, stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x,y))
    p.push(bezier(100 * scale, 100 * scale))
    p.push(goVertical(1))
    p.push(goHorizontal(1))
    p.push(goVertical(-1))
    p.push(bezier(-100 * scale, -100 * scale))
    p.push(finishPath())
    drawing.add(p)
    p = drawing.path(style="fill:"+color, stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x + 40 * scale,y))
    p.push(bezier(100 * scale, 100 * scale))
    p.push(goVertical(1))
    p.push(goHorizontal(-1))
    p.push(goVertical(-1))
    p.push(bezier(-100 * scale, -100 * scale))
    p.push(finishPath())
    drawing.add(p)


def rightPermut(x, y, drawing, color = "white", scale=1):
    p = drawing.path(style="fill:" + color)
    p.push(goToCoord(x, y))
    p.push(bezier(-100 * scale, 100 * scale))
    p.push(goVertical(1))
    p.push(goHorizontal(40 * scale))
    p.push(goVertical(-1))
    p.push(bezier(100 * scale, -100 * scale))
    p.push(finishPath())
    drawing.add(p)
    p = drawing.path(style="fill:" + color, stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x, y))
    p.push(bezier(-100 * scale, 100 * scale))
    p.push(goVertical(1))
    p.push(goHorizontal(1))
    p.push(goVertical(-1))
    p.push(bezier(100 * scale, -100 * scale))
    p.push(finishPath())
    drawing.add(p)
    p = drawing.path(style="fill:" + color, stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x + 40 * scale, y))
    p.push(bezier(-100 * scale, 100 * scale))
    p.push(goVertical(1))
    p.push(goHorizontal(-1))
    p.push(goVertical(-1))
    p.push(bezier(100 * scale, -100 * scale))
    p.push(finishPath())
    drawing.add(p)


def straightTransition(x, y, drawing, length, color="white", scale=1):
    p = drawing.path(style="fill:"+color)
    p.push(goToCoord(x, y))
    p.push(goVertical(length + 1))
    p.push(goHorizontal(40 * scale))
    p.push(goVertical(-(length + 1)))
    p.push(finishPath())
    drawing.add(p)
    p = drawing.path(style="fill:"+color, stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x, y))
    p.push(goVertical(length + 1))
    p.push(goHorizontal(1))
    p.push(goVertical(-(length + 1)))
    p.push(finishPath())
    drawing.add(p)
    p = drawing.path(style="fill:" + color, stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x + 40 * scale, y))
    p.push(goVertical(length + 1))
    p.push(goHorizontal(-1))
    p.push(goVertical(-(length + 1)))
    p.push(finishPath())
    drawing.add(p)


def torsionShape(x, y, drawing, color="white", scale=1):
    p = drawing.path(style="fill:"+color)
    p.push(goToCoord(x, y))
    p.push(bezier(40 * scale, 40 * scale))
    p.push(goVertical(1))
    p.push(goHorizontal(-(40 * scale)))
    p.push(goVertical(-1))
    p.push(bezier(40 * scale, -40 * scale))
    p.push(finishPath())
    drawing.add(p)

def positiveTorsion(x, y, drawing, color="white", scale=1):
    torsionShape(x, y, drawing, color, scale=scale)
    p = drawing.path(style="fill:"+color, stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x, y))
    p.push(bezier((40 * scale) - 1, 40 * scale))
    p.push(goVertical(1))
    p.push(goHorizontal(1))
    p.push(goVertical(-1))
    p.push(bezier(-(40 * scale) + 1, -40 * scale))
    p.push(finishPath())
    drawing.add(p)
    drawing.add(drawing.circle(center = (int(x + 20 * scale),int(y + 20 * scale)), r=str(int(5 * scale)), fill="white"))
    p = drawing.path(style="fill:"+color, stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x + 40 * scale, y))
    p.push(bezier(-(40 * scale) + 1, 40 * scale))
    p.push(goVertical(1))
    p.push(goHorizontal(-1))
    p.push(goVertical(-1))
    p.push(bezier(40 * scale - 1, -40 * scale))
    p.push(finishPath())
    drawing.add(p)

def negativeTorsion(x, y, drawing, color="white", scale=1):
    torsionShape(x, y, drawing, color, scale=scale)
    p = drawing.path(style="fill:"+color, stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x + 40 * scale, y))
    p.push(bezier(-(40 * scale) + 1, 40 * scale))
    p.push(goVertical(1))
    p.push(goHorizontal(-1))
    p.push(goVertical(-1))
    p.push(bezier(40 * scale - 1, -40 * scale))
    p.push(finishPath())
    drawing.add(p)
    drawing.add(drawing.circle(center = (int(x + 20 * scale),int(y + 20 * scale)), r=str(int(5 * scale)), fill="white"))
    p = drawing.path(style="fill:"+color, stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x, y))
    p.push(bezier(40 * scale - 1, 40 * scale))
    p.push(goVertical(1))
    p.push(goHorizontal(1))
    p.push(goVertical(-1))
    p.push(bezier(-(40 * scale) + 1, -40 * scale))
    p.push(finishPath())
    drawing.add(p)


def upperSemiCircle(x1, y1, x2, y2, drawing, scale):
    p = drawing.path(stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x1, y2))
    radius = (math.sqrt((int(x1)-int(x2))**2 + (int(y1)-int(y2))**2))/2
    p.push_arc((x2,y2),180,radius,large_arc=True,angle_dir = "+",absolute=True)
    p.push(goHorizontal(1))
    p.push_arc((int(x1)-1,int(y1)),180,radius,large_arc=True,angle_dir="-",absolute=True)
    p.push(finishPath())
    drawing.add(p)


def straightLine(x, y, length, drawing, scale):
    p = drawing.path(stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x, y))
    p.push(goVertical(length))
    p.push(goHorizontal(1))
    p.push(goVertical(-length))
    p.push(finishPath())
    drawing.add(p)


def lowerSemiCircle(x1, y1, x2, y2, drawing, scale):
    p = drawing.path(stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x1, y1))
    radius = (math.sqrt((int(x1)-int(x2))**2 + (int(y1)-int(y2))**2))/2
    p.push_arc((x2,y2),180,radius,large_arc=True,angle_dir = "-",absolute=True)
    p.push(goHorizontal(1))
    p.push_arc((int(x1)-1,int(y1)),180,radius,large_arc=True,angle_dir="+",absolute=True)
    p.push(finishPath())
    drawing.add(p)


def bottom(x1, y1, x2, y2, x3, y3, drawing, color="white", scale=1):
    p = drawing.path(style="fill:"+color)
    p.push(goToCoord(x1, y1))
    relativ_x2 = x2 - x1
    relativ_y2 = y2 - y1
    p.push(bezier(relativ_x2, relativ_y2))
    relativ_x3 = x3 - x2
    p.push(goHorizontal(relativ_x3))
    relativ_xlast = x1 - x3 + 40 * scale
    p.push(bezier(relativ_xlast, -relativ_y2))
    p.push(finishPath())
    drawing.add(p)
    p = drawing.path(stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x1, y1))
    p.push(bezier(relativ_x2, relativ_y2))
    p.push(goHorizontal(relativ_x3))
    p.push(bezier(relativ_xlast, -relativ_y2))
    p.push(goHorizontal(-1))
    p.push(bezier(-relativ_xlast, relativ_y2 - 1))
    p.push(goHorizontal(-relativ_x3+2))
    p.push(bezier(-relativ_x2, -relativ_y2))
    p.push(finishPath())
    drawing.add(p)


def top(x1, y1, x2, y2, drawing, scale=1):
    p = drawing.path(stroke="black", stroke_width=str(math.ceil(scale)))
    p.push(goToCoord(x1, y1))
    p.push(goVertical(-60 * scale))
    relativ_x2 = int(x2)-int(x1)
    p.push(goHorizontal(relativ_x2))
    p.push(goVertical(60 * scale))
    p.push(goHorizontal(-1))
    p.push(goVertical(-(60 * scale - 1)))
    p.push(goHorizontal(-relativ_x2 + 2))
    p.push(goVertical(60 * scale -1))
    p.push(finishPath())
    drawing.add(p)
