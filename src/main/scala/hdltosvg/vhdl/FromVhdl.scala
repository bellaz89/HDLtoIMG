package hdltosvg.vhdl
import scala.io.Source
import java.io.{File, FileOutputStream}
import scopt.OParser
import hdltosvg._


object fromVhdl {

  case class ProgramOptions(
    file: File = new File("."),
    outputOpt: OutputConfig = obtainConfig.default())

  val builder = OParser.builder[ProgramOptions]
  val parser= {
    import builder._
    OParser.sequence(
      programName("from-vhdl"),
      head("from-vhdl","1.0"),
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
        val components = getAll(source).map(getComponentInfo(_))
        components.foreach { 
          cmp => ComponentSVG.fromUnsorted(cmp,
            new FileOutputStream(cmp.componentName ++ ".svg"),
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
    val re = (keyword + """\s+(\w*)\s+is.*?port\s*\((.*?)\);\s*end(?:\s|;)""").r

    re.findAllIn(vhdlSource).matchData.map(m => (m.group(1), m.group(2))).toArray
  }

  def getComponents(vhdlSource : String) = getPorts(vhdlSource, "component")

  def getEntities(vhdlSource : String) = getPorts(vhdlSource, "entity") 

  def getAll(vhdlSource : String) = getComponents(vhdlSource) ++ getEntities(vhdlSource)

  def getComponentInfo(rawComponent : (String,String)) : ComponentInfo = {

    val (componentName, body) = rawComponent
    val arrowStyle = ArrowStyle.StyleReentrant
    val bodyMerged = body.replaceAll("[\n\r]","")
    val elements = bodyMerged.split(";").map(_.trim).toArray
    val inRe = """(\w*)\s*:\s*in\s*(.*)""".r
    val outRe = """(\w*)\s*:\s*out\s*(.*)""".r
    val inoutRe = """(\w*)\s*:\s*inout\s*(.*)""".r
    val groupsLeft = elements.map(_ match {
      case inRe(name, desc) => SignalInfo(name, desc, ArrowDirection.PointRight, arrowStyle)
      case outRe(name, desc) => SignalInfo(name, desc, ArrowDirection.PointLeft, arrowStyle)
      case inoutRe(name, desc) => SignalInfo(name, desc, ArrowDirection.PointBoth, arrowStyle)
    })
  ComponentInfo(componentName, Array(groupsLeft), Array())
  }
}
