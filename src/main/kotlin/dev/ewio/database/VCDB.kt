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
    lateinit var db: Database
        private set

    fun connect(dbPath: String) {
        // PRAGMAs *einmalig* vor Pool setzen
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { c ->
            c.createStatement().use { st ->
                st.execute("PRAGMA journal_mode=WAL;")
                st.execute("PRAGMA foreign_keys=ON;")
                st.execute("PRAGMA busy_timeout=5000;")
                st.execute("PRAGMA synchronous=NORMAL;") // schneller, ok für WAL
            }
        }

        val cfg = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:$dbPath"
            driverClassName = "org.sqlite.JDBC"

            // Writers bleiben trotzdem sequenziell (SQLite-Constraint).
            maximumPoolSize = 4
            minimumIdle = 1

            // Exposed managt Transaktionen – AutoCommit hier lieber AUS
            isAutoCommit = false

            // Leaks aufspüren
            leakDetectionThreshold = 10_000

            // SQLite blockiert – lieber länger warten als sofort fehlschlagen
            connectionTimeout = 60_000

            // Health-Check
            connectionTestQuery = "SELECT 1"
        }
        ds = HikariDataSource(cfg)
        db = Database.connect(ds)

        // Isolation muss bei SQLite nicht hochgedreht werden; Standard reicht.
        // TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(
                VCPlayers, VCClaims, VCChunks
            )
        }
    }

    fun shutdown() {
        if (this::ds.isInitialized) ds.close()
    }
}