package hdltosvg

import scala.io.Source
import java.io.{File, FileInputStream, BufferedReader}
import java.nio.file.Files;
import java.awt.{Font, Color}
import cats.syntax.either._
import io.circe._
import io.circe.parser._

object obtainConfig {

  def default() : OutputConfig = {
    val defaultSrc = scala.io.Source
                      .fromURL(getClass.getResource("/default.json"))
                      .mkString

    val defaultJson = parse(defaultSrc).getOrElse(Json.Null)
    getConfFromJson(defaultJson)
  }
  
  def apply(file: File) : OutputConfig = {
    val src = Source.fromFile(file).getLines.mkString
    val json = parse(src).getOrElse(Json.Null)
    getConfFromJson(json)
  }

  def getConfFromJson(json: Json) : OutputConfig = {

    val cursor: HCursor = json.hcursor
    val conf = OutputConfig.default()
    cursor.downField("figPadY").as[Double] match { case Right(x) => conf.figPadX = x}
    cursor.downField("compPadX").as[Double] match { case Right(x) => conf.compPadX = x}
    cursor.downField("compPadY").as[Double] match { case Right(x) => conf.compPadY = x}

    val fontC = cursor.downField("compFont").downField("font").as[String] match { case Right(x) => x}
    val weightC = cursor.downField("compFont").downField("weight").as[String] match { case Right(x) => x}
    val sizeC = cursor.downField("compFont").downField("size").as[Int] match { case Right(x) => x}  
    conf.compFont = new Font(fontC,
                              weightC match {
                                case "PLAIN"      => Font.PLAIN
                                case "BOLD"       => Font.BOLD
                                case "ITALIC"     => Font.ITALIC
                                case "BOLDITALIC" => Font.BOLD + Font.ITALIC
                              },
                              sizeC)
    
    val List(vAlignStr, hAlignStr) = cursor.downField("compAlignment")
      .as[List[String]] match { case  Right(x) => x}

    val vAlign = vAlignStr match {
      case "Top"    => VAlignment.Top    
      case "Bottom" => VAlignment.Bottom
    }
    val hAlign = hAlignStr match {
      case "Left"   => HAlignment.Left
      case "Center" => HAlignment.Center
      case "Right"  => HAlignment.Right
    }

    conf.compAlignment = (vAlign, hAlign)

    cursor.downField("boxTickness").as[Double] match { case Right(x) => conf.boxTickness = x}

    cursor.downField("boxFillColor").as[List[Double]] match 
      { case Right(List(r, g, b)) => conf.boxFillColor = new Color(r.toFloat, 
                                                                   g.toFloat,
                                                                   b.toFloat)}
    
    
    val bPolicyTypeStr = cursor.downField("boxWidthPolicy")
                               .downField("type")
                               .as[String] match {case Right(x) => x}

    conf.boxWidthPolicy = bPolicyTypeStr match {
      case "BoxConstantWidth" => {
        BoxConstantWidth(cursor.downField("boxWidthPolicy")
          .downField("width")
          .as[Double] match { case Right(x) => x})
      }
      case "BoxSigNamePad" => {
        BoxSigNamePad(cursor.downField("boxWidthPolicy")
          .downField("pad")
          .as[Double] match { case Right(x) => x})
      }
      case "BoxConstantRatio" => {
        BoxConstantRatio(cursor.downField("boxWidthPolicy")
          .downField("ratio")
          .as[Double] match { case Right(x) => x})
      }
    }

    cursor.downField("boxBorderPad").as[Double] match { case Right(x) => conf.boxBorderPad = x}
    cursor.downField("sigPadX").as[Double] match { case Right(x) => conf.sigPadX = x}
    cursor.downField("sigPadY").as[Double] match { case Right(x) => conf.sigPadX = x}
    
    val fontS = cursor.downField("sigFont").downField("font").as[String] match { case Right(x) => x}
    val weightS = cursor.downField("sigFont").downField("weight").as[String] match { case Right(x) => x}
    val sizeS = cursor.downField("sigFont").downField("size").as[Int] match { case Right(x) => x}  
    conf.sigFont = new Font(fontS,
                            weightS match {
                              case "PLAIN"      => Font.PLAIN
                              case "BOLD"       => Font.BOLD
                              case "ITALIC"     => Font.ITALIC
                              case "BOLDITALIC" => Font.BOLD + Font.ITALIC
                            },
                            sizeS)   

    cursor.downField("descPadX").as[Double] match { case Right(x) => conf.descPadX = x}
    cursor.downField("groupPadY").as[Double] match { case Right(x) => conf.groupPadY = x}

    val groupPolicyStr = cursor.downField("groupPolicy")
      .downField("type")
      .as[String] match { case Right(x) => x }

    conf.groupPolicy = groupPolicyStr match {
      case "SimplePolicy" => GroupPolicy.SimplePolicy
      case "RegexGroupsPolicy" => {
        cursor.downField("groupPolicy")
          .downField("groupRegex")
          .as[String] match { 
                              case Right(x) => GroupPolicy.RegexGroupsPolicy(x.r)
                              case _ => GroupPolicy.RegexGroupsPolicy() 
                            }
      }
    }

    cursor.downField("arrowLength").as[Double] match { case Right(x) => conf.arrowLength = x}
    cursor.downField("arrowPadX").as[Double] match { case Right(x) => conf.arrowPadX = x}
    cursor.downField("arrowThickness").as[Double] match { case Right(x) => conf.arrowThickness = x}
    cursor.downField("arrowTipSize").as[Double] match { case Right(x) => conf.arrowTipSize = x}
    
    val arrowStyleStr = cursor.downField("arrowStyle").as[String] match { case Right(x) => x}
    conf.arrowStyle = arrowStyleStr match {
      case "StyleNone"      => ArrowStyle.StyleNone
      case "StyleFull"      => ArrowStyle.StyleFull
      case "StyleEmpty"     => ArrowStyle.StyleEmpty
      case "StyleReentrant" => ArrowStyle.StyleReentrant
      case "StyleMinimal"   => ArrowStyle.StyleMinimal
    }

    val outputTypeStr = cursor.downField("outputType").as[String] match { case Right(x) => x}
    conf.outputType = outputTypeStr match {
      case "PDFTypeOutput" => OutputType.PDFTypeOutput
      case "EPSTypeOutput" => OutputType.EPSTypeOutput
      case "SVGTypeOutput" => OutputType.SVGTypeOutput
    }

    cursor.downField("removeDefaultInputArg").as[Boolean] match { case Right(x) => conf.removeDefaultInputArg = x}
    
    conf
  }
}
