import pages.Home
import java.util.*
import javax.swing.JFrame


private val prop = Properties()

fun main() {

    val homePage = Home()
    homePage.isVisible = true
    homePage.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    val timer = Timer()
        val task = object: TimerTask() {
        var timesRan = 0

        override fun run() = println("timer passed ${++timesRan} time(s)")
    }


    timer.schedule(task, 0, 1000)


}










