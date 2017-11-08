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

    fun onRequest(args: Array<String>, contract: Contract): String

    /**
     * Emitted from resource manager
     * [output] is emission
     * [callback] is response passage to client
     */
    fun onResponse(output: String, callback: Callback)
}

/**
 * This allows type conversion for command tokens
 * Note that in the formatter, %d represents int, %s represents string, and %b represents bool
 */
enum class Command(override val formatter: String,
                   private val onRequest: Contract.(Array<String>) -> Any,
                   private val onResponse: Callback.(String) -> Unit) : Binding {

    requestFlight("%d %d", { requestFlight(it[0].toInt(), it[1].toInt()) }, { onRequestFlight(it.toBoolean()) }),
    checkFlight("%d", { checkFlight(it[0].toInt()) }, { onCheckFlight(it.toBoolean()) }),
    deposit("%d", { deposit(it[0].toInt()) }, { onDeposit(it.toInt()) }),
    withdraw("%d", { withdraw(it[0].toInt()) }, { onWithdraw(it.toInt()) });

    override fun onRequest(args: Array<String>, contract: Contract) = contract.onRequest(args).toString()

    override fun onResponse(output: String, callback: Callback) = callback.onResponse(output)

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
            command.onRequest(args, server.manager)
        }
    }
}