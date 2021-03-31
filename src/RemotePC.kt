import Main.StreamGobbler
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.function.Consumer

class RemotePC internal constructor(
  private val gebruikersnaam: String,
  private val netwerknaam: String,
  private val wachtwoord: String,
  val macadres: String
) {
  override fun toString(): String {
    return "RemotePC{" +
        "netwerknaam='" + netwerknaam + '\'' +
        ", gebruikersnaam='" + gebruikersnaam + '\'' +
        ", wachtwoord='" + wachtwoord + '\'' +
        ", macadres='" + macadres + '\'' +
        '}'
  }

  fun turnOff(pc: RemotePC?) {
    if (pc != null) {
      if (!gebruikersnaam.isEmpty()) {
        val command =
          "c:/VR/psshutdown.exe  \\\\" + netwerknaam + " -u " + gebruikersnaam + " -p " + wachtwoord + " -s -f -t 1  "
        val shutdownThread = CommandThread(command, pc)
        shutdownThread.run()
      }
    }
  }

  fun turnOn(ipStr: String?, macStr: String) {
    val PORT = 9
    try {
      val macBytes = getMacBytes(macStr)
      val bytes = ByteArray(6 + 16 * macBytes.size)
      for (i in 0..5) {
        bytes[i] = 0xff.toByte()
      }
      var i = 6
      while (i < bytes.size) {
        System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
        i += macBytes.size
      }
      val address = InetAddress.getByName(ipStr)
      val packet = DatagramPacket(bytes, bytes.size, address, PORT)
      val socket = DatagramSocket()
      socket.send(packet)
      socket.close()
      println("Wake-on-LAN packet sent.")
    } catch (e: Exception) {
      println("Failed to send Wake-on-LAN packet:$e")
      System.exit(1)
    }
  }

  fun startMining(pc: RemotePC?) {
    if (pc != null) {
      val command =
        "c:/VR/psexec.exe \\\\" + netwerknaam + " -u " + gebruikersnaam + " -p " + wachtwoord + " c:/VR/Mining/PhoenixMiner.exe"
      println(command)
      val shutdownThread: CommandThread = CommandThread(command, pc)
      shutdownThread.run()
    }
  }

  internal inner class CommandThread(var command: String, var pc: RemotePC) : Runnable {
    @Throws(IOException::class)
    private fun isOn(pcName: String): Boolean {
      val pingablePC = InetAddress.getByName(pcName)
      for (i in 0..2) {
        println("Sending Ping Request to $pcName")
        if (pingablePC.isReachable(100)) {
          println("PC" + pcName + "is ON")
          return true
        }
      }
      println("PC" + pcName + "is OFF")
      return false
    }

    fun sendCommand(command: String) {
      val netCommand = "net use \\\\" +
          pc.netwerknaam + " /user:" + pc.gebruikersnaam + " " + pc.wachtwoord
      val netBuilder = ProcessBuilder()
      netBuilder.command("cmd.exe", "/c", netCommand)
      netBuilder.directory(File(System.getProperty("user.home")))
      val builder = ProcessBuilder()
      builder.command("cmd.exe", "/c", command)
      builder.directory(File(System.getProperty("user.home")))
      var netProcess: Process? = null
      var process: Process? = null
      try {
        netProcess = netBuilder.start()
        process = builder.start()
      } catch (e: IOException) {
        e.printStackTrace()
      }
      assert(netProcess != null)
      assert(process != null)
      val netstreamGobbler = StreamGobbler(
        netProcess!!.inputStream,
        Consumer { x: String? -> println(x) })
      val streamGobbler = StreamGobbler(
        process!!.inputStream,
        Consumer { x: String? -> println(x) })
      println("Sending NETcommand  $netCommand")
      Executors.newSingleThreadExecutor().submit(netstreamGobbler)
      println("Sending command  $command")
      Executors.newSingleThreadExecutor().submit(streamGobbler)
    }

    override fun run() {
      try {
        if (isOn(pc.netwerknaam)) {
          sendCommand(command)
        }
      } catch (e: Exception) {
        println(e.toString())
      }
    }
  }

  companion object {
    @Throws(IllegalArgumentException::class)
    private fun getMacBytes(macStr: String): ByteArray {
      val bytes = ByteArray(6)
      val hex = macStr.split("(\\:|\\-)".toRegex()).toTypedArray()
      require(hex.size == 6) { "Invalid MAC address." }
      try {
        for (i in 0..5) {
          bytes[i] = hex[i].toInt(16).toByte()
        }
      } catch (e: NumberFormatException) {
        throw IllegalArgumentException("Invalid hex digit in MAC address.")
      }
      return bytes
    }
  }
}
