package com.github.hdltoimg.vhdl

import scala.collection.mutable.{ArrayBuffer, StringBuilder}
import scala.io.Source
import java.io.{File, FileOutputStream}
import scopt.OParser
import com.github.hdltoimg._


object VhdlToImg {

  case class ProgramOptions(
    file: File = new File("."),
    outputOpt: OutputConfig = obtainConfig.default())

  val builder = OParser.builder[ProgramOptions]
  val parser= {
    import builder._
    OParser.sequence(
      programName("hdltoimg"),
      head("hdltoimg","1.0"),
      opt[String]('i', "input")
        .required()
        .action((x, c) => c.copy(file = new File(x)))
        .text("The input VHDL file"),
      opt[File]('c', "config")
        .optional()
        .action((x, c) => c.copy(outputOpt = obtainConfig(x))))
        .text("The output config file")
  }

  def main(args: Array[String]): Unit = {

    OParser.parse(parser, args, ProgramOptions()) match {
      case Some(config) => {
        val source = Source.fromFile(config.file)
                           .getLines
                           .map(removeComments(_))
                           .mkString
        val components = getAll(source).map(getComponentInfo(_, config.outputOpt))

        val extension = config.outputOpt.outputType match {
          case OutputType.PDFTypeOutput => ".pdf"
          case OutputType.EPSTypeOutput => ".eps"
          case OutputType.SVGTypeOutput => ".svg"
        }
        
        components.foreach { 
          cmp => ComponentSVG.fromUnsorted(cmp,
            new FileOutputStream(cmp.componentName + extension),
            config.outputOpt) 
        }
      }
      case _ => 
    }

  }

  def removeComments(rawVhdlSource : String) : String = {
    rawVhdlSource.replaceAll("--.*","")
  }

  def getPorts(vhdlSource : String, keyword : String) : Array[(String,String)] = {
    val re = ("""(?i)""" + 
              keyword + 
              """\s+(\w*)\s+is.*?port\s*\((.*?)\)\s*;\s*?end[\s|;|$]""").r

    re.findAllIn(vhdlSource).matchData.map(m => (m.group(1), m.group(2))).toArray
  }

  def getComponents(vhdlSource : String) = getPorts(vhdlSource, "component")

  def getEntities(vhdlSource : String) = getPorts(vhdlSource, "entity") 

  def getAll(vhdlSource : String) = getComponents(vhdlSource) ++ getEntities(vhdlSource)

  def getComponentInfo(rawComponent: (String,String), conf: OutputConfig) : ComponentInfo = {

    val (componentName, body) = rawComponent
    val arrowStyle = conf.arrowStyle
    val bodyMerged = body.replaceAll("[\n\r]","")
    val elements = bodyMerged.split(";").map(_.trim).toArray
    
    val (inRe, outRe, inoutRe) = if (conf.removeDefaultInputArg) {
      ("""(?i)(\w*)\s*:\s*in\s+(.*?(?=(?::=|$)))""".r,
      """(?i)(\w*)\s*:\s*out\s+(.*?(?=(?::=|$)))""".r,
      """(?i)(\w*)\s*:\s*inout\s+(.*?(?=(?::=|$)))""".r)
    } else {
     ("""(?i)(\w*)\s*:\s*in\s+(.*)""".r,
      """(?i)(\w*)\s*:\s*out\s+(.*)""".r,
      """(?i)(\w*)\s*:\s*inout\s+(.*)""".r)
    }
    
    val groupsLeft = if (conf.standardFormatting) {
      elements.map(_ match {
        case inRe(name, desc) => SignalInfo(name, standardizeDesc(desc), ArrowDirection.PointRight, arrowStyle)
        case outRe(name, desc) => SignalInfo(name, standardizeDesc(desc), ArrowDirection.PointLeft, arrowStyle)
        case inoutRe(name, desc) => SignalInfo(name, standardizeDesc(desc), ArrowDirection.PointBoth, arrowStyle)
      })
    } else {
      elements.map(_ match {
        case inRe(name, desc) => SignalInfo(name, desc, ArrowDirection.PointRight, arrowStyle)
        case outRe(name, desc) => SignalInfo(name, desc, ArrowDirection.PointLeft, arrowStyle)
        case inoutRe(name, desc) => SignalInfo(name, desc, ArrowDirection.PointBoth, arrowStyle)
      })
    }
  ComponentInfo(componentName, Array(groupsLeft), Array())
  }

  def standardizeDesc(desc: String): String = {
    val matchMultiwire = """(?i)([^\(\)]+)\((.*?(?: to | downto).+)""".r 
    desc.trim match {
      case matchMultiwire(outerDesc, innerDesc) => {
        val stringElems = new ArrayBuffer[String]()
        var elemAccum = new StringBuilder()
        var parenDepth = 0

        for(c <- innerDesc) {

          if(c == ')') {
            parenDepth -= 1
            if(parenDepth == -1) {
              stringElems.append(elemAccum.toString)
              elemAccum = new StringBuilder()
            }
          }

          if(parenDepth >= 0) elemAccum += c
          
          if(c == ',' & parenDepth == 0){
            stringElems.append(elemAccum.toString)
            elemAccum = new StringBuilder()
          }

          if(c == '(') {
            parenDepth += 1
          }
        }
        stringElems.append(elemAccum.toString)

        outerDesc + '['+ stringElems.map(x => standardizeRange(x)).mkString + ']'
      }

      case _ => desc
    }
  }

  def standardizeRange(ran: String): String = {
    
    val downtoMatch = "(?i)(.*?) downto (.*)".r
    val toMatch = "(?i)(.*?) to (.*)".r

    ran.trim match {
      case downtoMatch(m1, m2) => (m1.trim + ':' + m2.trim).trim 
      case toMatch(m1, m2) => (m1.trim + ':' + m2.trim).trim
      case str => str.trim
    }
  }
}

