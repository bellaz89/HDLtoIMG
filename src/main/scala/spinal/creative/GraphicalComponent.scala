package spinal.creative

import scala.collection.mutable.ArrayBuffer
import math._

import java.awt.{Font, Color, BasicStroke}
import java.awt.geom._
import java.io._

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
                                 var outputType: OutputType)

object SpinalGraphicsConfig {

  def default() : SpinalGraphicsConfig = {
    new SpinalGraphicsConfig(4.0,
                             4.0, 
                             1.0, 
                             1.0,
                             new Font("Serif", Font.BOLD, 10),
                             (VAlignment.Top, HAlignment.Center),
                             0.25,
                             Color.orange,
                             new BoxSigNamePad(20.0), 
                             1.5,
                             1.0,
                             1.0,
                             new Font("Serif", Font.BOLD, 10),
                             1.0,
                             1.0,
                             10.0,
                             1.0,
                             0.75,
                             OutputType.PDFTypeOutput)
  }
}
object SpinalGraphics {

  case class ComponentFigurePositions (
    figSize: (Double, Double),
    boxPos: (Double, Double),
    boxSize: (Double, Double),
    componentName: String,
    componentPos: (Double, Double),
    arrowPositions: Array[(ArrowDirection, Double, Double)],
    labelPositions: Array[(String, Double, Double)])


  case class SignalInfo(name: String, desc: String)
  case class ComponentInfo(componentName: String, 
                           inSigs: Array[SignalInfo], 
                           outSigs: Array[SignalInfo], 
                           inOutSigs: Array[SignalInfo])

  sealed abstract trait ArrowDirection
  object ArrowDirection {
    object PointRight extends ArrowDirection
    object PointBoth extends ArrowDirection
  }
  
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
    
    val compInfo = getComponentInfo(report)
    
    val vg2d = new VectorGraphics2D()
    val compPositions = getComponentFigurePositions(vg2d, config, compInfo)
    drawBox(vg2d, config, compPositions)
    drawComponentName(vg2d, config, compPositions)
    drawLabels(vg2d, config, compPositions)
    drawArrows(vg2d, config, compPositions)

    (vg2d, compPositions.figSize._1, compPositions.figSize._2)
  }

  def getComponentInfo[T <: Component](report: => SpinalReport[T]) : ComponentInfo = {
    
    var inSigs : ArrayBuffer[SignalInfo] = ArrayBuffer()
    var outSigs : ArrayBuffer[SignalInfo] = ArrayBuffer()
    var inOutSigs : ArrayBuffer[SignalInfo] = ArrayBuffer()
    
    val regexIn    = """.* : in (.*)\)""".r
    val regexOut   = """.* : out (.*)\)""".r
    val regexInOut = """.* : inout (.*)\)""".r

    for (sig <- report.toplevel.getAllIo) {
      //println(sig.getName())
      sig.toString match {
        case regexIn(identifier) => inSigs += new SignalInfo(sig.getName, identifier)
        case regexOut(identifier) => outSigs += new SignalInfo(sig.getName, identifier)
        case regexInOut(identifier) => inOutSigs += new SignalInfo(sig.getName, identifier)
        case _ =>
      }
    }
    new ComponentInfo(report.toplevelName, inSigs.toArray, outSigs.toArray, inOutSigs.toArray)
  }

  def getComponentFigurePositions(vg2d : VectorGraphics2D, config: SpinalGraphicsConfig, compInfo: ComponentInfo) : ComponentFigurePositions = {
   
    val labelFontMetrics = vg2d.getFontMetrics(config.sigFont)
    val labelHeight = labelFontMetrics.getHeight()

    var inSigsRelY = (1 to compInfo.inSigs.length).map((x: Int) => (x * (labelHeight + config.sigPadY))-labelHeight/2 + config.boxBorderPad: Double).toArray
    val inOutSigsRelStartY = if (inSigsRelY.length > 0) inSigsRelY.last + config.groupPadY else inSigsRelY.last 

    var inOutSigsRelY = (0 to compInfo.inOutSigs.length).map((x: Int) => (x * (labelHeight + config.sigPadY)) + inOutSigsRelStartY: Double).toArray
    val leftEndY = inOutSigsRelY.last + config.boxBorderPad
    inOutSigsRelY = inOutSigsRelY.dropRight(1)

    var outSigsRelY = (0 to compInfo.outSigs.length).map((x: Int) => (x * (labelHeight + config.sigPadY)) + config.boxBorderPad: Double).toArray
    val rightEndY = outSigsRelY.last + config.boxBorderPad
    outSigsRelY = outSigsRelY.dropRight(1)

    val boxHeight = leftEndY max rightEndY

    val maxStrLenLeft = (compInfo.inSigs ++ compInfo.inOutSigs).foldLeft(0.0)((maxi: Double, x: SignalInfo) => labelFontMetrics.stringWidth(x.name) max maxi)
    val maxStrLenRight = compInfo.outSigs.foldLeft(0.0)((maxi: Double, x: SignalInfo) => labelFontMetrics.stringWidth(x.name) max maxi)

    val boxWidth = config.boxWidthPolicy match {
      case BoxConstantWidth(width) => width
      case BoxSigNamePad(pad) => maxStrLenLeft + maxStrLenRight + pad + 2*config.sigPadX
      case BoxConstantRatio(ratio) => boxHeight * ratio
    }

    val inSigsRelX = Array.fill(compInfo.inSigs.length)(config.sigPadX)
    val inOutSigsRelX = Array.fill(compInfo.inOutSigs.length)(config.sigPadX)
    val outSigsRelX = compInfo.outSigs.map((x: SignalInfo) => boxWidth - config.sigPadX - labelFontMetrics.stringWidth(x.name): Double)

    val sigsLabels = (compInfo.inSigs ++ compInfo.inOutSigs ++ compInfo.outSigs).map(_.name)
    val sigsX = (inSigsRelX ++ inOutSigsRelX ++ outSigsRelX)
    val sigsY = (inSigsRelY ++ inOutSigsRelX ++ outSigsRelY)
    val sigPos = sigsX zip sigsY

    val sigsLabelPositions = sigsLabels.zip(sigPos).map({case (str, (x, y)) => (str, x, y)})

    val maxDescLenLeft = (compInfo.inSigs ++ compInfo.inOutSigs).foldLeft(0.0)((maxi: Double, x: SignalInfo) => labelFontMetrics.stringWidth(x.desc) max maxi)
    val maxDescLenRight = compInfo.outSigs.foldLeft(0.0)((maxi: Double, x: SignalInfo) => labelFontMetrics.stringWidth(x.desc) max maxi)

    val compFontMetrics = vg2d.getFontMetrics()

    val compHeight = compFontMetrics.getHeight()
    val compWidth = compFontMetrics.stringWidth(compInfo.componentName)

    val boxPosX = config.figPadX + maxDescLenLeft + config.descPadX + config.arrowLength + config.arrowPadX
    val boxPosY = (if (config.compAlignment._1 == VAlignment.Top) config.compPadY + compHeight else 0) + config.figPadY

    val figSizeX = boxWidth + maxDescLenLeft + maxStrLenRight + 2*(config.figPadX + config.descPadX + config.arrowLength + config.arrowPadX)
    val figSizeY = boxHeight + config.compPadY + compHeight + config.figPadY*2

    val inArrowY = inSigsRelY.map(_-(labelHeight/4))
    val inOutArrowY = inOutSigsRelY.map(_-(labelHeight/4))
    val outArrowY = outSigsRelY.map(_-(labelHeight/4))

    val inArrowX = Array.fill(compInfo.inSigs.length)(-config.arrowPadX-config.arrowLength)
    val inOutArrowX = Array.fill(compInfo.inOutSigs.length)(-config.arrowPadX-config.arrowLength)
    val outArrowX = Array.fill(compInfo.outSigs.length)(config.arrowPadX)
    
    val inArrowPos = inArrowX.zip(inArrowY).map({case (x,y) => (ArrowDirection.PointRight, x, y) : (ArrowDirection, Double, Double)})
    val inOutArrowPos = inOutArrowX.zip(inOutArrowY).map({case (x,y) => (ArrowDirection.PointBoth, x, y) : (ArrowDirection, Double, Double)})
    val outArrowPos = outArrowX.zip(outArrowX).map({case (x,y) => (ArrowDirection.PointRight, x, y) : (ArrowDirection, Double, Double)})

    val arrowPos = inArrowPos ++ inOutArrowPos ++ outArrowPos

    new ComponentFigurePositions(figSize=(figSizeX, figSizeY), 
                                 boxPos=(boxPosX, boxPosY), 
                                 boxSize=(boxWidth, boxHeight), 
                                 componentName=compInfo.componentName, 
                                 componentPos=null, 
                                 arrowPositions=arrowPos, 
                                 sigsLabelPositions)
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

  def drawComponentName(vg2d : VectorGraphics2D, config: SpinalGraphicsConfig, compPositions: ComponentFigurePositions) {}
  def drawLabels(vg2d : VectorGraphics2D, config: SpinalGraphicsConfig, compPositions: ComponentFigurePositions) {
    vg2d.setFont(config.sigFont)
    for ((str, x, y) <- compPositions.labelPositions) vg2d.drawString(str, x.toInt, y.toInt)
  }
  def drawArrows(vg2d : VectorGraphics2D, config: SpinalGraphicsConfig, compPositions: ComponentFigurePositions) {

    vg2d.setStroke(new BasicStroke(config.arrowThickness.toFloat))
    for ((dir, x, y) <- compPositions.arrowPositions) vg2d.drawLine(x.toInt, y.toInt, 
                                                                    (x+config.arrowLength).toInt, y.toInt)
  }
}
