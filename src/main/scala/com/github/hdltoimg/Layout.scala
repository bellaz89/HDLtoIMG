package com.github.hdltoimg

import math._

import java.awt.FontMetrics
import scala.collection.mutable.ArrayBuffer

import de.erichseifert.vectorgraphics2d._
import de.erichseifert.vectorgraphics2d.util._ 

object layout {
  def apply(vg2d : VectorGraphics2D, config: OutputConfig, compInfo: ComponentInfo) : ComponentFigurePositions = {

    val labelFontMetrics = vg2d.getFontMetrics(config.sigFont)

    val groupsLeft = compInfo.groupsLeft
    val groupsRight = compInfo.groupsRight

    val boxSize = getBoxSize(config, compInfo, labelFontMetrics)
    val boxPos = getBoxPosition(vg2d, config, compInfo, boxSize)
    val figSize = getFigSize(vg2d, config, compInfo, boxSize)
    val compNamePosition = getComponentNamePosition(vg2d, config, compInfo, boxSize)
    val sigsXLeft = getSigsXLeft(config, groupsLeft, labelFontMetrics, boxSize)
    val sigsXRight= getSigsXRight(config, groupsRight, labelFontMetrics, boxSize)
    val descXLeft = getDescXLeft(config, groupsLeft, labelFontMetrics, boxSize)
    val descXRight= getDescXRight(config, groupsRight, labelFontMetrics, boxSize)
    val groupsYsLeft  = getGroupsYs(config, groupsLeft, labelFontMetrics)
    val groupsYsRight = getGroupsYs(config, groupsRight, labelFontMetrics)
    val arrowYLeft   = getArrowYs(groupsYsLeft, labelFontMetrics)
    val arrowYRight  = getArrowYs(groupsYsRight, labelFontMetrics)
    val arrowXLeft    = getArrowXLeft(config, groupsLeft, boxSize)
    val arrowXRight   = getArrowXRight(config, groupsRight, boxSize)
    val (namesLeft, descsLeft, arrowsLeft) = extractNamesDescsArrowsStylesFromGroups(groupsLeft)
    val (namesRight, descsRight, arrowsRight) = extractNamesDescsArrowsStylesFromGroups(groupsRight)

    val labels = namesLeft ++ namesRight ++ descsLeft ++ descsRight
    val labelPosX = sigsXLeft ++ sigsXRight ++ descXLeft ++ descXRight 
    val labelPosY = groupsYsLeft ++ groupsYsRight ++ groupsYsLeft ++ groupsYsRight

    val arrows  = arrowsLeft ++ arrowsRight
    val arrowsX = arrowXLeft ++ arrowXRight
    val arrowsY = arrowYLeft ++ arrowYRight

    val arrowPos =  arrows.zip(arrowsX zip arrowsY).map({ case ((dir, sty), (x, y)) => (dir, sty, x, y) })
    val labelsPos = labels.zip(labelPosX zip labelPosY).map({ case (lab, (x, y)) => (lab, x, y) })

    new ComponentFigurePositions(figSize=figSize, 
      boxPos=boxPos, 
      boxSize=boxSize, 
      componentName=compInfo.componentName, 
      componentPos=compNamePosition, 
      arrowPositions=arrowPos, 
      labelsPos)
  }


  def getGroupsHeight(config: OutputConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics) : Double = {
    val chHeight = fontMetrics.getHeight
    val groupsPadHeight = if (groups.length > 1) (groups.length - 1) * config.groupPadY else 0
    val labelsHeight = groups.flatten.length * chHeight
    val labelsPad = if (groups.flatten.length > 1) (groups.flatten.length - 1) * config.sigPadY else 0
    groupsPadHeight + labelsHeight + labelsPad
  }

  def getGroupsWidth(config: OutputConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics) : Double = {
    groups.flatten
      .map(x => fontMetrics.stringWidth(x.name))
      .foldLeft(0)((y, z) => y max z)
  }

  def getBoxSize(config: OutputConfig, compInfo: ComponentInfo, fontMetrics: FontMetrics) : (Double, Double) = {

    val leftGroupsHeight = getGroupsHeight(config, compInfo.groupsLeft, fontMetrics)
    val rightGroupsHeight = getGroupsHeight(config, compInfo.groupsRight, fontMetrics)
    val boxHeight = 2*config.boxBorderPad + max(leftGroupsHeight, rightGroupsHeight)

    val leftGroupsWidth = getGroupsWidth(config, compInfo.groupsLeft, fontMetrics)
    val rightGroupsWidth = getGroupsWidth(config, compInfo.groupsRight, fontMetrics)

    val boxWidth = config.boxWidthPolicy match {
      case BoxConstantWidth(width) => width
      case BoxSigNamePad(pad) => leftGroupsWidth + rightGroupsWidth + pad + 2*config.sigPadX
      case BoxConstantRatio(ratio) => boxHeight * ratio
    }

    (boxWidth, boxHeight)
  }

  def getGroupsDescWidth(config: OutputConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics) : Double = {
    groups.flatten
      .map(x => fontMetrics.stringWidth(x.desc))
      .foldLeft(0)((y, z) => y max z)
  }

  def getFigWidth(vg2d: VectorGraphics2D, config: OutputConfig, compInfo: ComponentInfo, boxSize: (Double, Double)) : Double = {
    val labelFontMetrics = vg2d.getFontMetrics(config.sigFont)
    val leftGroupDescWidth = getGroupsDescWidth(config, compInfo.groupsLeft, labelFontMetrics)
    val rightGroupDescWidth = getGroupsDescWidth(config, compInfo.groupsRight, labelFontMetrics)
    boxSize._1 + leftGroupDescWidth + rightGroupDescWidth + ((config.arrowPadX + config.arrowLength + config.descPadX + config.figPadX)*2)
  }

  def getFigHeight(vg2d: VectorGraphics2D, config: OutputConfig, compInfo: ComponentInfo, boxSize: (Double, Double)) : Double = {
    val componentLabelHeight = vg2d.getFontMetrics(config.compFont).getHeight
    componentLabelHeight + boxSize._2 + config.compPadY + (config.figPadY*2)
  }

  def getFigSize(vg2d: VectorGraphics2D, config: OutputConfig, compInfo: ComponentInfo, boxSize: (Double, Double)) : (Double, Double) = {
    (getFigWidth(vg2d, config, compInfo, boxSize), getFigHeight(vg2d, config, compInfo, boxSize))
  }

  def getBoxPosition(vg2d: VectorGraphics2D, config: OutputConfig, compInfo: ComponentInfo, boxSize: (Double, Double)) : (Double, Double) = {
    val labelFontMetrics = vg2d.getFontMetrics(config.sigFont)
    val leftGroupDescWidth = getGroupsDescWidth(config, compInfo.groupsLeft, labelFontMetrics)
    val componentLabelHeight = vg2d.getFontMetrics(config.compFont).getHeight

    val boxPosX = leftGroupDescWidth + config.arrowPadX + config.descPadX + config.figPadX + config.arrowLength
    val boxPosY = (if (config.compAlignment._1 == VAlignment.Top) componentLabelHeight + config.compPadY else 0) + config.figPadY
    (boxPosX, boxPosY)
  }

  def getComponentNamePosition(vg2d: VectorGraphics2D, config: OutputConfig, compInfo: ComponentInfo, boxSize: (Double, Double)) : (Double, Double) = {
    val componentMetrics = vg2d.getFontMetrics(config.compFont)
    val componentAscent = componentMetrics.getAscent
    val componentDescent = componentMetrics.getDescent
    val componentWidth = componentMetrics.stringWidth(compInfo.componentName)
    val componentPosX = config.compAlignment._2 match {
      case HAlignment.Left => config.compPadX
      case HAlignment.Center => (boxSize._1 - componentWidth)/2
      case HAlignment.Right => boxSize._1 - componentWidth - config.compPadX
    }
    val componentPosY = if (config.compAlignment._1 == VAlignment.Top) {
      -config.compPadY - componentDescent
    } else {
      boxSize._2 + config.compPadY + componentAscent
    }

    (componentPosX, componentPosY)
  }

  def extractNamesDescsArrowsStylesFromGroups(groups: Array[Array[SignalInfo]]) : (Array[String], Array[String], Array[(ArrowDirection, ArrowStyle)]) = {
    val flatGroups = groups.flatten
    val names  = flatGroups.map(x => x.name)
    val descs  = flatGroups.map(x => x.desc)
    val arrows = flatGroups.map(x => (x.dir, x.sty))
    (names, descs, arrows)
  }


  def getGroupsYs(config: OutputConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics) : Array[Double] = {
    val labelAscent = fontMetrics.getAscent
    val labelHeight = fontMetrics.getHeight
    var posY = config.boxBorderPad + labelAscent
    val yS : ArrayBuffer[Double] = ArrayBuffer()
    for (group <- groups) {
      for (sigInfo <- group) {
        yS += posY
        posY += labelHeight + config.sigPadY
      }
      posY -= config.sigPadY
      posY += config.groupPadY
    }

    yS.toArray
  }

  def getSigsXLeft(config: OutputConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics, boxSize: (Double, Double)) : Array[Double] = {
    Array.fill(groups.flatten.length)(config.sigPadX)
  }

  def getSigsXRight(config: OutputConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics, boxSize: (Double, Double)) : Array[Double] = {
    groups.flatten.map(x => boxSize._1 - config.sigPadX - fontMetrics.stringWidth(x.name))
  }

  def getDescXLeft(config: OutputConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics, boxSize: (Double, Double)) : Array[Double] = {
    groups.flatten.map(x =>  - config.arrowPadX - config.arrowLength - config.descPadX - fontMetrics.stringWidth(x.desc))
  }

  def getDescXRight(config: OutputConfig, groups: Array[Array[SignalInfo]], fontMetrics: FontMetrics, boxSize: (Double, Double)) : Array[Double] = {
    Array.fill(groups.flatten.length)(config.arrowPadX + config.arrowLength + config.descPadX + boxSize._1)
  }

  def getArrowYs(groupYs: Array[Double], fontMetrics: FontMetrics) : Array[Double] = {
    val labelHeight = fontMetrics.getHeight
    groupYs.map(_-labelHeight/4)
  }

  def getArrowXLeft(config: OutputConfig, groups: Array[Array[SignalInfo]], boxSize: (Double, Double)) : Array[Double] = {
    Array.fill(groups.flatten.length)(- config.arrowPadX - config.arrowLength)
  }

  def getArrowXRight(config: OutputConfig, groups: Array[Array[SignalInfo]], boxSize: (Double, Double)) : Array[Double] = {
    Array.fill(groups.flatten.length)(config.arrowPadX + boxSize._1)
  }
}
