import java.util.*

/**
 * Created by Allan Wang on 2017-11-07.
 */
interface Binding {
    /**
     * Name of command
     */
    val name: String

    /**
     * Format to check for arguments
     */
    val formatter: String

    /**
     * Returns [true] if arguments match desired format
     * [false] otherwise
     */
    fun validate(input: Array<String>) =
            try {
                String.format(formatter, *input)
                true
            } catch (e: IllegalFormatException) {
                false
            }

    fun onRequest(args: Array<String>, server: Server)

}

inline fun <reified T> String.convert(): T =
        when (T::class) {
            String::class -> this
            Int::class -> toInt()
            Boolean::class -> toBoolean()
            else -> throw IllegalArgumentException("converter type must be one of string, int, or boolean")
        } as T

inline fun <reified T, reified O> Triple<Array<String>, Contract, Callback>.request(
        contractMethod: (Contract) -> (T) -> O,
        callbackMethod: (Callback) -> (O) -> Unit) {
    val (args, contract, callback) = this
    val out = contractMethod(contract)(args[0].convert())
    callbackMethod(callback)(out)
}

inline fun <reified T, reified R, reified O> Triple<Array<String>, Contract, Callback>.request2(
        contractMethod: (Contract) -> (T, R) -> O,
        callbackMethod: (Callback) -> (O) -> Unit) {
    val (args, contract, callback) = this
    val out = contractMethod(contract)(args[0].convert(), args[1].convert())
    callbackMethod(callback)(out)
}

val onRequest: (Triple<Array<String>, Contract, Callback>) -> Unit = {
    it.request<Int, Boolean>({ it::checkFlight }, { it::onCheckFlight })
}

/**
 * This allows type conversion for command tokens
 * Note that in the formatter, %d represents int, %s represents string, and %b represents bool
 */
enum class Command(override val formatter: String, val onRequest: (Triple<Array<String>, Contract, Callback>) -> Unit) : Binding {
    requestFlight("%d %d", { it.request2({ it::requestFlight }, { it::onRequestFlight }) }),
    checkFlight("%d", { it.request({ it::checkFlight }, { it::onCheckFlight }) }),
    deposit("%d", { it.request({ it::deposit }, { it::onDeposit }) }),
    withdraw("%d", { it.request({ it::withdraw }, { it::onWithdraw }) });

    override fun onRequest(args: Array<String>, server: Server) = onRequest(Triple(args, server.manager, server.callback))

    companion object {
        private val values = values().map { it.name }.toSet()

        /**
         * Given a client command, send to server if valid
         */
        operator fun invoke(client: Client, input: String) {
            val data = input.split(" ")
            val cmd = data[0]
            if (!values.contains(cmd))
                return println("$cmd is not a valid command")
            val command = valueOf(data[0])
            val args = data.subList(1, data.size).toTypedArray()
            if (command.validate(args))
                return println("$cmd does not have properly formatted arguments")
            client.sendToServer(input)
        }

        /**
         * Given input, execute on server side and call callback
         * You technically shouldn't need any of these validations as they were done already
         */
        fun execute(server: Server, input: String) {
            val data = input.split(" ")
            val cmd = data[0]
            if (!values.contains(cmd))
                return println("$cmd is not a valid command")
            val command = valueOf(data[0])
            val args = data.subList(1, data.size).toTypedArray()
            if (command.validate(args))
                return println("$cmd does not have properly formatted arguments")
            command.onRequest(args, server)
        }
    }
}