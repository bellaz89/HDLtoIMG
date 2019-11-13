package hdltosvg.vhdl
import hdltosvg._

object fromVhdl {

  def removeComments(rawVhdlSource : String) : String = {
    rawVhdlSource.replaceAll("--.*","")
  }

  def getPorts(vhdlSource : String, keyword : String) : Array[(String,String)] = {
    val re = ("""(?i)(?s)^\s*""" + 
              keyword + 
              """\s+(\w*)\s+is.*?port\s*\((.*?)\);\s*end(?:\s|;)""").r
  
    re.findAllIn(vhdlSource).matchData.map(m => (m.group(1), m.group(2))).toArray
  }

  def getComponents(vhdlSource : String) = getPorts(vhdlSource, "component")

  def getEntities(vhdlSource : String) = getPorts(vhdlSource, "entity") 

  def getAll(vhdlSource : String) = getComponents(vhdlSource) ++ getEntities(vhdlSource)

  def getComponentInfo(rawComponent : (String,String)) : ComponentInfo = {
    
    val (componentName, body) = rawComponent
    val bodyMerged = body.replaceAll("[\n\r]","")
    val elements = bodyMerged.split(";").map(_.trim).toArray
    val inRe = """(?i)(\w*)^\s*:\s*in\s*(*.)$""".r
    val outRe = """(?i)(\w*)^\s*:\s*out\s*(*.)$""".r
    val inoutRe = """(?i)(\w*)^\s*:\s*inout\s*(*.)$""".r
    val groupsLeft = elements.map(_ match {
      case inRe(name, desc) => SignalInfo(name, desc, ArrowDirection.PointRight, null)
      case outRe(name, desc) => SignalInfo(name, desc, ArrowDirection.PointLeft, null)
      case inoutRe(name, desc) => SignalInfo(name, desc, ArrowDirection.PointBoth, null)
    })

    ComponentInfo(componentName, Array(groupsLeft), Array())
  }
}
