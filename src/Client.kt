/**
 * Created by Allan Wang on 2017-11-07.
 */
interface Client : Callback {

    val server: Server
    /**
     * Registers input found on command line
     */
    fun onInputReceived(cmd: String) = Command(this, cmd)

    /**
     * Sends validated command to server as a string
     */
    fun sendToServer(cmd: String) = server.onInputReceived(cmd)
}

