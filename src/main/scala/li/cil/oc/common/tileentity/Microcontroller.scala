package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.driver.item.Memory
import li.cil.oc.api.driver.item.Processor
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Architecture
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

class Microcontroller extends traits.PowerAcceptor with traits.Computer with SidedEnvironment with internal.Microcontroller {
  val info = new ItemUtils.MicrocontrollerData()

  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("microcontroller").
    withConnector().
    create()

  private val snooperNode = api.Network.newNode(this, Visibility.Network).create()

  override protected def runSound = None // Microcontrollers are silent.

  // ----------------------------------------------------------------------- //

  override def sidedNode(side: ForgeDirection) = if (side != facing) node else null

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = side != facing

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = side != facing

  override protected def connector(side: ForgeDirection) = Option(if (side != facing && machine != null) machine.node.asInstanceOf[Connector] else null)

  override protected def energyThroughput = Settings.get.caseRate(Tier.One)

  override def getWorld = world

  // ----------------------------------------------------------------------- //

  override def cpuArchitecture: Class[_ <: Architecture] = info.components.map(stack => (stack, Driver.driverFor(stack, getClass))).collectFirst {
    case (stack, driver: Processor) if driver.slot(stack) == Slot.CPU => driver.architecture(stack)
  }.orNull

  override def callBudget = info.components.foldLeft(0.0)((sum, item) => sum + (Option(item) match {
    case Some(stack) => Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: Processor) if driver.slot(stack) == Slot.CPU => Settings.get.callBudgets(driver.tier(stack))
      case _ => 0
    }
    case _ => 0
  }))

  override def installedMemory = info.components.foldLeft(0)((sum, item) => sum + (Option(item) match {
    case Some(stack) => Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: Memory) => driver.amount(stack)
      case _ => 0
    }
    case _ => 0
  }))

  override def componentSlot(address: String) = components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  def maxComponents = 32

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():boolean -- Starts the microcontroller. Returns true if the state changed.""")
  def start(context: Context, args: Arguments): Array[AnyRef] =
    result(!machine.isPaused && machine.start())

  @Callback(doc = """function():boolean -- Stops the microcontroller. Returns true if the state changed.""")
  def stop(context: Context, args: Arguments): Array[AnyRef] =
    result(machine.stop())

  @Callback(direct = true, doc = """function():boolean -- Returns whether the microcontroller is running.""")
  def isRunning(context: Context, args: Arguments): Array[AnyRef] =
    result(machine.isRunning)

  @Callback(direct = true, doc = """function():string -- Returns the reason the microcontroller crashed, if applicable.""")
  def lastError(context: Context, args: Arguments): Array[AnyRef] =
    result(machine.lastError)

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

  override def updateEntity() {
    super.updateEntity()

    // Pump energy into the internal network.
    if (world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      machine.node match {
        case connector: Connector =>
          val demand = connector.globalBufferSize - connector.globalBuffer
          val available = demand + node.changeBuffer(-demand)
          connector.changeBuffer(available)
        case _ =>
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    if (node == this.node) {
      api.Network.joinNewNetwork(machine.node)
      machine.node.connect(snooperNode)
      machine.setCostPerTick(Settings.get.microcontrollerCost)
    }
    super.onConnect(node)
  }

  override def onMessage(message: Message) {
    super.onMessage(message)
    if (message.name == "network.message") {
      if (message.source.network == snooperNode.network)
        node.sendToReachable(message.name, message.data: _*)
      else
        snooperNode.sendToReachable(message.name, message.data: _*)
    }
  }

  override protected def connectItemNode(node: Node) {
    if (machine.node != null && node != null) {
      machine.node.connect(node)
    }
  }

  override def readFromNBT(nbt: NBTTagCompound) {
    // Load info before inventory and such, to avoid initializing components
    // to empty inventory.
    info.load(nbt.getCompoundTag(Settings.namespace + "info"))
    super.readFromNBT(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setNewCompoundTag(Settings.namespace + "info", info.save)
  }

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound) {
    info.load(nbt.getCompoundTag("info"))
    super.readFromNBTForClient(nbt)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setNewCompoundTag("info", info.save)
  }

  override lazy val items = info.components.map(Option(_))

  override def getSizeInventory = info.components.length

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = false

  // Nope.
  override def setInventorySlotContents(slot: Int, stack: ItemStack) {}

  // Nope.
  override def decrStackSize(slot: Int, amount: Int) = null
}
