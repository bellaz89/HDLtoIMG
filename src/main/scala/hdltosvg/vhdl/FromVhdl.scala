package hdltosvg.vhdl
import java.io.FileOutputStream
import hdltosvg._
import scala.io.Source

object fromVhdl {

  def main(args: Array[String]): Unit = {
    val source = Source.fromFile(args(0)).getLines.map(removeComments(_)).mkString
    val components = getAll(source).map(getComponentInfo(_))
    components.foreach { 
      cmp => ComponentSVG.fromUnsorted(cmp, 
        new FileOutputStream(cmp.componentName ++ ".svg"),
        OutputConfig.default()) 
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
