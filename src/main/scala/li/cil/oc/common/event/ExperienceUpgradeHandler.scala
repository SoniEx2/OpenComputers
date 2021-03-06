package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api.event._
import li.cil.oc.api.internal.Robot
import li.cil.oc.server.component
import org.lwjgl.opengl.GL11

object ExperienceUpgradeHandler {
  @SubscribeEvent
  def onRobotAnalyze(e: RobotAnalyzeEvent) {
    val (level, experience) = getLevelAndExperience(e.robot)
    // This is basically a 'does it have an experience upgrade' check.
    if (experience != 0.0) {
      e.player.addChatMessage(Localization.Analyzer.RobotXp(experience, level))
    }
  }

  @SubscribeEvent
  def onRobotComputeDamageRate(e: RobotUsedToolEvent.ComputeDamageRate) {
    e.setDamageRate(e.getDamageRate * math.max(0, 1 - getLevel(e.robot) * Settings.get.toolEfficiencyPerLevel))
  }

  @SubscribeEvent
  def onRobotBreakBlockPre(e: RobotBreakBlockEvent.Pre) {
    val boost = math.max(0, 1 - getLevel(e.robot) * Settings.get.harvestSpeedBoostPerLevel)
    e.setBreakTime(e.getBreakTime * boost)
  }

  @SubscribeEvent
  def onRobotAttackEntityPost(e: RobotAttackEntityEvent.Post) {
    if (e.robot.getComponentInSlot(e.robot.selectedSlot()) != null && e.target.isDead) {
      addExperience(e.robot, Settings.get.robotActionXp)
    }
  }

  @SubscribeEvent
  def onRobotBreakBlockPost(e: RobotBreakBlockEvent.Post) {
    addExperience(e.robot, e.experience * Settings.get.robotOreXpRate + Settings.get.robotActionXp)
  }

  @SubscribeEvent
  def onRobotPlaceBlockPost(e: RobotPlaceBlockEvent.Post) {
    addExperience(e.robot, Settings.get.robotActionXp)
  }

  @SubscribeEvent
  def onRobotMovePost(e: RobotMoveEvent.Post) {
    addExperience(e.robot, Settings.get.robotExhaustionXpRate * 0.01)
  }

  @SubscribeEvent
  def onRobotExhaustion(e: RobotExhaustionEvent) {
    addExperience(e.robot, Settings.get.robotExhaustionXpRate * e.exhaustion)
  }

  @SubscribeEvent
  def onRobotRender(e: RobotRenderEvent) {
    val level = if (e.robot != null) getLevel(e.robot) else 0
    if (level > 19) {
      GL11.glColor3f(0.4f, 1, 1)
    }
    else if (level > 9) {
      GL11.glColor3f(1, 1, 0.4f)
    }
    else {
      GL11.glColor3f(0.5f, 0.5f, 0.5f)
    }
  }

  private def getLevel(robot: Robot) = {
    var level = 0
    for (index <- 0 until robot.getSizeInventory) {
      robot.getComponentInSlot(index) match {
        case upgrade: component.UpgradeExperience =>
          level += upgrade.level
        case _ =>
      }
    }
    level
  }

  private def getLevelAndExperience(robot: Robot) = {
    var level = 0
    var experience = 0.0
    for (index <- 0 until robot.getSizeInventory) {
      robot.getComponentInSlot(index) match {
        case upgrade: component.UpgradeExperience =>
          level += upgrade.level
          experience += upgrade.experience
        case _ =>
      }
    }
    (level, experience)
  }

  private def addExperience(robot: Robot, amount: Double) {
    for (index <- 0 until robot.getSizeInventory) {
      robot.getComponentInSlot(index) match {
        case upgrade: component.UpgradeExperience =>
          upgrade.addExperience(amount)
        case _ =>
      }
    }
  }
}
