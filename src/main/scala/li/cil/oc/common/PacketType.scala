package li.cil.oc.common

object PacketType extends Enumeration {
  val
  // Server -> Client
  AbstractBusState,
  Analyze,
  ChargerState,
  ColorChange,
  ComputerState,
  ComputerUserList,
  DisassemblerActiveChange,
  FileSystemActivity,
  FloppyChange,
  HologramClear,
  HologramColor,
  HologramPowerChange,
  HologramScale,
  HologramSet,
  HologramTranslation,
  PetVisibility, // Goes both ways.
  PowerState,
  RaidStateChange,
  RedstoneState,
  RobotAnimateSwing,
  RobotAnimateTurn,
  RobotAssemblingState,
  RobotInventoryChange,
  RobotMove,
  RobotSelectedSlotChange,
  RotatableState,
  SwitchActivity,
  TextBufferColorChange,
  TextBufferCopy,
  TextBufferDepthChange,
  TextBufferFill,
  TextBufferInit, // Goes both ways.
  TextBufferMulti,
  TextBufferPaletteChange,
  TextBufferPowerChange,
  TextBufferResolutionChange,
  TextBufferSet,
  ScreenTouchMode,
  ServerPresence,
  Sound,
  SoundPattern,

  // Client -> Server
  ComputerPower,
  KeyDown,
  KeyUp,
  Clipboard,
  MouseClickOrDrag,
  MouseScroll,
  MouseUp,
  MultiPartPlace,
  RobotAssemblerStart,
  RobotStateRequest,
  ServerRange,
  ServerSide,
  ServerSwitchMode,

  EndOfList = Value
}