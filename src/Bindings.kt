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

/*
 * -------------------------------------------------------------
 * Commence black magic
 * This is possible through Kotlin's inline abilities, which effectively
 * removes type erasure with generic types
 * -------------------------------------------------------------
 */

/**
 * Infers proper string conversion based on desired output
 * The cases are not exhaustive, but should satisfy the command conditions
 */
private inline fun <reified T> String.convert(): T =
        when (T::class) {
            String::class -> this
            Int::class -> toInt()
            Boolean::class -> toBoolean()
            else -> throw IllegalArgumentException("converter type must be one of string, int, or boolean")
        } as T

/**
 * Where the magic happens
 * The specificity of argument size allows for the use of references,
 * and all of the generic types are inline and inferred
 * We will need to make a new method each time the number of arguments increases,
 * but this makes everything else just about as short as it can get
 */
private inline fun <reified T, reified O> Triple<Array<String>, Contract, Callback>.request(
        contractMethod: (Contract) -> (T) -> O,
        callbackMethod: (Callback) -> (O) -> Unit) {
    val (args, contract, callback) = this
    val out = contractMethod(contract)(args[0].convert())
    callbackMethod(callback)(out)
}

private inline fun <reified T, reified R, reified O> Triple<Array<String>, Contract, Callback>.request2(
        contractMethod: (Contract) -> (T, R) -> O,
        callbackMethod: (Callback) -> (O) -> Unit) {
    val (args, contract, callback) = this
    val out = contractMethod(contract)(args[0].convert(), args[1].convert())
    callbackMethod(callback)(out)
}

/*
 * -------------------------------------------------------------
 * End black magic
 * -------------------------------------------------------------
 */

/**
 * This allows type conversion for command tokens
 * Note that in the formatter, %d represents int, %s represents string, and %b represents bool
 */
enum class Command(override val formatter: String, private val onRequest: (Triple<Array<String>, Contract, Callback>) -> Unit) : Binding {
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