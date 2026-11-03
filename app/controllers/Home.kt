package controllers

import play.mvc.*

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
class Home : Controller() {

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    fun index() : Result {
        return HomeController().index()
   
    }

}
