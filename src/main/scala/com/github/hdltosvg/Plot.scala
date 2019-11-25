package com.github.hdltosvg

import de.erichseifert.vectorgraphics2d._
import de.erichseifert.vectorgraphics2d.util._ 

import java.awt.{Color, BasicStroke}
import java.awt.geom._

object drawComponent {

  def apply(vg2d : VectorGraphics2D, config: OutputConfig, compPositions: ComponentFigurePositions) {
    drawBox(vg2d, config, compPositions)
    drawComponentName(vg2d, config, compPositions)
    drawLabels(vg2d, config, compPositions)
    drawArrows(vg2d, config, compPositions)
  }

  def drawBox(vg2d : VectorGraphics2D, config: OutputConfig, compPositions: ComponentFigurePositions) {

    vg2d.translate(compPositions.boxPos._1.toInt, compPositions.boxPos._2.toInt)
    vg2d.setColor(config.boxFillColor)
    vg2d.fillRect(0, 0, compPositions.boxSize._1.toInt, compPositions.boxSize._2.toInt)
    vg2d.setColor(Color.black)
    vg2d.setStroke(new BasicStroke(config.boxTickness.toFloat))
    vg2d.drawRect(0, 0, compPositions.boxSize._1.toInt, compPositions.boxSize._2.toInt)
  }

  def drawComponentName(vg2d : VectorGraphics2D, config: OutputConfig, compPositions: ComponentFigurePositions) {
    vg2d.setFont(config.compFont)
    val (x, y) = compPositions.componentPos
    vg2d.drawString(compPositions.componentName, x.toInt, y.toInt)
  }
  def drawLabels(vg2d : VectorGraphics2D, config: OutputConfig, compPositions: ComponentFigurePositions) {
    vg2d.setFont(config.sigFont)
    for ((str, x, y) <- compPositions.labelPositions) vg2d.drawString(str, x.toInt, y.toInt)
  }
  def drawArrows(vg2d : VectorGraphics2D, config: OutputConfig, compPositions: ComponentFigurePositions) {

    vg2d.setStroke(new BasicStroke(config.arrowThickness.toFloat))
    for ((dir, sty, x, y) <- compPositions.arrowPositions) {  
      sty match {
        case ArrowStyle.StyleNone      => vg2d.drawLine(x.toInt, y.toInt,  (x+config.arrowLength).toInt, y.toInt)
        case ArrowStyle.StyleFull      => drawArrowFull(vg2d, config, dir, x, y)
        case ArrowStyle.StyleEmpty     => drawArrowEmpty(vg2d, config, dir, x, y)
        case ArrowStyle.StyleReentrant => drawArrowReentrant(vg2d, config, dir, x, y)
        case ArrowStyle.StyleMinimal   => drawArrowMinimal(vg2d, config, dir, x, y)
      }

    }
  }

  def drawArrowFull(vg2d : VectorGraphics2D, config: OutputConfig, dir: ArrowDirection, x: Double, y: Double) {
    val xend = (x+config.arrowLength) 
    val xVertexL = Array(x, x + 2*config.arrowTipSize, x + 2*config.arrowTipSize).map(_.toInt)
    val yVertexL = Array(y, y + config.arrowTipSize, y - config.arrowTipSize).map(_.toInt)
    val xVertexR = Array(xend, xend - 2*config.arrowTipSize, xend - 2*config.arrowTipSize).map(_.toInt)
    val yVertexR = Array(y, y + config.arrowTipSize, y - config.arrowTipSize).map(_.toInt)

    dir match {
      case ArrowDirection.PointRight => {
        vg2d.drawLine(x.toInt, y.toInt,  (xend - config.arrowTipSize).toInt, y.toInt)
        vg2d.fillPolygon(xVertexR, yVertexR, 3)
      }

      case ArrowDirection.PointLeft => {
        vg2d.drawLine((x + config.arrowTipSize).toInt, y.toInt,  xend.toInt, y.toInt)
        vg2d.fillPolygon(xVertexL, yVertexL, 3)
      }

      case ArrowDirection.PointBoth => {
        vg2d.drawLine((x + config.arrowTipSize).toInt, y.toInt,  (xend.toInt - config.arrowTipSize).toInt, y.toInt)
        vg2d.fillPolygon(xVertexR, yVertexR, 3)
        vg2d.fillPolygon(xVertexL, yVertexL, 3)
      }
    }
  }

  def drawArrowEmpty(vg2d : VectorGraphics2D, config: OutputConfig, dir: ArrowDirection, x: Double, y: Double) {
    val xend = (x+config.arrowLength) 
    val xVertexL = Array(x, x + 2*config.arrowTipSize, x + 2*config.arrowTipSize).map(_.toInt)
    val yVertexL = Array(y, y + config.arrowTipSize, y - config.arrowTipSize).map(_.toInt)
    val xVertexR = Array(xend, xend - 2*config.arrowTipSize, xend - 2*config.arrowTipSize).map(_.toInt)
    val yVertexR = Array(y, y + config.arrowTipSize, y - config.arrowTipSize).map(_.toInt)

    dir match {
      case ArrowDirection.PointRight => {
        vg2d.drawLine(x.toInt, y.toInt,  (xend - config.arrowTipSize).toInt, y.toInt)
        vg2d.setColor(Color.white)
        vg2d.fillPolygon(xVertexR, yVertexR, 3)
        vg2d.setColor(Color.black)
        vg2d.drawPolygon(xVertexR, yVertexR, 3)
      }

      case ArrowDirection.PointLeft => {
        vg2d.drawLine((x + config.arrowTipSize).toInt, y.toInt,  xend.toInt, y.toInt)
        vg2d.setColor(Color.white)
        vg2d.fillPolygon(xVertexL, yVertexL, 3)
        vg2d.setColor(Color.black)
        vg2d.drawPolygon(xVertexL, yVertexL, 3)
      }

      case ArrowDirection.PointBoth => {
        vg2d.drawLine((x + config.arrowTipSize).toInt, y.toInt,  (xend.toInt - config.arrowTipSize).toInt, y.toInt)
        vg2d.setColor(Color.white)
        vg2d.fillPolygon(xVertexR, yVertexR, 3)
        vg2d.fillPolygon(xVertexL, yVertexL, 3)
        vg2d.setColor(Color.black)
        vg2d.drawPolygon(xVertexR, yVertexR, 3)
        vg2d.drawPolygon(xVertexL, yVertexL, 3)
      }
    }
  }

  def drawArrowReentrant(vg2d : VectorGraphics2D, config: OutputConfig, dir: ArrowDirection, x: Double, y: Double) {
    val xend = (x+config.arrowLength) 
    val xVertexL = Array(x, x + 2*config.arrowTipSize, x + config.arrowTipSize, x + 2*config.arrowTipSize).map(_.toInt)
    val yVertexL = Array(y, y + config.arrowTipSize, y, y - config.arrowTipSize).map(_.toInt)
    val xVertexR = Array(xend, xend - 2*config.arrowTipSize, xend - config.arrowTipSize, xend - 2*config.arrowTipSize).map(_.toInt)
    val yVertexR = Array(y, y + config.arrowTipSize, y, y - config.arrowTipSize).map(_.toInt)

    dir match {
      case ArrowDirection.PointRight => {
        vg2d.drawLine(x.toInt, y.toInt,  (xend - config.arrowTipSize).toInt, y.toInt)
        vg2d.fillPolygon(xVertexR, yVertexR, 4)
      }

      case ArrowDirection.PointLeft => {
        vg2d.drawLine((x + config.arrowTipSize).toInt, y.toInt,  xend.toInt, y.toInt)
        vg2d.fillPolygon(xVertexL, yVertexL, 4)
      }

      case ArrowDirection.PointBoth => {
        vg2d.drawLine((x + config.arrowTipSize).toInt, y.toInt,  (xend.toInt - config.arrowTipSize).toInt, y.toInt)
        vg2d.fillPolygon(xVertexR, yVertexR, 4)
        vg2d.fillPolygon(xVertexL, yVertexL, 4)
      }
    }
  }

  def drawArrowMinimal(vg2d : VectorGraphics2D, config: OutputConfig, dir: ArrowDirection, x: Double, y: Double) {
    val xend = (x+config.arrowLength) 
    val xVertexL = Array(x, x + 2*config.arrowTipSize, x + 2*config.arrowTipSize).map(_.toInt)
    val yVertexL = Array(y, y + config.arrowTipSize, y - config.arrowTipSize).map(_.toInt)
    val xVertexR = Array(xend, xend - 2*config.arrowTipSize, xend - 2*config.arrowTipSize).map(_.toInt)
    val yVertexR = Array(y, y + config.arrowTipSize, y - config.arrowTipSize).map(_.toInt)

    dir match {
      case ArrowDirection.PointRight => {
        vg2d.drawLine(x.toInt, y.toInt,  (xend - config.arrowThickness/2).toInt, y.toInt)
        vg2d.drawLine(xVertexR(0), yVertexR(0), xVertexR(1), yVertexR(1))
        vg2d.drawLine(xVertexR(0), yVertexR(0), xVertexR(2), yVertexR(2))
      }

      case ArrowDirection.PointLeft => {
        vg2d.drawLine((x + config.arrowThickness).toInt, y.toInt,  xend.toInt, y.toInt)
        vg2d.drawLine(xVertexL(0), yVertexL(0), xVertexL(1), yVertexL(1))
        vg2d.drawLine(xVertexL(0), yVertexL(0), xVertexL(2), yVertexL(2))
      }

      case ArrowDirection.PointBoth => {
        vg2d.drawLine((x + config.arrowThickness/2).toInt, y.toInt,  (xend.toInt - config.arrowThickness/2).toInt, y.toInt)
        vg2d.drawLine(xVertexR(0), yVertexR(0), xVertexR(1), yVertexR(1))
        vg2d.drawLine(xVertexR(0), yVertexR(0), xVertexR(2), yVertexR(2))
        vg2d.drawLine(xVertexL(0), yVertexL(0), xVertexL(1), yVertexL(1))
        vg2d.drawLine(xVertexL(0), yVertexL(0), xVertexL(2), yVertexL(2))
      }
    }
  }


}
