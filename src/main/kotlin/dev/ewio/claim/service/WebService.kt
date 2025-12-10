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
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.button
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.textArea
import kotlinx.html.title
import kotlinx.html.unsafe
import java.util.UUID

class WebService(
    val port: Int = 8085,
    val onLoreEdited: (newLore: String, oldClaim: VCClaim) -> Unit,
    val tokenLifetimeMinutes: Int,
    val webAddress: String,
    val cssFile: String
){
    private val tokenStore = mutableMapOf<String, TokenData>()

    data class TokenData(
        val uuid: UUID,
        val claim: VCClaim,
        val expiresAt: Long
    )

    private lateinit var server: ApplicationEngine

    fun start() {
        server = embeddedServer(Netty, port = port) {
            install(ContentNegotiation) { jackson() }

            routing {

                // Seite anzeigen
                get("/claim/edit/{token}") {
                    val token = call.parameters["token"] ?: return@get call.respondText(
                        "Invalid token", status = HttpStatusCode.BadRequest
                    )

                    val data = tokenStore[token]
                    if (data == null || System.currentTimeMillis() > data.expiresAt) {
                        return@get call.respondText("Token expired.", status = HttpStatusCode.Forbidden)
                    }

                    val claim = data.claim
                    val lore = claim.description

                    call.respondHtml {
                        head {
                            title { +"Claim Lore bearbeiten" }
                            style { + cssFile}
                        }
                        body {
                            h2 { +"Claim: ${data.claim.displayName}" }
                            textArea {
                                attributes["id"] = "lore"
                                +lore
                            }
                            br()
                            button {
                                attributes["onclick"] = "saveLore()"
                                +"Speichern"
                            }
                            script {
                                unsafe {
                                    +"""
                                        function saveLore() {
                                            fetch('/claim/save/$token', {
                                                method: 'POST',
                                                headers: { 'Content-Type': 'application/json' },
                                                body: JSON.stringify({ lore: document.getElementById('lore').value })
                                            }).then(r => alert("Gespeichert!"));
                                        }
                                    """.trimIndent()
                                }
                            }
                        }
                    }
                }

                // Lore speichern (POST)
                post("/claim/save/{token}") {
                    val token = call.parameters["token"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                    val data = tokenStore[token]
                    if (data == null || System.currentTimeMillis() > data.expiresAt) {
                        return@post call.respond(HttpStatusCode.Forbidden)
                    }

                    val payload = call.receive<Map<String, String>>()
                    val lore = payload["lore"] ?: ""

                    onLoreEdited(lore,data.claim)

                    call.respond(HttpStatusCode.OK)
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
            expiresAt = System.currentTimeMillis() + (tokenLifetimeMinutes*60*1000)
        )
        return "$webAddress/claim/edit/$token"
    }

    fun stop() = server.stop()
}