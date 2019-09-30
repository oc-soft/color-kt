package net.ocsoft.mswp.ui
import jQuery
import JQuery
import JQueryAjaxSettings
import kotlin.js.Promise
import kotlin.browser.window
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import org.w3c.xhr.FormData

/**
 * manage user interface persistence data.
 */
class Persistence {

   
    /**
     * class instance
     */
    companion object {
        
        /**
         * read icon
         */
        fun ReadIcon(): Promise<Icon> {
            val result = Promise<Icon>({
                resolve, reject ->  

                 val body = FormData()
                 body.append("icon", "read")
                 var prm = window.fetch("persistence.php",
                    RequestInit(
                        method = "POST",
                        body = body)) 
                 prm.then(
                    {
                        res ->
                        res.text()
                    }).then(
                    {
                        res ->
                        val res0: dynamic = res 
                        resolve(Icon(res0.prefix, res0.name)) 
                    }).catch( 
                    {
                        reason ->
                        reject(Throwable(reason))
                    })
            }) 
            return result 
        }

        /**
         * save icon setting
         */
        fun saveIcon(data: Icon): Promise<Any?> {
            val strData = JSON.stringify(data)  
            val result = Promise<Any?>({
                resolve, reject ->
                val body = FormData()
                body.append("icon", "write")
                body.append("data", strData)

                window.fetch("persistence.php",
                    RequestInit(
                        method = "POST",
                        body = body)).then(
                    {
                        res ->
                        res.json()
                    }).then(
                    {
                        res ->
                        resolve(res) 
                    }).catch( 
                    {
                        reason ->
                        reject(Throwable(reason))
                    })
            })
            return result
        }
    }

    /**
     * icon data
     */
    data class Icon(
        @JsName("prefix")
        val prefix: String,
        @JsName("iconName")
        val iconName: String)   
}