package net.ocsoft.mswp
import jQuery
import net.ocsoft.mswp.ui.Grid
import net.ocsoft.mswp.ui.ShaderPrograms
import kotlin.js.Promise
import kotlin.js.Json
import org.w3c.fetch.*
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import org.w3c.dom.MutationRecord
import org.w3c.dom.Node
import org.w3c.dom.Element
import org.w3c.dom.Image
import kotlinx.browser.window
import kotlin.collections.Set
import kotlin.collections.HashSet
import net.ocsoft.mswp.ui.GridSettings
import net.ocsoft.mswp.ui.AppSettings
import net.ocsoft.mswp.ui.Persistence
import net.ocsoft.mswp.ui.IconSetting
import net.ocsoft.mswp.ui.Flag

/**
 * main page display
 */
actual class MainPage {
        
    companion object {
    }
    /**
     * configuration
     */
    val config: MainPageConfig = MainPageConfig()

    /**
     * game grid
     */
    var grid : Grid? = null
    /**
     * model
     */
    var model: Model? = null

    /**
     * camera
     */
    var camera: Camera? = null

    /**
     * light
     */
    var pointLight: PointLight? = null

    /**
     * color scheme
     */
    var colorScheme: ColorScheme? = null
 
    /**
     * start gaming
     */
    var runPlayground : (()->Unit)? = null


    /**
     * setting
     */
    var appSettings: AppSettings = AppSettings(config.appSettings)

    /**
     * setup body
     */ 
    actual fun setupBody(model : Model, camera: Camera, 
        pointLight: PointLight,
        colorScheme: ColorScheme) {
        this.model = model
        this.camera = camera
        this.pointLight = pointLight        
        this.colorScheme = colorScheme
    }
    /**
     * setup for html page
     */
    actual fun setup(settings: Settings) {
    }     
    /**
     * run program
     */ 
    fun run(settings: Any?) {
        var settingObj : dynamic
        settingObj = settings
            
        if (settingObj != null) {
            var promises = ArrayList<Promise<Any>>()
            
            promises.add(loadFont(settingObj.textureText))
            promises.add(Polyfill.load())
            Promise.all(Array<Promise<Any>>(promises.size) { 
                promises[it] 
            }).then {
                setupBodyI(model!!, 
                camera!!, 
                pointLight!!,
                colorScheme!!,
                settingObj.rootDir,
                settingObj.progDir,
                settingObj.ui) 
            }
        }
    }
    fun setupBodyI(model : Model, 
        camera: Camera, 
        pointLight: PointLight,
        colorScheme: ColorScheme,
        rootDir: String,
        progDir: String,
        uiSetting: Json) {

        appSettings.runtimeConfig = uiSetting
        jQuery { 
            val grid = Grid(appSettings.option.pointLightSettingOption)
            val flag = Flag(appSettings.option.flagOption)
            val shaders = arrayOf(
                "${rootDir}${progDir}net/ocsoft/mswp/ui/vertex.gls", 
                "${rootDir}${progDir}net/ocsoft/mswp/ui/fragment.gls",
                "${rootDir}${progDir}net/ocsoft/mswp/ui/point-vertex.gls", 
                "${rootDir}${progDir}net/ocsoft/mswp/ui/point-fragment.gls",
                "${rootDir}${progDir}net/ocsoft/mswp/ui/shadow-vertex.gls", 
                "${rootDir}${progDir}net/ocsoft/mswp/ui/shadow-fragment.gls")
            var promises = ArrayList<Promise<Any>>()

            shaders.forEach {
                promises.add(window.fetch(it).then({ it.text() }));
            }

            promises.add(
                glrs.init("${rootDir}${progDir}glrs_bg.wasm"))
            promises.add(
                Persistence.loadIcon().then({ 
                    if (it != null) {
                        config.gridSettings.iconSetting.replaceIcons(it)
                    }
                    Unit
                }))
            var pointLight0 : PointLight? = null
            promises.add(
                Persistence.loadPointLight().then({
                    if (it != null) {
                        pointLight0 = it
                    }
                    Unit
                }))
            var colorScheme0 : ColorScheme? = null
            promises.add(
                Persistence.loadColorScheme().then({
                    if (it != null) {
                        colorScheme0 = it
                    }
                    Unit
                }))
            val promisesArray = Array<Promise<Any>>(promises.size) {
                promises[it]
            }

            Promise.all(promisesArray).then {
                responses : Array<out Any> -> 
                var shaderPrograms = ShaderPrograms(
                    responses[0] as String, 
                    responses[1] as String,
                    responses[2] as String,
                    responses[3] as String,
                    responses[4] as String,
                    responses[5] as String)
                grid.glrs = responses[6] as glrs.InitOutput

                var pointLightParam = pointLight
                if (pointLight0 != null) {
                    this.pointLight = pointLight0!!
                    pointLightParam = pointLight0!!
                }
                var colorSchemeParam = colorScheme
                if (colorScheme0 != null) {
                    this.colorScheme = colorScheme0!!
                    colorSchemeParam = colorScheme0!!
                }
                val env = Environment(
                    appSettings.option.environmentOption,
                    colorSchemeParam)
                flag.logic = model.logic
                flag.bind(window.document.body!!)
                 
                grid.bind(config.gridSettings,
                    model, camera, 
                    pointLightParam, 
                    flag,
                    colorSchemeParam,
                    env,
                    shaderPrograms,
                    appSettings)
                appSettings.bind()
                readyToPlay() 
 
            }
        }
    }

    /**
     * the program is ready to play now
     */
    fun readyToPlay() {
        jQuery(".loading", config.splashPaneId).hide()   
        jQuery(config.splashPaneId).height(0)
    }

    /**
     * load font
     */
    fun loadFont(textToLoad : String) : Promise<Unit> {
        val result = Promise<Unit> {
            resolve, _ -> Unit  
            val activeCallback = {
                resolve(Unit) 
            }
            val inactiveCallback = {
                resolve(Unit)
                // reject(Error("failed"))
            }
            val config : dynamic = WebFontConfig(activeCallback, 
                inactiveCallback,
                textToLoad)
            WebFont.load(config)
        }
        return result
    }
}

// vi: se ts=4 sw=4 et:
