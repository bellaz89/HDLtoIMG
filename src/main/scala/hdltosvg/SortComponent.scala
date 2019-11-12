package hdltosvg

import scala.util.matching.Regex
import scala.collection.mutable.{ArrayBuffer, LinkedHashMap}

sealed abstract trait GroupPolicy
object GroupPolicy {

  val defaultGroupRegex = """^(?:io_)?([^_]*).*""".r

  object SimplePolicy extends GroupPolicy
  case class RegexGroupsPolicy(groupRegex: Regex = defaultGroupRegex) extends GroupPolicy
}

object sortComponent {
  def apply(componentInfo: ComponentInfo, config: OutputConfig) : ComponentInfo = {

    val signals : Array[SignalInfo] = componentInfo.groupsLeft.flatten

    config.groupPolicy match {
      case GroupPolicy.SimplePolicy => {

        var inSigs    : Array[SignalInfo] = signals.filter(_.dir == ArrowDirection.PointRight)
        var inOutSigs : Array[SignalInfo] = signals.filter(_.dir == ArrowDirection.PointBoth)
        var outSigs   : Array[SignalInfo] = signals.filter(_.dir == ArrowDirection.PointLeft)

        new ComponentInfo(componentInfo.componentName, 
          Array(inSigs, inOutSigs).filter(_.length > 0), 
          reverseGroupsArrowDirection(Array(outSigs).filter(_.length > 0)))
      }

      case GroupPolicy.RegexGroupsPolicy(regex) => {
        val groupsRaw : LinkedHashMap[String, ArrayBuffer[SignalInfo]] = LinkedHashMap()
        for (sig <- signals) {
          sig.name match { 
            case regex(groupName) => {
              if (groupsRaw contains groupName) {
                groupsRaw(groupName) += sig
              } else {
                groupsRaw += (groupName -> ArrayBuffer(sig))
              }
            }
          }
        }


        val singletonGroup = groupsRaw.toArray
          .unzip
          ._2
          .filter( _.length < 2)
          .map(_.toArray)
          .flatten

          val fatGroups = groupsRaw.toArray
            .unzip
            ._2
            .filter( _.length >= 2)
            .map(_.toArray)

            val groups = if (singletonGroup.length > 0) singletonGroup +: fatGroups else fatGroups 

            val splitAtValue = groups.length - groups.length/2

            val (leftGroups, rightGroups) = groups.splitAt(splitAtValue)

            new ComponentInfo(componentInfo.componentName, leftGroups, reverseGroupsArrowDirection(rightGroups))
      }
    }

  }

  def reverseGroupsArrowDirection(groups: Array[Array[SignalInfo]]) : Array[Array[SignalInfo]] = {
    groups.map(group => group.map(_.withFlippedArrow))
  }
}
