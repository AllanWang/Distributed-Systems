/**
 * Created by Allan Wang on 2017-11-07.
 */
interface Client {
    fun onInputReceived(cmd:String) = Command(this, cmd)
    fun sendToServer(cmd:String)
}


interface Contract {
    /**
     * Request booking for given flight number
     * Returns [true] if successfully booked, [false] otherwise
     */
    fun requestFlight(id:Int, num: Int): Boolean

    /**
     * Check if flight is booked
     * Return [true] if booked, [false] otherwise
     */
    fun checkFlight(num: Int): Boolean

    /**
     * Deposit given sum
     * Return total in account
     */
    fun deposit(num: Int): Int

    /**
     * Withdraw given sum
     * Return total remaining
     */
    fun withdraw(num: Int): Int
}

interface Callback {
    fun onRequestFlight(out: Boolean)
    fun onCheckFlight(out: Boolean)
    fun onDeposit(out: Int)
    fun onWithdraw(out: Int)
}