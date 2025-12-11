package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCPlayerContext
import dev.ewio.util.log
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.files
import io.ktor.server.http.content.staticFiles
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.html.unsafe
import java.io.File
import java.util.UUID

/**
 * The WebService uses predefined web pages and injects runtime data into the before serving them.
 * For this to work the following files have to be present in the webRoot folder:
 * - claimlore.html : The main HTML page for editing the lore of a claim.
 * - default.html : A default HTML page to serve as the root. (Telling the user that they need to use a specific link)
 * - tokenexpired.html : A page to show when the token has expired.
 *
 */
class WebService(
    val port: Int = 8085,
    val onLoreEdited: (newLore: String, oldClaim: VCClaim, uuid: UUID) -> Unit,
    val tokenLifetimeMinutes: Int,
    val webAddress: String,
    val webRoot: File,
){
    private val tokenStore = mutableMapOf<String, TokenData>()

    data class TokenData(
        val uuid: UUID,
        val claim: VCClaim,
        val expiresAt: Long,
        val maxCharacters: Int
    )

    private lateinit var server: ApplicationEngine

    fun start() {
        server = embeddedServer(Netty, port = port) {
            install(ContentNegotiation) { jackson() }

            routing {

                // Static routing für HTML/CSS/JS/Bilder
                staticResources("/public", basePackage = "default.html") {
                    staticFiles("", webRoot)               // alle Dateien direkt verfügbar
                    default("claimlore.html")  // Standardseite
                }


                // Seite anzeigen
                get("/claim/edit/{token}") {
                    val token = call.parameters["token"] ?: return@get call.respondText(
                        "Invalid token", status = HttpStatusCode.BadRequest
                    )

                    val data = tokenStore[token]
                    if (data == null || System.currentTimeMillis() > data.expiresAt) {
                        var response = File(webRoot, "tokenexpired.html").readText()
                        response = response.replace("%web_address%", webAddress)
                        return@get call.respondHtml(HttpStatusCode.OK) {
                            unsafe {
                                +response
                            }
                        }

                    }


                    var editor = File(webRoot, "claimlore.html").readText()

                    //inject data into HTML
                    editor = editor.replace("%claim_name%", data.claim.displayName)
                    editor = editor.replace("%claim_lore%", data.claim.description)
                    editor = editor.replace("%token%", token)
                    editor = editor.replace("%web_address%", webAddress)
                    editor = editor.replace("%expires_at%", data.expiresAt.toString())
                    editor = editor.replace("%max_characters%", data.maxCharacters.toString())

                    call.respondHtml(HttpStatusCode.OK) {
                        unsafe {
                            +editor
                        }
                    }
                }

                // Lore speichern (POST)
                post("/claim/save/{token}") {
                    val token = call.parameters["token"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                    val data = tokenStore[token]
                    if (data == null || System.currentTimeMillis() > data.expiresAt) {
                        return@post call.respond(HttpStatusCode.OK, mapOf("status" to "error"))
                    }

                    val payload = call.receive<Map<String, String>>()
                    val lore = payload["lore"] ?: ""

                    if(lore.length > data.maxCharacters){
                        return@post call.respond(HttpStatusCode.OK, mapOf("status" to "error"))
                    }

                    onLoreEdited(lore,data.claim, data.uuid)

                    call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
                }
            }
        }

        server.start()
        log("WebService started on port $port")
    }

    fun createLoreTokenLink(context: VCPlayerContext, targetClaim: VCClaim): String {
        val token = UUID.randomUUID().toString()
        tokenStore[token] = TokenData(
            uuid = context.player.mcUUID,
            claim = targetClaim,
            expiresAt = System.currentTimeMillis() + (tokenLifetimeMinutes*60*1000),
            maxCharacters = context.restrictions.maxClaimLoreLength
        )
        return "$webAddress/claim/edit/$token"
    }

    fun stop() = server.stop()
}