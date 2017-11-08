/**
 * Created by Allan Wang on 2017-11-07.
 */

/**
 * The resource manager is held on the server
 * and has actual implementation of the given commands
 */
interface ResourceManager : Contract {

}

interface Server {
    val manager: ResourceManager
}