package dev.ewio.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object VCDB {
    private lateinit var ds: HikariDataSource

    fun connect(dbPath: String) {
        /// WAL vorher einmalig setzen
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { conn ->
            conn.createStatement().use { st ->
                st.execute("PRAGMA journal_mode = WAL;")
            }
        }

        val cfg = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:$dbPath"
            maximumPoolSize = 1          // SQLite: eine Verbindung reicht
            isAutoCommit = true          // PRAGMAs laufen außerhalb von Transaktionen
            connectionInitSql = """
                PRAGMA foreign_keys = ON;
                PRAGMA busy_timeout = 5000;
            """.trimIndent()
        }
        ds = HikariDataSource(cfg)

        val db = Database.connect(ds)

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction(db) {
            // Tabellen anlegen
            SchemaUtils.createMissingTablesAndColumns(
                VCPlayers, VCClaims, VCChunks
            )
        }
    }

    fun shutdown() {
        if (this::ds.isInitialized) ds.close()
    }
}