package com.github.hdltoimg.spinal

import java.io._
import scala.collection.mutable.ArrayBuffer

import spinal.core._
import spinal.core.internals._

import com.github.hdltoimg.{ComponentSVG, SignalInfo, OutputConfig, ArrowDirection, ComponentInfo}

object fromComponent {
  def apply[T <: Component](component: => T, filename: String) { fromComponent(component, filename, OutputConfig.default()) }
  def apply[T <: Component](component: => T, filename: String, config: OutputConfig){
    fromComponent(component, new FileOutputStream(filename), config)
  }

  def apply[T <: Component](component: => T, out: OutputStream){ fromComponent(component, out, OutputConfig.default()) }
  def apply[T <: Component](component: => T, out: OutputStream, config: OutputConfig){
    fromReport(SpinalVhdl(component), out, config)
  }
}

object fromReport {
  def apply[T <: Component](report: => SpinalReport[T], filename: String) { fromReport(report, filename, OutputConfig.default()) }
  def apply[T <: Component](report: => SpinalReport[T], filename: String, config: OutputConfig){
    fromReport(report, new FileOutputStream(filename), config)
  }

  def apply[T <: Component](report: => SpinalReport[T], out: OutputStream){ fromReport(report, out, OutputConfig.default()) }
  def apply[T <: Component](report: => SpinalReport[T], out: OutputStream, config: OutputConfig){

    val componentInfo: ComponentInfo = getComponentInfoFromReport(report, config)
    ComponentSVG.fromUnsorted(componentInfo, out, config)
  }

  def getComponentInfoFromReport[T <: Component](report: => SpinalReport[T], config: OutputConfig) : ComponentInfo = {

    var signals : ArrayBuffer[SignalInfo] = ArrayBuffer()
    val regexIn    = """.* : in (.*)\)""".r
    val regexOut   = """.* : out (.*)\)""".r
    val regexInOut = """.* : inout (.*)\)""".r

    for (sig <- report.toplevel.getAllIo) {
      sig.toString match {
        case regexIn(identifier) => signals += new SignalInfo(sig.getName, identifier, ArrowDirection.PointRight, config.arrowStyle)
        case regexOut(identifier) => signals += new SignalInfo(sig.getName, identifier, ArrowDirection.PointLeft, config.arrowStyle)
        case regexInOut(identifier) => signals += new SignalInfo(sig.getName, identifier, ArrowDirection.PointBoth, config.arrowStyle)
        case _ => {
          println("Unknown " ++ sig.toString)
          signals += new SignalInfo(sig.getName, sig.toString , ArrowDirection.PointBoth, config.arrowStyle)
        }
      }
    }
    new ComponentInfo(report.toplevelName, 
      Array(signals.toArray), Array())
  }
}


package test {
  import spinal.lib.bus.amba4.axi._

  class SumReg extends Component {
    val axiConfig = Axi4Config(
      addressWidth = 32,
      dataWidth    = 32,
      idWidth      = 4)

    val io = new Bundle {
      val a = in(SInt(4 bits))
      val b = in(SInt(4 bits))
      val c = inout(SInt(4 bits))
      val d = out(SInt(32 bits))
      val f = out(Bool)
    }

    val reg = Reg(SInt(4 bits))

    reg := io.a + io.b
    io.c := reg
    io.d := reg.resized
    io.f := False
  }


  object ComponentTest {
    def main(args: Array[String]): Unit = {
      fromComponent(new SumReg(), "test.svg")
    }
  }
}
