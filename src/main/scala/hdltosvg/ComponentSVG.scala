package hdltosvg

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex
import scala.collection.mutable.{ArrayBuffer, LinkedHashMap}

import java.awt.{Font, Color}
import java.awt.geom._
import java.io._

import de.erichseifert.vectorgraphics2d._
import de.erichseifert.vectorgraphics2d.util._ 

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
  object StyleMinimal extends ArrowStyle 
}

case class OutputConfig (var figPadX: Double,
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
  var groupPolicy: GroupPolicy,
  var arrowLength: Double,
  var arrowPadX: Double,
  var arrowThickness: Double,
  var arrowTipSize: Double,
  var arrowStyle: ArrowStyle,
  var outputType: OutputType)

object OutputConfig {

  def default() : OutputConfig = {
    new OutputConfig(4.0,
      4.0, 
      1.0, 
      2.0,
      new Font("Courier", Font.BOLD, 11),
      (VAlignment.Bottom, HAlignment.Right),
      0.25,
      Color.orange,
      new BoxSigNamePad(20.0), 
      2.0,
      1.0,
      1.0,
      new Font("Courier", Font.BOLD, 10),
      1.0,
      10.0,
      GroupPolicy.RegexGroupsPolicy(),
      30.0,
      0.0,
      1.0,
      3.0,
      ArrowStyle.StyleReentrant,
      OutputType.SVGTypeOutput)
  }
}

case class SignalInfo(name: String, desc: String, dir: ArrowDirection, sty: ArrowStyle) {
  def withFlippedArrow : SignalInfo = dir match {
    case ArrowDirection.PointRight => this.copy(dir=ArrowDirection.PointLeft)
    case ArrowDirection.PointLeft => this.copy(dir=ArrowDirection.PointRight)
    case ArrowDirection.PointBoth => this.copy()
  } 
}

case class ComponentInfo(componentName: String, 
  groupsLeft: Array[Array[SignalInfo]], 
  groupsRight: Array[Array[SignalInfo]])


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
  object PointLeft extends ArrowDirection
  object PointBoth extends ArrowDirection
}
object ComponentSVG {
  def fromUnsorted(componentInfo: ComponentInfo, out: OutputStream, config: OutputConfig) {

    fromSorted(sortComponent(componentInfo, config), out, config)
  }

  def fromSorted(componentInfo: ComponentInfo, out: OutputStream, config: OutputConfig) {

    val vg2d = new VectorGraphics2D()
    val compPositions = layout(vg2d, config, componentInfo)

    drawComponent(vg2d, config, compPositions)
    val processor = Processors.get(config.outputType.processor)
    val doc = processor.getDocument(vg2d.getCommands(),
                                    new PageSize(compPositions.figSize._1,
                                                 compPositions.figSize._2))
    doc.writeTo(out)
  }
}
