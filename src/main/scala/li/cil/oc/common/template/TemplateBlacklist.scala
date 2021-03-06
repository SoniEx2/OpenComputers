package li.cil.oc.common.template

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsScala._

object TemplateBlacklist {
  private lazy val TheBlacklist = { // scnr
    val pattern = """^([^@]+)(?:@(\d+))?$""".r
    def parseDescriptor(id: String, meta: Int) = {
      val item = Item.itemRegistry.getObject(id).asInstanceOf[Item]
      if (item == null) {
        OpenComputers.log.warn(s"Bad assembler blacklist entry '$id', unknown item id.")
        None
      }
      else {
        Option(new ItemStack(item, 1, meta))
      }
    }
    Settings.get.assemblerBlacklist.map {
      case pattern(id, null) => parseDescriptor(id, 0)
      case pattern(id, meta) => try parseDescriptor(id, meta.toInt) catch {
        case _: NumberFormatException =>
          OpenComputers.log.warn(s"Bad assembler blacklist entry '$id@$meta', invalid damage value.")
          None
      }
      case badFormat =>
        OpenComputers.log.warn(s"Bad assembler blacklist entry '$badFormat', invalid format (should be 'id' or 'id@damage').")
        None
    }.collect {
      case Some(stack) => stack
    }.toArray
  }

  def register(): Unit = {
    FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerFilter", "li.cil.oc.common.template.TemplateBlacklist.filter")
  }

  def filter(stack: ItemStack): Boolean = {
    !TheBlacklist.exists(_.isItemEqual(stack))
  }
}
