package spinal.creative

import scala.collection.mutable.ArrayBuffer
import math._

import java.awt.{Font, Color, BasicStroke, FontMetrics}
import java.awt.geom._
import java.io._

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

import de.erichseifert.vectorgraphics2d._
import de.erichseifert.vectorgraphics2d.util._ 

import spinal.core._
import spinal.core.internals._

sealed trait BoxWidthPolicy
case class BoxConstantWidth(width: Double) extends BoxWidthPolicy
case class BoxSigNamePad(pad: Double) extends BoxWidthPolicy
case class BoxConstantRatio(ratio: Double) extends BoxWidthPolicy

sealed abstract case class OutputType(processor : String)
object OutputType {
  object PDFTypeOutput extends OutputType("pdf")
  object EPSTypeOutput extends OutputType("eps")
  object SVGTypeOutput extends OutputType("svg")
}

sealed abstract trait VAlignment
object VAlignment {
  object Top extends VAlignment  
  object Bottom extends VAlignment
}

sealed abstract trait HAlignment
object HAlignment {
  object Left extends HAlignment  
  object Center extends HAlignment
  object Right extends HAlignment
}

sealed abstract trait ArrowStyle
  object ArrowStyle {
    object StyleNone extends ArrowStyle
    object StyleFull extends ArrowStyle
    object StyleEmpty extends ArrowStyle
    object StyleReentrant extends ArrowStyle
    object StyleMinimal extends ArrowStyle }


case class SpinalGraphicsConfig (var figPadX: Double,
  var figPadY: Double,
  var compPadX: Double,
  var compPadY: Double,
  var compFont: Font,
  var compAlignment: (VAlignment, HAlignment),
  var boxTickness: Double,
  var boxFillColor: Color,
  var boxWidthPolicy: BoxWidthPolicy,
  var boxBorderPad: Double,
  var sigPadX: Double,
  var sigPadY: Double,
  var sigFont: Font,
  var descPadX: Double,
  var groupPadY: Double,
  var arrowLength: Double,
  var arrowPadX: Double,
  var arrowThickness: Double,
  var arrowTipSize: Double,
  var arrowStyle: ArrowStyle,
  var outputType: OutputType)

object SpinalGraphicsConfig {

  def default() : SpinalGraphicsConfig = {
    new SpinalGraphicsConfig(4.0,
      4.0, 
      1.0, 
      2.0,
      new Font("Serif", Font.BOLD, 11),
      (VAlignment.Bottom, HAlignment.Right),
      0.25,
      Color.orange,
      new BoxSigNamePad(20.0), 
      2.0,
      1.0,
      1.0,
      new Font("Serif", Font.BOLD, 10),
      1.0,
      10.0,
      30.0,
      0.0,
      1.0,
      3.0,
      ArrowStyle.StyleReentrant,
      OutputType.SVGTypeOutput)
  }
}
object SpinalGraphics {

  case class ComponentFigurePositions (
    figSize: (Double, Double),
    boxPos: (Double, Double),
    boxSize: (Double, Double),
    componentName: String,
    componentPos: (Double, Double),
    arrowPositions: Array[(ArrowDirection, ArrowStyle, Double, Double)],
    labelPositions: Array[(String, Double, Double)])

  sealed abstract trait ArrowDirection
  object ArrowDirection {
    object PointRight extends ArrowDirection
    object PointBoth extends ArrowDirection
  }

  case class SignalInfo(name: String, desc: String, dir: ArrowDirection, sty: ArrowStyle)
  case class ComponentInfo(componentName: String, 
    groupsLeft: Array[Array[SignalInfo]], 
    groupsRight: Array[Array[SignalInfo]])


  def fromComponent[T <: Component](component: => T, filename: String) { fromComponent(component, filename, SpinalGraphicsConfig.default()) }
  def fromComponent[T <: Component](component: => T, filename: String, config: SpinalGraphicsConfig){
    fromComponent(component, new FileOutputStream(filename), config)
  }

  def fromComponent[T <: Component](component: => T, out: OutputStream){ fromComponent(component, out, SpinalGraphicsConfig.default()) }
  def fromComponent[T <: Component](component: => T, out: OutputStream, config: SpinalGraphicsConfig){
    fromReport(SpinalVhdl(component), out, config)
  }

  def fromReport[T <: Component](report: => SpinalReport[T], filename: String) { fromReport(report, filename, SpinalGraphicsConfig.default()) }
  def fromReport[T <: Component](report: => SpinalReport[T], filename: String, config: SpinalGraphicsConfig){
    fromReport(report, new FileOutputStream(filename), config)
  }

  def fromReport[T <: Component](report: => SpinalReport[T], out: OutputStream){ fromReport(report, out, SpinalGraphicsConfig.default()) }
  def fromReport[T <: Component](report: => SpinalReport[T], out: OutputStream, config: SpinalGraphicsConfig){

    val (vectorGraphics2D, sizeX, sizeY) = getVectorGraphics2D(report, config)
    val processor = Processors.get(config.outputType.processor)
    val doc = processor.getDocument(vectorGraphics2D.getCommands(), new PageSize(sizeX, sizeY))
    doc.writeTo(out)
  }

  def getVectorGraphics2D[T <: Component](report: => SpinalReport[T], config: SpinalGraphicsConfig) : (VectorGraphics2D, Double, Double) = {

    val compInfo = getComponentInfo(config, report)

    val vg2d = new VectorGraphics2D()
    val compPositions = getComponentFigurePositions(vg2d, config, compInfo)
    drawBox(vg2d, config, compPositions)
    drawComponentName(vg2d, config, compPositions)
    drawLabels(vg2d, config, compPositions)
    drawArrows(vg2d, config, compPositions)

    (vg2d, compPositions.figSize._1, compPositions.figSize._2)
  }

  def getComponentInfo[T <: Component](config: SpinalGraphicsConfig, report: => SpinalReport[T]) : ComponentInfo = {

    var inSigs : ArrayBuffer[SignalInfo] = ArrayBuffer()
    var outSigs : ArrayBuffer[SignalInfo] = ArrayBuffer()
    var inOutSigs : ArrayBuffer[SignalInfo] = ArrayBuffer()

    val regexIn    = """.* : in (.*)\)""".r
    val regexOut   = """.* : out (.*)\)""".r
    val regexInOut = """.* : inout (.*)\)""".r

    for (sig <- report.toplevel.getAllIo) {
      sig.toString match {
        case regexIn(identifier) => inSigs += new SignalInfo(sig.getName, identifier, ArrowDirection.PointRight, config.arrowStyle)
        case regexOut(identifier) => outSigs += new SignalInfo(sig.getName, identifier, ArrowDirection.PointRight, config.arrowStyle)
        case regexInOut(identifier) => inOutSigs += new SignalInfo(sig.getName, identifier, ArrowDirection.PointBoth, config.arrowStyle)
        case _ =>
      }
    }
    new ComponentInfo(report.toplevelName, 
      Array(inSigs.toArray, inOutSigs.toArray).filter(_.length > 0), 
      Array(outSigs.toArray).filter(_.length > 0))
  }

  def getGroupsHeight(config: SpinalGraphicsConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics) : Double = {
    val chHeight = fontMetrics.getHeight
    val groupsPadHeight = if (groups.length > 1) (groups.length - 1) * config.groupPadY else 0
    val labelsHeight = groups.flatten.length * chHeight
    val labelsPad = if (groups.flatten.length > 1) (groups.flatten.length - 1) * config.sigPadY else 0
    groupsPadHeight + labelsHeight + labelsPad
  }

  def getGroupsWidth(config: SpinalGraphicsConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics) : Double = {
    groups.flatten
      .map(x => fontMetrics.stringWidth(x.name))
      .reduce((y, z) => y max z)
  }

  def getBoxSize(config: SpinalGraphicsConfig, compInfo: ComponentInfo, fontMetrics: FontMetrics) : (Double, Double) = {

    val leftGroupsHeight = getGroupsHeight(config, compInfo.groupsLeft, fontMetrics)
    val rightGroupsHeight = getGroupsHeight(config, compInfo.groupsRight, fontMetrics)
    val boxHeight = 2*config.boxBorderPad + max(leftGroupsHeight, rightGroupsHeight)

    val leftGroupsWidth = getGroupsWidth(config, compInfo.groupsLeft, fontMetrics)
    val rightGroupsWidth = getGroupsWidth(config, compInfo.groupsRight, fontMetrics)

    val boxWidth = config.boxWidthPolicy match {
      case BoxConstantWidth(width) => width
      case BoxSigNamePad(pad) => leftGroupsWidth + rightGroupsWidth + pad + 2*config.sigPadX
      case BoxConstantRatio(ratio) => boxHeight * ratio
    }

    (boxWidth, boxHeight)
  }

  def getGroupsDescWidth(config: SpinalGraphicsConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics) : Double = {
    groups.flatten
      .map(x => fontMetrics.stringWidth(x.desc))
      .reduce((y, z) => y max z)
  }

  def getFigWidth(vg2d: VectorGraphics2D, config: SpinalGraphicsConfig, compInfo: ComponentInfo, boxSize: (Double, Double)) : Double = {
    val labelFontMetrics = vg2d.getFontMetrics(config.sigFont)
    val leftGroupDescWidth = getGroupsDescWidth(config, compInfo.groupsLeft, labelFontMetrics)
    val rightGroupDescWidth = getGroupsDescWidth(config, compInfo.groupsRight, labelFontMetrics)
    boxSize._1 + leftGroupDescWidth + rightGroupDescWidth + ((config.arrowPadX + config.arrowLength + config.descPadX + config.figPadX)*2)
  }

  def getFigHeight(vg2d: VectorGraphics2D, config: SpinalGraphicsConfig, compInfo: ComponentInfo, boxSize: (Double, Double)) : Double = {
    val componentLabelHeight = vg2d.getFontMetrics(config.compFont).getHeight
    componentLabelHeight + boxSize._2 + config.compPadY + (config.figPadY*2)
  }

  def getFigSize(vg2d: VectorGraphics2D, config: SpinalGraphicsConfig, compInfo: ComponentInfo, boxSize: (Double, Double)) : (Double, Double) = {
    (getFigWidth(vg2d, config, compInfo, boxSize), getFigHeight(vg2d, config, compInfo, boxSize))
  }

  def getBoxPosition(vg2d: VectorGraphics2D, config: SpinalGraphicsConfig, compInfo: ComponentInfo, boxSize: (Double, Double)) : (Double, Double) = {
    val labelFontMetrics = vg2d.getFontMetrics(config.sigFont)
    val leftGroupDescWidth = getGroupsDescWidth(config, compInfo.groupsLeft, labelFontMetrics)
    val componentLabelHeight = vg2d.getFontMetrics(config.compFont).getHeight

    val boxPosX = leftGroupDescWidth + config.arrowPadX + config.descPadX + config.figPadX + config.arrowLength
    val boxPosY = (if (config.compAlignment._1 == VAlignment.Top) componentLabelHeight + config.compPadY else 0) + config.figPadY
    (boxPosX, boxPosY)
  }

  def getComponentNamePosition(vg2d: VectorGraphics2D, config: SpinalGraphicsConfig, compInfo: ComponentInfo, boxSize: (Double, Double)) : (Double, Double) = {
    val componentMetrics = vg2d.getFontMetrics(config.compFont)
    val componentAscent = componentMetrics.getAscent
    val componentDescent = componentMetrics.getDescent
    val componentWidth = componentMetrics.stringWidth(compInfo.componentName)
    val componentPosX = config.compAlignment._2 match {
      case HAlignment.Left => config.compPadX
      case HAlignment.Center => (boxSize._1 - componentWidth)/2
      case HAlignment.Right => boxSize._1 - componentWidth - config.compPadX
    }
    val componentPosY = if (config.compAlignment._1 == VAlignment.Top) {
      -config.compPadY - componentDescent
    } else {
      boxSize._2 + config.compPadY + componentAscent
    }

    (componentPosX, componentPosY)
  }

  def extractNamesDescsArrowsStylesFromGroups(groups: Array[Array[SignalInfo]]) : (Array[String], Array[String], Array[(ArrowDirection, ArrowStyle)]) = {
    val flatGroups = groups.flatten
    val names  = flatGroups.map(x => x.name)
    val descs  = flatGroups.map(x => x.desc)
    val arrows = flatGroups.map(x => (x.dir, x.sty))
    (names, descs, arrows)
  }


  def getGroupsYs(config: SpinalGraphicsConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics) : Array[Double] = {
    val labelAscent = fontMetrics.getAscent
    val labelHeight = fontMetrics.getHeight
    var posY = config.boxBorderPad + labelAscent
    val yS : ArrayBuffer[Double] = ArrayBuffer()
    for (group <- groups) {
      for (sigInfo <- group) {
        yS += posY
        posY += labelHeight + config.sigPadY
      }
      posY -= config.sigPadY
      posY += config.groupPadY
    }

    yS.toArray
  }

  def getSigsXLeft(config: SpinalGraphicsConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics, boxSize: (Double, Double)) : Array[Double] = {
    Array.fill(groups.flatten.length)(config.sigPadX)
  }

  def getSigsXRight(config: SpinalGraphicsConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics, boxSize: (Double, Double)) : Array[Double] = {
    groups.flatten.map(x => boxSize._1 - config.sigPadX - fontMetrics.stringWidth(x.name))
  }

  def getDescXLeft(config: SpinalGraphicsConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics, boxSize: (Double, Double)) : Array[Double] = {
    groups.flatten.map(x =>  - config.arrowPadX - config.arrowLength - config.descPadX - fontMetrics.stringWidth(x.desc))
  }

  def getDescXRight(config: SpinalGraphicsConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics, boxSize: (Double, Double)) : Array[Double] = {
    Array.fill(groups.flatten.length)(config.arrowPadX + config.arrowLength + config.descPadX + boxSize._1)
  }

  def getArrowYs(groupYs: Array[Double], fontMetrics: FontMetrics) : Array[Double] = {
    val labelHeight = fontMetrics.getHeight
    groupYs.map(_-labelHeight/4)
  }

  def getArrowXLeft(config: SpinalGraphicsConfig, groups: Array[Array[SignalInfo]], boxSize: (Double, Double)) : Array[Double] = {
    Array.fill(groups.flatten.length)(- config.arrowPadX - config.arrowLength)
  }

  def getArrowXRight(config: SpinalGraphicsConfig, groups: Array[Array[SignalInfo]], boxSize: (Double, Double)) : Array[Double] = {
    Array.fill(groups.flatten.length)(config.arrowPadX + boxSize._1)
  }

  def getComponentFigurePositions(vg2d : VectorGraphics2D, config: SpinalGraphicsConfig, compInfo: ComponentInfo) : ComponentFigurePositions = {
    
    val labelFontMetrics = vg2d.getFontMetrics(config.sigFont)
    
    val groupsLeft = compInfo.groupsLeft
    val groupsRight = compInfo.groupsRight

    val boxSize = getBoxSize(config, compInfo, labelFontMetrics)
    val boxPos = getBoxPosition(vg2d, config, compInfo, boxSize)
    val figSize = getFigSize(vg2d, config, compInfo, boxSize)
    val compNamePosition = getComponentNamePosition(vg2d, config, compInfo, boxSize)
    val sigsXLeft = getSigsXLeft(config, groupsLeft, labelFontMetrics, boxSize)
    val sigsXRight= getSigsXRight(config, groupsRight, labelFontMetrics, boxSize)
    val descXLeft = getDescXLeft(config, groupsLeft, labelFontMetrics, boxSize)
    val descXRight= getDescXRight(config, groupsRight, labelFontMetrics, boxSize)
    val groupsYsLeft  = getGroupsYs(config, groupsLeft, labelFontMetrics)
    val groupsYsRight = getGroupsYs(config, groupsRight, labelFontMetrics)
    val arrowYLeft   = getArrowYs(groupsYsLeft, labelFontMetrics)
    val arrowYRight  = getArrowYs(groupsYsRight, labelFontMetrics)
    val arrowXLeft    = getArrowXLeft(config, groupsLeft, boxSize)
    val arrowXRight   = getArrowXRight(config, groupsRight, boxSize)
    val (namesLeft, descsLeft, arrowsLeft) = extractNamesDescsArrowsStylesFromGroups(groupsLeft)
    val (namesRight, descsRight, arrowsRight) = extractNamesDescsArrowsStylesFromGroups(groupsRight)

    val labels = namesLeft ++ namesRight ++ descsLeft ++ descsRight
    val labelPosX = sigsXLeft ++ sigsXRight ++ descXLeft ++ descXRight 
    val labelPosY = groupsYsLeft ++ groupsYsRight ++ groupsYsLeft ++ groupsYsRight

    val arrows  = arrowsLeft ++ arrowsRight
    val arrowsX = arrowXLeft ++ arrowXRight
    val arrowsY = arrowYLeft ++ arrowYRight

    val arrowPos =  arrows.zip(arrowsX zip arrowsY).map({ case ((dir, sty), (x, y)) => (dir, sty, x, y) })
    val labelsPos = labels.zip(labelPosX zip labelPosY).map({ case (lab, (x, y)) => (lab, x, y) })

    new ComponentFigurePositions(figSize=figSize, 
      boxPos=boxPos, 
      boxSize=boxSize, 
      componentName=compInfo.componentName, 
      componentPos=compNamePosition, 
      arrowPositions=arrowPos, 
      labelsPos)
  }

  def drawBox(vg2d : VectorGraphics2D, config: SpinalGraphicsConfig, compPositions: ComponentFigurePositions) {

    println(compPositions)

    vg2d.translate(compPositions.boxPos._1.toInt, compPositions.boxPos._2.toInt)
    vg2d.setColor(config.boxFillColor)
    vg2d.fillRect(0, 0, compPositions.boxSize._1.toInt, compPositions.boxSize._2.toInt)
    vg2d.setColor(Color.black)
    vg2d.setStroke(new BasicStroke(config.boxTickness.toFloat))
    vg2d.drawRect(0, 0, compPositions.boxSize._1.toInt, compPositions.boxSize._2.toInt)
  }

  def drawComponentName(vg2d : VectorGraphics2D, config: SpinalGraphicsConfig, compPositions: ComponentFigurePositions) {
    vg2d.setFont(config.compFont)
    val (x, y) = compPositions.componentPos
    vg2d.drawString(compPositions.componentName, x.toInt, y.toInt)
  }
  def drawLabels(vg2d : VectorGraphics2D, config: SpinalGraphicsConfig, compPositions: ComponentFigurePositions) {
    vg2d.setFont(config.sigFont)
    for ((str, x, y) <- compPositions.labelPositions) vg2d.drawString(str, x.toInt, y.toInt)
  }
  def drawArrows(vg2d : VectorGraphics2D, config: SpinalGraphicsConfig, compPositions: ComponentFigurePositions) {

    vg2d.setStroke(new BasicStroke(config.arrowThickness.toFloat))
    for ((dir, sty, x, y) <- compPositions.arrowPositions) {  
      sty match {
        case ArrowStyle.StyleNone      => vg2d.drawLine(x.toInt, y.toInt,  (x+config.arrowLength).toInt, y.toInt)
        case ArrowStyle.StyleFull      => drawArrowFull(vg2d, config, dir, x, y)
        case ArrowStyle.StyleEmpty     => drawArrowEmpty(vg2d, config, dir, x, y)
        case ArrowStyle.StyleReentrant => drawArrowReentrant(vg2d, config, dir, x, y)
        case ArrowStyle.StyleMinimal   => drawArrowMinimal(vg2d, config, dir, x, y)
      }

    }
  }

  def drawArrowFull(vg2d : VectorGraphics2D, config: SpinalGraphicsConfig, dir: ArrowDirection, x: Double, y: Double) {
    val xend = (x+config.arrowLength) 
    val xVertexL = Array(x, x + 2*config.arrowTipSize, x + 2*config.arrowTipSize).map(_.toInt)
    val yVertexL = Array(y, y + config.arrowTipSize, y - config.arrowTipSize).map(_.toInt)
    val xVertexR = Array(xend, xend - 2*config.arrowTipSize, xend - 2*config.arrowTipSize).map(_.toInt)
    val yVertexR = Array(y, y + config.arrowTipSize, y - config.arrowTipSize).map(_.toInt)

    dir match {
      case ArrowDirection.PointRight => {
        vg2d.drawLine(x.toInt, y.toInt,  (xend - config.arrowTipSize).toInt, y.toInt)
        vg2d.fillPolygon(xVertexR, yVertexR, 3)
      }
      
      case ArrowDirection.PointBoth => {
        vg2d.drawLine((x + config.arrowTipSize).toInt, y.toInt,  (xend.toInt - config.arrowTipSize).toInt, y.toInt)
        vg2d.fillPolygon(xVertexR, yVertexR, 3)
        vg2d.fillPolygon(xVertexL, yVertexL, 3)
      }
    }
  }

  def drawArrowEmpty(vg2d : VectorGraphics2D, config: SpinalGraphicsConfig, dir: ArrowDirection, x: Double, y: Double) {
    val xend = (x+config.arrowLength) 
    val xVertexL = Array(x, x + 2*config.arrowTipSize, x + 2*config.arrowTipSize).map(_.toInt)
    val yVertexL = Array(y, y + config.arrowTipSize, y - config.arrowTipSize).map(_.toInt)
    val xVertexR = Array(xend, xend - 2*config.arrowTipSize, xend - 2*config.arrowTipSize).map(_.toInt)
    val yVertexR = Array(y, y + config.arrowTipSize, y - config.arrowTipSize).map(_.toInt)

    dir match {
      case ArrowDirection.PointRight => {
        vg2d.drawLine(x.toInt, y.toInt,  (xend - config.arrowTipSize).toInt, y.toInt)
        vg2d.setColor(Color.white)
        vg2d.fillPolygon(xVertexR, yVertexR, 3)
        vg2d.setColor(Color.black)
        vg2d.drawPolygon(xVertexR, yVertexR, 3)
      }
      
      case ArrowDirection.PointBoth => {
        vg2d.drawLine((x + config.arrowTipSize).toInt, y.toInt,  (xend.toInt - config.arrowTipSize).toInt, y.toInt)
        vg2d.setColor(Color.white)
        vg2d.fillPolygon(xVertexR, yVertexR, 3)
        vg2d.fillPolygon(xVertexL, yVertexL, 3)
        vg2d.setColor(Color.black)
        vg2d.drawPolygon(xVertexR, yVertexR, 3)
        vg2d.drawPolygon(xVertexL, yVertexL, 3)
      }
    }
  }

  def drawArrowReentrant(vg2d : VectorGraphics2D, config: SpinalGraphicsConfig, dir: ArrowDirection, x: Double, y: Double) {
    val xend = (x+config.arrowLength) 
    val xVertexL = Array(x, x + 2*config.arrowTipSize, x + config.arrowTipSize, x + 2*config.arrowTipSize).map(_.toInt)
    val yVertexL = Array(y, y + config.arrowTipSize, y, y - config.arrowTipSize).map(_.toInt)
    val xVertexR = Array(xend, xend - 2*config.arrowTipSize, xend - config.arrowTipSize, xend - 2*config.arrowTipSize).map(_.toInt)
    val yVertexR = Array(y, y + config.arrowTipSize, y, y - config.arrowTipSize).map(_.toInt)

    dir match {
      case ArrowDirection.PointRight => {
        vg2d.drawLine(x.toInt, y.toInt,  (xend - config.arrowTipSize).toInt, y.toInt)
        vg2d.fillPolygon(xVertexR, yVertexR, 4)
      }
      
      case ArrowDirection.PointBoth => {
        vg2d.drawLine((x + config.arrowTipSize).toInt, y.toInt,  (xend.toInt - config.arrowTipSize).toInt, y.toInt)
        vg2d.fillPolygon(xVertexR, yVertexR, 4)
        vg2d.fillPolygon(xVertexL, yVertexL, 4)
      }
    }
  }

  def drawArrowMinimal(vg2d : VectorGraphics2D, config: SpinalGraphicsConfig, dir: ArrowDirection, x: Double, y: Double) {
    val xend = (x+config.arrowLength) 
    val xVertexL = Array(x, x + 2*config.arrowTipSize, x + 2*config.arrowTipSize).map(_.toInt)
    val yVertexL = Array(y, y + config.arrowTipSize, y - config.arrowTipSize).map(_.toInt)
    val xVertexR = Array(xend, xend - 2*config.arrowTipSize, xend - 2*config.arrowTipSize).map(_.toInt)
    val yVertexR = Array(y, y + config.arrowTipSize, y - config.arrowTipSize).map(_.toInt)

    dir match {
      case ArrowDirection.PointRight => {
        vg2d.drawLine(x.toInt, y.toInt,  (xend - config.arrowThickness/2).toInt, y.toInt)
        vg2d.drawLine(xVertexR(0), yVertexR(0), xVertexR(1), yVertexR(1))
        vg2d.drawLine(xVertexR(0), yVertexR(0), xVertexR(2), yVertexR(2))
      }
      
      case ArrowDirection.PointBoth => {
        vg2d.drawLine((x + config.arrowThickness/2).toInt, y.toInt,  (xend.toInt - config.arrowThickness/2).toInt, y.toInt)
        vg2d.drawLine(xVertexR(0), yVertexR(0), xVertexR(1), yVertexR(1))
        vg2d.drawLine(xVertexR(0), yVertexR(0), xVertexR(2), yVertexR(2))
        vg2d.drawLine(xVertexL(0), yVertexL(0), xVertexL(1), yVertexL(1))
        vg2d.drawLine(xVertexL(0), yVertexL(0), xVertexL(2), yVertexL(2))
      }
    }
  }
}
