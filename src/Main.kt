import java.awt.Color
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.io.*
import java.lang.Integer.parseInt
import java.util.*
import java.util.function.Consumer
import javax.swing.*
import javax.swing.JScrollPane


object Main {

  private val prop = Properties()
  private val remotePCs = ArrayList<RemotePC>()
  private var saveTextField: String? = null
  private val f = JFrame("Start/shutdown pc's")
  private val componentMap = mutableMapOf<String, JTextArea>()

  @Throws(IOException::class)
  @JvmStatic
  fun main(args: Array<String>) {
    createWindow()
    loadProperties()
  }

  @Throws(IOException::class)
  private fun loadProperties() {
    if (System.getProperty("os.name") != "Mac OS X") {
      val fileName = "c:/VR/application.properties"
      val inputStream = FileInputStream(fileName)
      prop.load(inputStream)
      for (i in 0..9) {
        remotePCs.add(buildRemotePCs(i + 1))
      }
    }
      else{
        val fileName = "src/resources/application.properties"
        val `is` = FileInputStream(fileName)
        prop.load(`is`)
        for (i in 1..6) {
            remotePCs.add(buildRemotePCs(i))
        }
    }
  }

  private fun buildRemotePCs(i: Int): RemotePC {
    print("Adding pc$i")
    val pcNumber = "pc$i"
    return RemotePC(
      prop.getProperty("$pcNumber.gebruikersnaam"),
      prop.getProperty("$pcNumber.netwerknaam"),
      prop.getProperty("$pcNumber.wachtwoord"),
      prop.getProperty("$pcNumber.macadres")
    )
  }

  internal class StreamGobbler(private val inputStream: InputStream, private val consumer: Consumer<String>) :
    Runnable {
    override fun run() {
      BufferedReader(InputStreamReader(inputStream)).lines()
        .forEach(consumer)
    }
  }

  private fun createWindow() {
    //submit button
    val allOnButton = JButton("Alles aan")
    val turnOnXButton = JButton("X aan")
    val turnAllOffButton = JButton(" Alles uit")
    val turnxOffButton = JButton("X uit")
    val setLogFieldButton = JButton("set textField")
    val startMiningButton = JButton("Start all mining")


    val logTextField = JTextArea("", 5, 20)
    val scrollPane = JScrollPane(logTextField)
    componentMap["Logfield"] = logTextField
    f.add(scrollPane)

    val textField = JTextField("PC nummers simpel achter elkaar. BV: '124'")

    textField.foreground = Color.GRAY
    textField.addFocusListener(object : FocusListener {
      override fun focusGained(e: FocusEvent) {
        textField.text = ""
        textField.foreground = Color.BLACK
      }

      override fun focusLost(e: FocusEvent) {
        textField.foreground = Color.GRAY
        saveTextField = textField.text
        textField.text = "PC nummers achter elkaar: bijvoorbeeld :'143'"

      }
    })

    scrollPane.setBounds(400, 70, 350, 120)

    textField.setBounds(400, 10, 350, 40)
    logTextField.setBounds(400, 70, 350, 120)
    setLogFieldButton.setBounds(400, 190, 350, 35)
    allOnButton.setBounds(10, 10, 140, 40)
    turnOnXButton.setBounds(10, 110, 140, 40)
    turnAllOffButton.setBounds(200, 10, 140, 40)
    turnxOffButton.setBounds(200, 110, 140, 40)
    startMiningButton.setBounds(10, 210, 140, 40)


    //add to frame
    f.add(allOnButton)
    f.add(turnOnXButton)
    f.add(turnAllOffButton)
    f.add(turnxOffButton)
    f.add(setLogFieldButton)
    f.add(logTextField)
    f.add(scrollPane)
    f.add(startMiningButton)

    f.add(textField)
    f.setSize(1800, 600)
    f.layout = null
    f.isVisible = true
    f.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    //action listener
    turnOnXButton.addActionListener { turnOnxPCs(saveTextField!!) }
    allOnButton.addActionListener { remotePCs.forEach { it.turnOn(prop.getProperty("subnetmask"), it.macadres) } }
    turnAllOffButton.addActionListener { remotePCs.forEach { it.turnOff(it) } }
    turnxOffButton.addActionListener { turnOfXPCs(saveTextField!!) }
    startMiningButton.addActionListener {  remotePCs.forEach { it.startMining(it) }  }
//        setLogFieldButton.addActionListener { componentMap["Logfield"]!!.append("\nasdf") }

  }

  @Throws(IOException::class)
  private fun turnofAllpcs(temp: ArrayList<RemotePC>) {
    temp.forEach { pc -> pc.turnOff(pc) }
  }

  //1,2,3,11

  @Throws(IOException::class)
  private fun turnOfXPCs(pcArray: String) {
    if (pcArray.isEmpty()) {
      return
    }
    val temp = ArrayList<RemotePC>()

    pcArray.toCharArray()

    for (i in 1..pcArray.toCharArray().size) {
      temp.add(remotePCs[parseInt(i.toString())])
    }
    turnofAllpcs(temp)
  }

  private fun turnOnAllPCs(temp: ArrayList<RemotePC>) {
    for (pc in temp) {
      pc.turnOn(prop.getProperty("subnetmask"), pc.macadres)
    }
  }

  private fun turnOnxPCs(pcArray: String) {
    val temp = ArrayList<RemotePC>()
    for (i in pcArray.toCharArray()) {
      temp.add(remotePCs[parseInt(i.toString()) - 1])
    }
    turnOnAllPCs(temp)
  }

  private fun logToConsole(logText: String) {
    componentMap["Logtext"]!!.append(logText)
  }

}
