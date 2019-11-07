package spinal.creative

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import java.awt.{Font, Color, BasicStroke}
import java.awt.geom._
import java.io._
import de.erichseifert.vectorgraphics2d._
import de.erichseifert.vectorgraphics2d.util._

object MainTest {

	def main(args: Array[String]): Unit = {
		val vg2d = new VectorGraphics2D()
        vg2d.setColor(Color.red)
		vg2d.fillRect(10, 10, 10, 10)
        vg2d.setColor(Color.black)
		vg2d.setStroke(new BasicStroke(2.0f))
		vg2d.drawRect(10, 10, 10, 10)
        vg2d.setColor(Color.green)
        val he = new Font("TimesNewRoman", Font.PLAIN, 2)
        vg2d.setFont(he)
        vg2d.translate(10,50)
        vg2d.drawString("Lollone", 20, 20)
		vg2d.fillRect(10, 10, 10, 10)
		vg2d.setStroke(new BasicStroke(2.0f))
		val commands = vg2d.getCommands()
		val pdfProcessor = Processors.get("pdf")

		val doc = pdfProcessor.getDocument(commands, new PageSize(0,0,100,100))
		doc.writeTo(new FileOutputStream("rect.pdf"))
	}
}

class SumReg extends Component {
  val axiConfig = Axi4Config(
    addressWidth = 32,
    dataWidth    = 32,
    idWidth      = 4)

  val io = new Bundle {
    val a = in(SInt(4 bits))
    val b = in(SInt(4 bits))
    val c = inout(SInt(4 bits))
    val d = out(SInt(4 bits))
  }

  val reg = Reg(SInt(4 bits))
  
  reg := io.a + io.b
  io.c := reg
  io.d := reg
}


object ComponentTest {
	def main(args: Array[String]): Unit = {
      SpinalGraphics.fromComponent(new SumReg(), "test.pdf")
      //val report = SpinalVhdl(new SumReg())
	}
}

